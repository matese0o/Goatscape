import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** A wall of snow sliding down the whole cliff, except for one safe gap. */
public class Avalanche {
    public static final int HEIGHT = 84;

    double y;
    final double speed;
    final int left, right, gapX, gapW;

    public Avalanche(int left, int right, int gapX, int gapW, double speed) {
        this.left = left;
        this.right = right;
        this.gapX = gapX;
        this.gapW = gapW;
        this.speed = speed;
        this.y = -HEIGHT;
    }

    public boolean hits(Rectangle r) {
        int top = (int) y + 16;
        int h = HEIGHT - 24;
        return r.intersects(new Rectangle(left, top, gapX - left, h))
                || r.intersects(new Rectangle(gapX + gapW, top, right - gapX - gapW, h));
    }

    public void draw(Graphics2D g, int tick) {
        drawSide(g, left, gapX, tick);
        drawSide(g, gapX + gapW, right, tick);
    }

    private void drawSide(Graphics2D g, int from, int to, int tick) {
        int s = MountainArt.P;
        int top = MountainArt.snap((int) y);
        for (int x = MountainArt.snap(from); x < to; x += s) {
            int col = x / s;
            // Lumpy, churning top and bottom edges.
            int lump = (MountainArt.hash(col, tick / 4) % 3 + MountainArt.hash(col / 3, 5) % 4) * s;
            int bottomLump = (MountainArt.hash(col, 9) % 3) * s;
            int y0 = top + lump;
            int y1 = top + HEIGHT - bottomLump;
            g.setColor(new Color(0x7f9fbf));
            g.fillRect(x, y0, s, s);
            g.setColor(new Color(0xffffff));
            g.fillRect(x, y0 + s, s, 3 * s);
            g.setColor(new Color(0xe6eef7));
            g.fillRect(x, y0 + 4 * s, s, Math.max(0, y1 - y0 - 8 * s));
            g.setColor(new Color(0xbfd2e6));
            g.fillRect(x, y1 - 4 * s, s, 3 * s);
            g.setColor(new Color(0x8eaccb));
            g.fillRect(x, y1 - s, s, s);
            // Snowball speckles.
            if (MountainArt.hash(col, tick / 3) % 7 == 0) {
                g.setColor(new Color(0xc9d9ea));
                g.fillRect(x, y0 + 8 * s + (MountainArt.hash(col, 2) % 6) * s, s, s);
            }
        }
    }
}
