import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/** A square pixel of dust, snow or sparkle, or a floating text label. */
public class Particle {
    double x, y, vx, vy;
    int life;
    final int maxLife;
    final int color;
    final int size;
    final boolean gravity;
    final String text;

    public Particle(double x, double y, double vx, double vy, int life, int color, int size, boolean gravity) {
        this(x, y, vx, vy, life, color, size, gravity, null);
    }

    private Particle(double x, double y, double vx, double vy, int life, int color, int size,
            boolean gravity, String text) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.color = color;
        this.size = size;
        this.gravity = gravity;
        this.text = text;
    }

    /** Floating label such as "+250" that drifts up and fades. */
    public static Particle text(double x, double y, String text, int color) {
        return new Particle(x, y, 0, -1.2, 60, color, 0, false, text);
    }

    /** Returns false once the particle has expired. */
    public boolean update() {
        x += vx;
        y += vy;
        if (gravity) vy += 0.35;
        vx *= 0.97;
        return --life > 0;
    }

    public void draw(Graphics2D g) {
        Composite old = g.getComposite();
        float a = Math.min(1f, life / (float) Math.max(1, maxLife / 2));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, a)));
        if (text != null) {
            g.setFont(Fonts.get(30f));
            FontMetrics fm = g.getFontMetrics();
            MountainArt.drawShadowText(g, text, (int) x - fm.stringWidth(text) / 2, (int) y, new Color(color));
        } else {
            g.setColor(new Color(color));
            g.fillRect(MountainArt.snap((int) x), MountainArt.snap((int) y), size, size);
        }
        g.setComposite(old);
    }
}
