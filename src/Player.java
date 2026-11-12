import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Player {
    public static final int WIDTH = MountainArt.goatWidth(MountainArt.P);
    public static final int HEIGHT = MountainArt.goatHeight(MountainArt.P);

    public int x, y;
    public boolean facingLeft = false;
    public boolean moving = false;
    private int tick = 0;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Advances the little climbing bob animation. */
    public void update() {
        tick = moving ? tick + 1 : 0;
    }

    public void draw(Graphics g) {
        int bob = (tick / 6) % 2 == 1 ? MountainArt.P : 0;
        MountainArt.drawGoat((Graphics2D) g, MountainArt.snap(x), MountainArt.snap(y) - bob,
                facingLeft, MountainArt.P);
    }

    public Rectangle getBounds() {
        return new Rectangle(x + 8, y + 16, WIDTH - 16, HEIGHT - 20);
    }
}
