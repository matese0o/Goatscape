import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** A collectible on the cliff face: bonus flower, helmet or slow-motion hourglass. */
public class Pickup {
    public enum Kind { EDELWEISS, HELMET, SLOWMO }

    public static final BufferedImage EDELWEISS_IMG = MountainArt.sprite(new String[]{
        "...KWK...",
        "..KWWWK..",
        "KWKWYWKWK",
        "KWWYYYWWK",
        ".KWYOYWK.",
        "KWWYYYWWK",
        "KWKWYWKWK",
        "..KWWWK..",
        "...KGK...",
    }, "KWYOG", 0x3b4a3a, 0xfdfdf4, 0xffd23f, 0xe09a1a, 0x4f8a3a);

    public static final BufferedImage HELMET_IMG = MountainArt.sprite(new String[]{
        "...KKKK...",
        "..KYYYYK..",
        ".KYWYYYYK.",
        ".KYWYYYYK.",
        ".KYYYYYYK.",
        "KKKKKKKKKK",
        "KOOOOOOOOK",
        ".KKKKKKKK.",
    }, "KYWO", 0x5a3a10, 0xffc93c, 0xfff3b0, 0xe0952a);

    public static final BufferedImage SLOWMO_IMG = MountainArt.sprite(new String[]{
        "KKKKKKK",
        "KWCCCWK",
        ".KCCCK.",
        "..KCK..",
        "...K...",
        "..KSK..",
        ".KWSWK.",
        "KWSSSWK",
        "KKKKKKK",
    }, "KWCS", 0x1d3550, 0xd8f1ff, 0x7fd3ff, 0x3a9ad9);

    final Kind kind;
    double x, y;

    public Pickup(Kind kind, double x, double y) {
        this.kind = kind;
        this.x = x;
        this.y = y;
    }

    public BufferedImage image() {
        return switch (kind) {
            case EDELWEISS -> EDELWEISS_IMG;
            case HELMET -> HELMET_IMG;
            case SLOWMO -> SLOWMO_IMG;
        };
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - 6, (int) y - 6, 48, 48);
    }

    public void draw(Graphics2D g, int tick) {
        BufferedImage img = image();
        int s = MountainArt.P;
        int bob = ((tick / 10) % 2) * s;
        int dx = MountainArt.snap((int) x);
        int dy = MountainArt.snap((int) y) - bob;
        // Soft glow so pickups read clearly against the rock.
        Composite old = g.getComposite();
        float pulse = 0.25f + 0.15f * (float) Math.sin(tick / 6.0);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
        g.setColor(new Color(0xfff6c4));
        int pw = img.getWidth() * s, ph = img.getHeight() * s;
        g.fillRect(dx - 2 * s, dy - s, pw + 4 * s, ph + 2 * s);
        g.fillRect(dx - s, dy - 2 * s, pw + 2 * s, ph + 4 * s);
        g.setComposite(old);
        MountainArt.drawSprite(g, img, dx, dy, s);
    }
}
