import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Player {
    public static final int WIDTH = MountainArt.goatWidth(MountainArt.P);
    public static final int HEIGHT = MountainArt.goatHeight(MountainArt.P);

    public double x, y;
    /** Velocity, used for sliding on ice and for dashing. */
    public double vx, vy;
    public boolean facingLeft = false;
    public boolean moving = false;
    private final Skin skin;
    private int tick = 0;

    public Player(int x, int y, Skin skin) {
        this.x = x;
        this.y = y;
        this.skin = skin;
    }

    /** Advances the walking animation. */
    public void update() {
        tick = moving ? tick + 1 : 0;
    }

    public void draw(Graphics g) {
        // Alternate leg frames while moving, with a one-pixel bob on the stride.
        int frame = (tick / 6) % 2;
        int bob = frame == 1 ? MountainArt.P : 0;
        MountainArt.drawGoat((Graphics2D) g, MountainArt.snap((int) x), MountainArt.snap((int) y) - bob,
                facingLeft, MountainArt.P, frame, skin);
    }

    public int centerX() {
        return (int) x + WIDTH / 2;
    }

    public int centerY() {
        return (int) y + HEIGHT / 2;
    }

    public Rectangle getBounds() {
        // The torso and legs only, so horns and beard brushing a rock don't count.
        int p = MountainArt.P;
        return new Rectangle((int) x + 5 * p, (int) y + 6 * p, 18 * p, 11 * p);
    }
}
