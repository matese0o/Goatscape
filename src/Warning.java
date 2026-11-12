import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * A flashing "!" at the top of the cliff that marks where a hazard is about
 * to drop. When the timer runs out the game launches the hazard.
 */
public class Warning {
    final Rock.Kind kind; // null for an avalanche
    final int x, w, h;
    // Avalanche only: cliff bounds and the safe gap.
    final int left, right, gapX, gapW;
    double timer;

    /** Warning for a single falling rock. */
    public Warning(Rock.Kind kind, int x, int w, int h, int delay) {
        this.kind = kind;
        this.x = x;
        this.w = w;
        this.h = h;
        this.left = this.right = this.gapX = this.gapW = 0;
        this.timer = delay;
    }

    /** Warning for an avalanche spanning the cliff, with a safe gap. */
    public Warning(int left, int right, int gapX, int gapW, int delay) {
        this.kind = null;
        this.x = left;
        this.w = right - left;
        this.h = 0;
        this.left = left;
        this.right = right;
        this.gapX = gapX;
        this.gapW = gapW;
        this.timer = delay;
    }

    public boolean isAvalanche() {
        return kind == null;
    }

    public void draw(Graphics2D g) {
        boolean on = ((int) timer / 5) % 2 == 0;
        int s = MountainArt.P;
        if (isAvalanche()) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, on ? 0.45f : 0.25f));
            g.setColor(new Color(0xe8414f));
            g.fillRect(left, 0, gapX - left, 22 * s);
            g.fillRect(gapX + gapW, 0, right - gapX - gapW, 22 * s);
            g.setColor(new Color(0x5fd36a));
            g.fillRect(gapX, 0, gapW, 22 * s);
            g.setComposite(old);
            g.setFont(Fonts.get(40f));
            FontMetrics fm = g.getFontMetrics();
            String t = "AVALANCHE!  FIND THE GAP";
            if (on) MountainArt.drawShadowText(g, t, (left + right - fm.stringWidth(t)) / 2, 15 * s, Color.WHITE);
            // Arrows pointing into the safe gap.
            g.setColor(new Color(0x2e8b3a));
            int cx = MountainArt.snap(gapX + gapW / 2);
            for (int i = 0; i < 4; i++) g.fillRect(cx - (4 - i) * s, (17 + i) * s, (8 - 2 * i) * s, s);
            return;
        }
        int cx = MountainArt.snap(x + w / 2);
        int rows = kind == Rock.Kind.BOULDER ? 11 : kind == Rock.Kind.PEBBLE ? 7 : 9;
        int top = 2 * s;
        // Downward-pointing hazard triangle: dark outline, then a flashing fill.
        for (int i = 0; i <= rows; i++) {
            int half = rows - i;
            g.setColor(new Color(0x2a0a0e));
            g.fillRect(cx - (half + 1) * s, top + i * s, (2 * half + 2) * s, s);
        }
        g.setColor(on ? new Color(0xffd23f) : new Color(0xe8414f));
        for (int i = 1; i < rows; i++) {
            int half = rows - i - 1;
            if (half < 0) break;
            g.fillRect(cx - half * s, top + i * s, 2 * half * s, s);
        }
        // "!" in the wide upper part of the triangle.
        g.setColor(new Color(0x2a0a0e));
        int barH = Math.max(2, rows / 2 - 1);
        g.fillRect(cx - s, top + 2 * s, 2 * s, barH * s);
        g.fillRect(cx - s, top + (3 + barH) * s, 2 * s, s);
    }
}
