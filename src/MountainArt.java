import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Pixel-art renderer. Everything is painted on a low-resolution canvas and
 * scaled up by {@link #P} with nearest-neighbour sampling, so the whole game
 * shares one chunky pixel grid.
 */
public final class MountainArt {
    /** Size of one art pixel on screen. */
    public static final int P = 4;

    /** Minimum columns (in art pixels) of open sky on each side of the cliff. */
    private static final int WALL_INSET = 28;
    private static final int WALL_WOBBLE = 12;

    private static final float[][] BAYER = {
        {0.03f, 0.53f, 0.15f, 0.65f},
        {0.78f, 0.28f, 0.90f, 0.40f},
        {0.21f, 0.71f, 0.09f, 0.59f},
        {0.96f, 0.46f, 0.84f, 0.34f},
    };

    // Cliff palette: three stone tones, each lit / base / shade.
    // All warm browns, so the cool slate falling rocks always contrast.
    private static final int[] ROCK_LIT = {0xd39f6c, 0xc28d5c, 0xc78a62};
    private static final int[] ROCK_BASE = {0xa8744a, 0x95653f, 0x9a5f3f};
    private static final int[] ROCK_SHADE = {0x7a5234, 0x6c4a2f, 0x6e412b};
    private static final int CRACK = 0x4a2f1f;
    private static final int CLIFF_OUTLINE = 0x3a261a;
    private static final int RIM_LIT = 0xe0b07a;
    private static final int RIM_SHADE = 0x5a3a25;

    private static BufferedImage canvas;
    private static int[] pixels;
    private static int[] backdrop;
    private static int canvasW;
    private static int canvasH;

    private MountainArt() {}

    // ------------------------------------------------------------------ layout

    private static int lowW(int width) {
        return (width + P - 1) / P;
    }

    /** Left edge of the cliff in art pixels: the cliff spans the window, like the old road. */
    private static int cliffLeft(int w) {
        return WALL_INSET;
    }

    private static int cliffRight(int w) {
        return w - cliffLeft(w);
    }

    /** Left edge (screen px) of the always-climbable part of the cliff. */
    public static int playLeft(int width) {
        return cliffLeft(lowW(width)) * P;
    }

    /** Right edge (screen px) of the always-climbable part of the cliff. */
    public static int playRight(int width) {
        return cliffRight(lowW(width)) * P;
    }

    /** Rounds a screen coordinate down onto the art-pixel grid. */
    public static int snap(int v) {
        return v - Math.floorMod(v, P);
    }

    // -------------------------------------------------------------- background

    public static void drawBackground(Graphics2D g, int width, int height, int scrollOffset) {
        int w = lowW(width);
        int h = (height + P - 1) / P;
        if (w <= 0 || h <= 0) return;

        if (canvas == null || canvasW != w || canvasH != h) {
            canvasW = w;
            canvasH = h;
            canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            pixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
            backdrop = buildBackdrop(w, h);
        }

        System.arraycopy(backdrop, 0, pixels, 0, pixels.length);
        drawClouds(w, h, scrollOffset);
        drawCliffFace(w, h, scrollOffset / P);

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(canvas, 0, 0, w * P, h * P, null);
    }

    /** Static distant scenery: sky, two snowy ranges, valley haze and pines. */
    private static int[] buildBackdrop(int w, int h) {
        int[] out = new int[w * h];

        int[] sky = {0x3b7bc4, 0x4889cf, 0x5898d8, 0x6aa7df, 0x80b6e6, 0x99c6ec};
        int horizon = h * 62 / 100;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float t = Math.min(1f, y / (float) horizon) * (sky.length - 1);
                int band = (int) t;
                if (band < sky.length - 1 && t - band > BAYER[y & 3][x & 3]) band++;
                out[y * w + x] = sky[band];
            }
        }

        drawRange(out, w, h, new float[][]{
            {-0.05f, 0.36f}, {0.12f, 0.24f}, {0.33f, 0.33f}, {0.55f, 0.16f},
            {0.80f, 0.26f}, {1.02f, 0.30f}},
            1.25f, 0x8aa0bb, 0x6c83a1, 0xf5f8fb, 0xc3d3e4, 1);
        drawRange(out, w, h, new float[][]{
            {-0.02f, 0.47f}, {0.20f, 0.39f}, {0.44f, 0.50f}, {0.72f, 0.37f},
            {0.96f, 0.45f}},
            1.0f, 0x627891, 0x4b5f78, 0xe9f0f6, 0xaebfd2, 2);

        // Valley haze fading into a dark forest floor.
        int valley = h * 70 / 100;
        int forest = h * 80 / 100;
        for (int y = valley; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c;
                if (y < forest) {
                    float t = (y - valley) / (float) (forest - valley);
                    c = t > BAYER[y & 3][x & 3] ? 0x5b7f74 : 0x7896a4;
                } else {
                    c = 0x2a4f39;
                }
                out[y * w + x] = c;
            }
        }

        // Two rows of pines: a dim back row and a brighter front row.
        drawPines(out, w, h, valley + 6, 0x2d5f48, 0x21483a, 11);
        drawPines(out, w, h, forest + 2, 0x2f7048, 0x1d4c33, 23);
        return out;
    }

    private static void drawRange(int[] out, int w, int h, float[][] peaks, float slope,
            int rockLit, int rockShade, int snowLit, int snowShade, int seed) {
        int snowDepth = Math.max(4, h * 9 / 100);
        for (int x = 0; x < w; x++) {
            int ridge = Integer.MAX_VALUE;
            int peakX = 0;
            int peakY = 0;
            for (float[] p : peaks) {
                int px = Math.round(p[0] * w);
                int py = Math.round(p[1] * h);
                int r = py + Math.round(Math.abs(x - px) * slope);
                if (r < ridge) {
                    ridge = r;
                    peakX = px;
                    peakY = py;
                }
            }
            ridge += hash(x, seed) % 2;
            boolean lit = x < peakX;
            int snowLine = peakY + snowDepth + hash(x / 2, seed + 7) % 5;
            if (x % 3 == 0) snowLine += hash(x, seed + 3) % 7; // streaks
            for (int y = Math.max(0, ridge); y < h; y++) {
                boolean snow = y < snowLine;
                int c = snow ? (lit ? snowLit : snowShade) : (lit ? rockLit : rockShade);
                out[y * w + x] = c;
            }
        }
    }

    private static void drawPines(int[] out, int w, int h, int baseY, int lit, int shade, int seed) {
        int x = -2 + hash(seed, 1) % 4;
        int i = 0;
        while (x < w + 4) {
            int hh = hash(i, seed);
            int treeH = 9 + hh % 8;
            int bottom = baseY + (hh >>> 8) % 4;
            for (int k = 0; k < treeH; k++) {
                int y = bottom - treeH + k;
                int half = (k % 4) / 2 + k / 3; // tiered branches
                for (int dx = -half; dx <= half; dx++) {
                    put(out, w, h, x + dx, y, dx <= 0 ? lit : shade);
                }
            }
            put(out, w, h, x, bottom, 0x4a3324);
            put(out, w, h, x, bottom + 1, 0x4a3324);
            x += 5 + (hh >>> 4) % 4;
            i++;
        }
    }

    private static void put(int[] out, int w, int h, int x, int y, int c) {
        if (x >= 0 && x < w && y >= 0 && y < h) out[y * w + x] = c;
    }

    private static void drawClouds(int w, int h, int scrollOffset) {
        float[][] clouds = {{0.10f, 0.10f, 1.0f}, {0.62f, 0.07f, 1.2f},
            {0.36f, 0.22f, 0.8f}, {0.86f, 0.19f, 0.9f}};
        int drift = scrollOffset / 24;
        for (float[] c : clouds) {
            float s = c[2];
            int cx = Math.floorMod(Math.round(c[0] * w) + drift, w + 40) - 30;
            int cy = Math.round(c[1] * h);
            float[][] puffs = {{6 * s, -3 * s, 4 * s}, {13 * s, -6 * s, 6.5f * s},
                {21 * s, -4 * s, 4.5f * s}, {26 * s, -2 * s, 3 * s}};
            int cw = Math.round(30 * s);
            int ch = Math.round(13 * s);
            for (int ly = -ch; ly <= 0; ly++) {
                for (int lx = 0; lx <= cw; lx++) {
                    boolean inside = false;
                    for (float[] p : puffs) {
                        float dx = lx - p[0];
                        float dy = ly - p[1];
                        if (dx * dx + dy * dy <= p[2] * p[2]) {
                            inside = true;
                            break;
                        }
                    }
                    if (!inside) continue;
                    put(pixels, w, h, cx + lx, cy + ly, ly > -2 ? 0xd6e4f1 : 0xffffff);
                }
            }
        }
    }

    /** The scrolling boulder cliff the goat climbs. */
    private static void drawCliffFace(int w, int h, int scroll) {
        for (int y = 0; y < h; y++) {
            int wy = y - scroll;
            int left = cliffLeft(w) - wobble(wy, 11);
            int right = cliffRight(w) + wobble(wy, 23);
            int from = Math.max(0, left);
            int to = Math.min(w, right);
            for (int x = from; x < to; x++) {
                int c;
                if (x == left || x == right - 1) c = CLIFF_OUTLINE;
                else if (x < left + 3) c = RIM_LIT;
                else if (x >= right - 3) c = RIM_SHADE;
                else c = rockTexture(x, wy);
                pixels[y * w + x] = c;
            }
        }
    }

    private static int wobble(int wy, int seed) {
        int cell = 9;
        int i = Math.floorDiv(wy, cell);
        float f = Math.floorMod(wy, cell) / (float) cell;
        int a = hash(i, seed) % (WALL_WOBBLE + 1);
        int b = hash(i + 1, seed) % (WALL_WOBBLE + 1);
        return Math.round(a + (b - a) * f);
    }

    /** Voronoi boulders lit from the top-left, with cracks, snow and moss. */
    private static int rockTexture(int x, int wy) {
        final int grid = 16;
        int gx = Math.floorDiv(x, grid);
        int gy = Math.floorDiv(wy, grid);
        float d1 = Float.MAX_VALUE;
        float d2 = Float.MAX_VALUE;
        int bestId = 0;
        int secondId = 0;
        int bestFx = 0;
        int bestFy = 0;
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                int cx = gx + ox;
                int cy = gy + oy;
                int id = hash(cx, cy * 7 + 3);
                int fx = cx * grid + 2 + id % (grid - 4);
                int fy = cy * grid + 2 + (id >>> 8) % (grid - 4);
                float dx = x - fx;
                float dy = wy - fy;
                float d = dx * dx + dy * dy;
                if (d < d1) {
                    d2 = d1;
                    secondId = bestId;
                    d1 = d;
                    bestId = id;
                    bestFx = fx;
                    bestFy = fy;
                } else if (d < d2) {
                    d2 = d;
                    secondId = id;
                }
            }
        }

        float edge = (float) (Math.sqrt(d2) - Math.sqrt(d1));
        int dx = x - bestFx;
        int dy = wy - bestFy;
        // Some neighbouring stones fuse into bigger slabs with no crack between.
        boolean fused = hash(Math.min(bestId, secondId), Math.max(bestId, secondId)) % 3 == 0;
        if (edge < 1.1f && !fused) return dy > 0 ? CRACK : ROCK_SHADE[(bestId >>> 4) % 3];

        // Lit from above: top rims catch light, bottom rims fall into shadow.
        int slant = dy * 2 + dx;
        boolean rim = edge < 2.4f && !fused;

        if (bestId % 9 == 0 && dy < -1) {
            return rim && slant < 0 ? 0xffffff : 0xe1e9f0; // snow cap
        }
        if (bestId % 13 == 1 && dy < -2) {
            return (x + wy) % 3 == 0 ? 0x56702c : 0x6f8f3a; // moss / alpine grass
        }

        int tone = (bestId >>> 4) % 3;
        if (rim) return slant < 0 ? ROCK_LIT[tone] : ROCK_SHADE[tone];
        int speck = hash(x, wy) % 29;
        if (speck == 0) return ROCK_SHADE[tone];
        if (speck == 1) return ROCK_LIT[tone];
        return ROCK_BASE[tone];
    }

    // ------------------------------------------------------------------- goat

    private static final String[] GOAT_ROWS = {
        "..............H.H...",
        "..............H.H...",
        "...............H.H..",
        "..............KHWHK.",
        ".........KKKKKWWWWWK",
        "......KKKWWWWWLWWEWK",
        "....KKWWWWWWWWLWWWWK",
        "...KWWWWWWWWWWWLWWWN",
        "..KWWWWWWWWWWWWLWWK.",
        ".KWWWWWWWWWWWWWLWWK.",
        ".KWWWWWWWWWWWWWLWK..",
        ".KLWWWWWWWWWWWWLWK..",
        "..KLLLLLLLLLLLLKK...",
        "..KWWKKWWK.KWWKKWWK.",
        "..KGWKKGWK.KGWKKGWK.",
        "..KGGKKGGK.KGGKKGGK.",
        "..BBBBBBBB.BBBBBBBB.",
    };

    private static final BufferedImage GOAT = buildGoat(false);
    private static final BufferedImage GOAT_LEFT = buildGoat(true);

    private static BufferedImage buildGoat(boolean faceLeft) {
        int w = GOAT_ROWS[0].length();
        int h = GOAT_ROWS.length;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = switch (GOAT_ROWS[y].charAt(x)) {
                    case 'W' -> 0xfff6f6ee; // shaggy white coat
                    case 'L' -> 0xffd2d4cc; // coat shade
                    case 'G' -> 0xffa9adaa; // leg shade
                    case 'K' -> 0xff3e434c; // outline
                    case 'H' -> 0xff2a2624; // horns
                    case 'E' -> 0xff141418; // eye
                    case 'N' -> 0xff2f2f34; // nose
                    case 'B' -> 0xff18181c; // hooves
                    default -> 0;
                };
                img.setRGB(faceLeft ? w - 1 - x : x, y, argb);
            }
        }
        return img;
    }

    public static int goatWidth(int scale) {
        return GOAT.getWidth() * scale;
    }

    public static int goatHeight(int scale) {
        return GOAT.getHeight() * scale;
    }

    public static void drawGoat(Graphics2D g, int x, int y, boolean faceLeft, int scale) {
        BufferedImage img = faceLeft ? GOAT_LEFT : GOAT;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        // Soft pixel shadow under the hooves.
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g.setColor(Color.BLACK);
        g.fillRect(x + 2 * scale, y + img.getHeight() * scale, (img.getWidth() - 4) * scale, scale);
        g.setComposite(old);
        g.drawImage(img, x, y, img.getWidth() * scale, img.getHeight() * scale, null);
    }

    // --------------------------------------------------------------------- UI

    /** Dark translucent panel with notched pixel corners, like the buttons. */
    public static void drawPanel(Graphics2D g, int x, int y, int w, int h) {
        int s = P;
        g.setColor(new Color(16, 32, 52, 190));
        g.fillRect(x + s, y, w - 2 * s, h);
        g.fillRect(x, y + s, w, h - 2 * s);
        g.setColor(new Color(239, 240, 220));
        g.fillRect(x + s, y, w - 2 * s, s / 2);
        g.fillRect(x + s, y + h - s / 2, w - 2 * s, s / 2);
        g.fillRect(x, y + s, s / 2, h - 2 * s);
        g.fillRect(x + w - s / 2, y + s, s / 2, h - 2 * s);
    }

    /** Text with a hard one-pixel drop shadow. */
    public static void drawShadowText(Graphics2D g, String text, int x, int y, Color color) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(new Color(18, 30, 44));
        g.drawString(text, x + 3, y + 3);
        g.setColor(color);
        g.drawString(text, x, y);
    }

    static int hash(int a, int b) {
        int h = a * 0x27d4eb2d ^ b * 0x165667b1;
        h ^= h >>> 15;
        h *= 0x2c1b3c6d;
        h ^= h >>> 12;
        h *= 0x297a2d39;
        h ^= h >>> 15;
        return h & 0x7fffffff;
    }
}
