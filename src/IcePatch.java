import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Random;

/** A slippery sheet of ice on the cliff. The goat slides while standing on it. */
public class IcePatch {
    double x, y;
    final int width, height;
    private final BufferedImage sprite;

    public IcePatch(double x, double y, int width, int height, Random rand) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.sprite = build(width / MountainArt.P, height / MountainArt.P, rand);
    }

    private static BufferedImage build(int w, int h, Random rand) {
        float[] lumps = new float[10];
        for (int i = 0; i < lumps.length; i++) lumps[i] = 0.78f + rand.nextFloat() * 0.22f;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        boolean[][] in = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float nx = (x + 0.5f - w / 2f) / (w / 2f);
                float ny = (y + 0.5f - h / 2f) / (h / 2f);
                int sector = (int) ((Math.atan2(ny, nx) + Math.PI) / (2 * Math.PI) * lumps.length) % lumps.length;
                in[y][x] = nx * nx + ny * ny <= lumps[sector] * lumps[sector];
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!in[y][x]) continue;
                boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1
                        || !in[y][x - 1] || !in[y][x + 1] || !in[y - 1][x] || !in[y + 1][x];
                int c;
                if (edge) c = 0xff5f9cc4;
                else if ((x - y * 2) % 11 == 0 || (x - y * 2) % 11 == 1) c = 0xffffffff; // glints
                else if (y < h / 3) c = 0xffd6f1fb;
                else c = 0xffaedff2;
                img.setRGB(x, y, c);
            }
        }
        return img;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x + width / 8, (int) y + height / 8, width * 3 / 4, height * 3 / 4);
    }

    public void draw(Graphics2D g) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f));
        MountainArt.drawSprite(g, sprite, MountainArt.snap((int) x), MountainArt.snap((int) y), MountainArt.P);
        g.setComposite(old);
    }
}
