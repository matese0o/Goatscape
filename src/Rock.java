import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * A falling boulder, rendered as a small pixel-art sprite. Rocks use cool
 * slate colours, a thick dark outline and a drop shadow so they stand out
 * against the warm brown cliff.
 */
public class Rock {
    public int x, y;
    private final int width;
    private final int height;
    private final BufferedImage sprite;
    private final BufferedImage shadow;
    private static final Random RAND = new Random();

    private static final int OUTLINE = 0xff12151b;
    private static final int[][] TONES = {
        {0xffeef4f8, 0xffb4c3cf, 0xff7f91a1, 0xff52626f}, // slate
        {0xffe6eef6, 0xffa6b9cc, 0xff72889e, 0xff4a5b6d}, // blue slate
        {0xfff2f4f6, 0xffbcc4cc, 0xff87929c, 0xff5a646e}, // granite
    };

    public Rock(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        boolean[][] mask = buildMask(Math.max(6, width / MountainArt.P),
                Math.max(6, height / MountainArt.P));
        sprite = buildSprite(mask);
        shadow = buildShadow(mask);
    }

    /** Lumpy ellipse with one empty pixel of padding all round for the outline. */
    private static boolean[][] buildMask(int w, int h) {
        float[] lumps = new float[8];
        for (int i = 0; i < lumps.length; i++) lumps[i] = 0.82f + RAND.nextFloat() * 0.18f;

        boolean[][] inside = new boolean[h + 2][w + 2];
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                float nx = (px + 0.5f - w / 2f) / (w / 2f);
                float ny = (py + 0.5f - h / 2f) / (h / 2f);
                double angle = Math.atan2(ny, nx) + Math.PI;
                int sector = (int) (angle / (2 * Math.PI) * lumps.length) % lumps.length;
                float r = lumps[sector];
                inside[py + 1][px + 1] = nx * nx + ny * ny <= r * r;
            }
        }
        return inside;
    }

    private static boolean in(boolean[][] m, int x, int y) {
        return y >= 0 && y < m.length && x >= 0 && x < m[0].length && m[y][x];
    }

    private static BufferedImage buildSprite(boolean[][] mask) {
        int h = mask.length;
        int w = mask[0].length;
        int[] tone = TONES[RAND.nextInt(TONES.length)];
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int crackY = h / 3 + RAND.nextInt(Math.max(1, h / 3));
        int crackX = w / 3 + RAND.nextInt(Math.max(1, w / 3));

        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                boolean inside = mask[py][px];
                boolean touches = in(mask, px - 1, py) || in(mask, px + 1, py)
                        || in(mask, px, py - 1) || in(mask, px, py + 1);
                if (!inside) {
                    if (touches) img.setRGB(px, py, OUTLINE); // outer outline ring
                    continue;
                }
                boolean edge = !in(mask, px - 1, py) || !in(mask, px + 1, py)
                        || !in(mask, px, py - 1) || !in(mask, px, py + 1);
                float nx = (px + 0.5f - w / 2f) / (w / 2f);
                float ny = (py + 0.5f - h / 2f) / (h / 2f);
                float light = -nx * 0.6f - ny * 0.8f; // lit from the top-left
                int c;
                if (edge) c = light > 0 ? tone[0] : OUTLINE; // bright top rim, thick dark base
                else if (light > 0.45f) c = tone[0];
                else if (light > -0.05f) c = tone[1];
                else if (light > -0.5f) c = tone[2];
                else c = tone[3];
                int k = px - crackX;
                if (k >= 0 && k < w / 4 && py == crackY + k / 2) c = tone[3];
                img.setRGB(px, py, c);
            }
        }
        return img;
    }

    private static BufferedImage buildShadow(boolean[][] mask) {
        int h = mask.length;
        int w = mask[0].length;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                if (mask[py][px]) img.setRGB(px, py, 0xff1a0e06);
            }
        }
        return img;
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int s = MountainArt.P;
        int sx = MountainArt.snap(x) - s; // compensate for the outline padding
        int sy = MountainArt.snap(y) - s;
        int dw = sprite.getWidth() * s;
        int dh = sprite.getHeight() * s;

        Composite old = g2.getComposite();
        // Drop shadow cast onto the cliff.
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        g2.drawImage(shadow, sx + 2 * s, sy + 3 * s, dw, dh, null);
        // Speed lines trailing above the rock.
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        g2.setColor(Color.WHITE);
        g2.fillRect(sx + dw / 3, sy - 4 * s, s, 3 * s);
        g2.fillRect(sx + dw * 2 / 3, sy - 6 * s, s, 4 * s);
        g2.setComposite(old);

        g2.drawImage(sprite, sx, sy, dw, dh, null);
    }

    public Rectangle getBounds() {
        return new Rectangle(x + width / 5, y + height / 5, width * 3 / 5, height * 3 / 5);
    }
}
