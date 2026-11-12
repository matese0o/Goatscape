import java.awt.*;
import java.util.Random;

/** How to play: goal, controls, hazards, pickups and altitude zones. */
public class HelpPanel extends ScreenPanel {
    private static final int VW = 900, VH = 704;
    private static final int LEFT = 52, RIGHT = 470;

    private final PixelButton backBtn;
    private final Rock pebble, rock, boulder;
    private final IcePatch ice;

    public HelpPanel(GameFrame frame, MusicManager music) {
        super(frame, music, VW, VH);
        backBtn = addButton(new PixelButton("BACK", PixelButton.Style.GREEN, PixelButton.Icon.BACK, 30f), 240, 60);
        backBtn.addActionListener(e -> back());
        pebble = new Rock(Rock.Kind.PEBBLE, 0, 0, 28, 24, 0, 0);
        rock = new Rock(Rock.Kind.ROCK, 0, 0, 40, 36, 0, 0);
        boulder = new Rock(Rock.Kind.BOULDER, 0, 0, 52, 44, 0, 0);
        ice = new IcePatch(0, 0, 60, 36, new Random(4));
    }

    @Override
    protected void layoutButtons() {
        place(backBtn, (VW - 240) / 2, 636);
    }

    @Override
    protected void paintContent(Graphics2D g) {
        MountainArt.drawPanel(g, 0, 0, VW, 620);
        g.setFont(Fonts.get(64f));
        center(g, "HOW TO PLAY", VW / 2, 66, Color.WHITE);

        // Left column: goal, controls, zones.
        int y = 116;
        y = header(g, "GOAL", LEFT, y);
        y = line(g, "Climb as high as you can. Score grows", LEFT, y);
        y = line(g, "with altitude, and rocks fall faster.", LEFT, y);
        y = line(g, "You have 3 hearts - the run ends at zero.", LEFT, y) + 14;

        y = header(g, "CONTROLS", LEFT, y);
        y = keyLine(g, "ARROWS / WASD", "move", y);
        y = keyLine(g, "SPACE", "dash (can't be hit)", y);
        y = keyLine(g, "ESC / P", "pause", y);
        y = keyLine(g, "ENTER", "play again", y) + 14;

        y = header(g, "ALTITUDE ZONES", LEFT, y);
        y = keyLine(g, "0 m", "Pine Forest", y);
        y = keyLine(g, "150 m", "Red Cliffs", y);
        y = keyLine(g, "350 m", "Snowfields - more ice", y);
        keyLine(g, "600 m", "Starry Summit", y);

        // Right column: hazards and pickups, with their sprites.
        y = 116;
        y = header(g, "HAZARDS", RIGHT, y);
        y = hazard(g, pebble, "PEBBLE", "small but very fast", y);
        y = hazard(g, rock, "ROCK", "watch for the ! marker", y);
        y = hazard(g, boulder, "BOULDER", "huge, bounces off the edges", y);
        drawAvalancheIcon(g, RIGHT, y - 6);
        y = labelPair(g, "AVALANCHE", "from 200 m - find the gap", y);
        ice.x = RIGHT - 6;
        ice.y = y - 10;
        ice.draw(g);
        y = labelPair(g, "ICE", "slippery - you slide", y) + 22;

        y = header(g, "PICKUPS", RIGHT, y);
        MountainArt.drawSprite(g, Pickup.EDELWEISS_IMG, RIGHT + 4, y - 8, 4);
        y = labelPair(g, "EDELWEISS", "+250 points", y);
        MountainArt.drawSprite(g, Pickup.HELMET_IMG, RIGHT + 2, y - 4, 4);
        y = labelPair(g, "HELMET", "blocks one hit", y);
        MountainArt.drawSprite(g, Pickup.SLOWMO_IMG, RIGHT + 6, y - 10, 4);
        labelPair(g, "HOURGLASS", "slow motion for 5 s", y);
    }

    private static int header(Graphics2D g, String title, int x, int y) {
        g.setFont(Fonts.get(32f));
        text(g, title, x, y, GOLD);
        return y + 32;
    }

    private static int line(Graphics2D g, String s, int x, int y) {
        g.setFont(Fonts.get(24f));
        text(g, s, x, y, SOFT);
        return y + 26;
    }

    private static int keyLine(Graphics2D g, String key, String action, int y) {
        g.setFont(Fonts.get(24f));
        text(g, key, LEFT, y, Color.WHITE);
        text(g, action, LEFT + 170, y, SOFT);
        return y + 26;
    }

    private int hazard(Graphics2D g, Rock r, String name, String desc, int y) {
        r.x = RIGHT + 4 + (52 - r.width) / 2.0;
        r.y = y - 4 + (40 - r.height) / 2.0;
        r.draw(g);
        return labelPair(g, name, desc, y);
    }

    /** Name and description to the right of an icon; each row is 48 px tall. */
    private static int labelPair(Graphics2D g, String name, String desc, int y) {
        g.setFont(Fonts.get(26f));
        text(g, name, RIGHT + 76, y + 14, Color.WHITE);
        g.setFont(Fonts.get(22f));
        text(g, desc, RIGHT + 76, y + 36, SOFT);
        return y + 48;
    }

    private static void drawAvalancheIcon(Graphics2D g, int x, int y) {
        int s = MountainArt.P;
        g.setColor(new Color(0x8eaccb));
        g.fillRect(x, y + 4 * s, 15 * s, 8 * s);
        g.setColor(new Color(0xe6eef7));
        g.fillRect(x, y + 3 * s, 15 * s, 7 * s);
        g.setColor(Color.WHITE);
        for (int i = 0; i < 15; i++) g.fillRect(x + i * s, y + (i % 3) * s, s, 4 * s);
        g.setColor(new Color(0x5fd36a));
        g.fillRect(x + 6 * s, y, 3 * s, 12 * s);
    }
}
