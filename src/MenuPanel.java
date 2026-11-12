import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Random;
import javax.swing.*;

/** Home screen: title, stats, goat skin picker and the main buttons. */
public class MenuPanel extends JPanel {

    /** The menu is laid out for this height and centred vertically in taller windows. */
    private static final int LAYOUT_H = 704;
    private static final int COL_W = 456;

    private final GameFrame frame;
    private final MusicManager music;
    private final PixelButton playBtn, helpBtn, settingsBtn, exitBtn, prevSkin, nextSkin;
    private final Timer anim;
    private final Random rand = new Random();
    private final float[][] flakes = new float[70][3]; // x, y, speed (0-1 of panel)
    private Skin shown = Save.skin();
    private int tick;

    public MenuPanel(GameFrame frame, MusicManager music) {
        this.frame = frame;
        this.music = music;
        setLayout(null);
        setFocusable(true);
        MountainArt.setBiome(0);
        music.playMusic("menu-music.wav", true);

        playBtn = add(new PixelButton("PLAY", PixelButton.Style.GREEN, PixelButton.Icon.PLAY, 48f), COL_W, 76);
        helpBtn = add(new PixelButton("HOW TO PLAY", PixelButton.Style.BLUE, PixelButton.Icon.HELP, 28f), 220, 58);
        settingsBtn = add(new PixelButton("SETTINGS", PixelButton.Style.BLUE, PixelButton.Icon.GEAR, 28f), 220, 58);
        exitBtn = add(new PixelButton("EXIT", PixelButton.Style.RED, PixelButton.Icon.EXIT, 28f), 220, 54);
        prevSkin = add(new PixelButton("", PixelButton.Style.STONE, PixelButton.Icon.LEFT, 27f), 52, 52);
        nextSkin = add(new PixelButton("", PixelButton.Style.STONE, PixelButton.Icon.RIGHT, 27f), 52, 52);

        playBtn.addActionListener(e -> startGame());
        helpBtn.addActionListener(e -> go(new HelpPanel(frame, music)));
        settingsBtn.addActionListener(e -> go(new SettingsPanel(frame, music)));
        exitBtn.addActionListener(e -> {
            music.playSFX("click.wav");
            System.exit(0);
        });
        prevSkin.addActionListener(e -> cycleSkin(-1));
        nextSkin.addActionListener(e -> cycleSkin(1));

        bind("ENTER", "play", this::startGame);
        bind("SPACE", "play", this::startGame);
        bind("LEFT", "prev", () -> cycleSkin(-1));
        bind("A", "prev", () -> cycleSkin(-1));
        bind("RIGHT", "next", () -> cycleSkin(1));
        bind("D", "next", () -> cycleSkin(1));

        for (float[] f : flakes) {
            f[0] = rand.nextFloat();
            f[1] = rand.nextFloat();
            f[2] = 0.3f + rand.nextFloat() * 0.7f;
        }
        anim = new Timer(33, e -> {
            tick++;
            for (float[] f : flakes) {
                f[1] += 0.002f * f[2];
                f[0] += 0.0006f * (float) Math.sin((tick + f[2] * 100) / 30.0);
                if (f[1] > 1) {
                    f[1] = 0;
                    f[0] = rand.nextFloat();
                }
            }
            repaint();
        });
        anim.start();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutButtons();
            }
        });
        SwingUtilities.invokeLater(this::layoutButtons);
    }

    private PixelButton add(PixelButton b, int w, int h) {
        b.setSize(w, h);
        super.add(b);
        return b;
    }

    private void bind(String key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private int top() {
        return Math.max(0, (getHeight() - LAYOUT_H) / 2);
    }

    private void layoutButtons() {
        int cx = getWidth() / 2;
        int t = top();
        playBtn.setLocation(cx - COL_W / 2, t + 446);
        helpBtn.setLocation(cx - COL_W / 2, t + 532);
        settingsBtn.setLocation(cx + COL_W / 2 - 220, t + 532);
        exitBtn.setLocation(cx - 110, t + 600);
        prevSkin.setLocation(cx - 168 - 52, t + 316);
        nextSkin.setLocation(cx + 168, t + 316);
    }

    private void cycleSkin(int dir) {
        music.playSFX("click.wav");
        shown = dir < 0 ? shown.previous() : shown.next();
        if (shown.unlocked()) Save.setSkin(shown);
        repaint();
    }

    private void go(JComponent next) {
        music.playSFX("click.wav");
        anim.stop();
        frame.showPanel(next);
    }

    private void startGame() {
        music.playSFX("click.wav");
        anim.stop();
        music.playMusic("game-music.wav", true);
        frame.showPanel(new GamePanel(frame, music));
    }

    // ----------------------------------------------------------------- render

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        int w = getWidth();
        int cx = w / 2;
        int t = top();
        MountainArt.drawBackground(g2d, w, getHeight(), tick * 2);
        drawSnow(g2d);

        // Title card.
        int cardW = 520;
        MountainArt.drawPanel(g2d, cx - cardW / 2, t + 16, cardW, 144);
        g2d.setFont(Fonts.get(104f));
        center(g2d, "PEAKFALL", cx, t + 100, Color.WHITE);
        g2d.setFont(Fonts.get(28f));
        center(g2d, "Climb high. Dodge the rocks.", cx, t + 140, new Color(225, 235, 245));

        // Stats row.
        String[][] stats = {
            {"BEST SCORE", String.valueOf(Save.bestScore())},
            {"BEST ALTITUDE", Save.bestAltitude() + " m"},
            {"RUNS", String.valueOf(Save.runs())},
        };
        int sw = (COL_W - 24) / 3;
        for (int i = 0; i < 3; i++) {
            int x = cx - COL_W / 2 + i * (sw + 12);
            MountainArt.drawPanel(g2d, x, t + 170, sw, 62);
            g2d.setFont(Fonts.get(20f));
            center(g2d, stats[i][0], x + sw / 2, t + 192, new Color(170, 195, 220));
            g2d.setFont(Fonts.get(32f));
            center(g2d, stats[i][1], x + sw / 2, t + 224, new Color(255, 214, 102));
        }

        drawGoatStage(g2d, cx, t);

        g2d.setFont(Fonts.get(22f));
        String hint = "ENTER  PLAY      A / D  CHANGE SKIN";
        int hw = g2d.getFontMetrics().stringWidth(hint);
        MountainArt.drawPanel(g2d, cx - hw / 2 - 18, t + 664, hw + 36, 36);
        center(g2d, hint, cx, t + 690, new Color(235, 242, 250));
        g2d.dispose();
    }

    private void drawGoatStage(Graphics2D g2d, int cx, int t) {
        int scale = 5;
        int goatW = MountainArt.goatWidth(scale);
        int goatH = MountainArt.goatHeight(scale);
        int goatX = MountainArt.snap(cx - goatW / 2);
        int ledgeY = MountainArt.snap(t + 380);
        drawLedge(g2d, goatX - 36, ledgeY, goatW + 72);

        // Idle animation: an occasional step.
        int frame = (tick / 12) % 8 == 0 ? 1 : 0;
        if (shown.unlocked()) {
            MountainArt.drawGoat(g2d, goatX, ledgeY - goatH, false, scale, frame, shown);
        } else {
            Composite old = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            MountainArt.drawGoat(g2d, goatX, ledgeY - goatH, false, scale, 0, Skin.SHADOW);
            g2d.setComposite(old);
            drawLock(g2d, cx, ledgeY - goatH / 2 - 10);
        }

        // Skin name and unlock progress.
        g2d.setFont(Fonts.get(28f));
        String name = shown.label + (shown == Save.skin() ? "  -  EQUIPPED" : "");
        center(g2d, shown.unlocked() ? name : shown.label + "  -  REACH " + shown.unlockAltitude + " m",
                cx, ledgeY + 56, shown.unlocked() ? Color.WHITE : new Color(255, 160, 150));

        Skin nextLocked = null;
        for (Skin s : Skin.values()) {
            if (!s.unlocked()) {
                nextLocked = s;
                break;
            }
        }
        if (nextLocked != null) {
            int bw = 300, bx = cx - bw / 2, by = t + 262;
            double p = Math.min(1, Save.bestAltitude() / (double) nextLocked.unlockAltitude);
            g2d.setFont(Fonts.get(20f));
            center(g2d, "NEXT SKIN: " + nextLocked.label + " AT " + nextLocked.unlockAltitude + " m",
                    cx, by - 6, new Color(235, 242, 250));
            g2d.setColor(new Color(0x0d1826));
            g2d.fillRect(bx, by, bw, 10);
            g2d.setColor(new Color(0xffd23f));
            g2d.fillRect(bx + 2, by + 2, (int) ((bw - 4) * p), 6);
        }
    }

    private void drawLock(Graphics2D g, int cx, int cy) {
        int s = MountainArt.P;
        int x = MountainArt.snap(cx - 5 * s);
        int y = MountainArt.snap(cy - 4 * s);
        g.setColor(new Color(0x1a2230));
        g.fillRect(x + 2 * s, y - 4 * s, 6 * s, s);
        g.fillRect(x + s, y - 3 * s, s, 4 * s);
        g.fillRect(x + 8 * s, y - 3 * s, s, 4 * s);
        g.fillRect(x, y, 10 * s, 8 * s);
        g.setColor(new Color(0xffd23f));
        g.fillRect(x + s, y + s, 8 * s, 6 * s);
        g.setColor(new Color(0x1a2230));
        g.fillRect(x + 4 * s, y + 3 * s, 2 * s, 3 * s);
    }

    private void drawSnow(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, 200));
        for (float[] f : flakes) {
            int size = f[2] > 0.7f ? 8 : 4;
            g.fillRect(MountainArt.snap((int) (f[0] * getWidth())), MountainArt.snap((int) (f[1] * getHeight())), size, size);
        }
    }

    private static void center(Graphics2D g, String text, int cx, int y, Color c) {
        int w = g.getFontMetrics().stringWidth(text);
        MountainArt.drawShadowText(g, text, cx - w / 2, y, c);
    }

    /** A chunky brown rock ledge for the menu goat to stand on. */
    private void drawLedge(Graphics2D g, int x, int y, int w) {
        int s = MountainArt.P;
        x = MountainArt.snap(x);
        w = MountainArt.snap(w);
        g.setColor(new Color(58, 38, 26));
        g.fillRect(x + s, y, w - 2 * s, 7 * s);
        g.fillRect(x, y + s, w, 5 * s);
        g.setColor(new Color(168, 116, 74));
        g.fillRect(x + s, y + s, w - 2 * s, 5 * s);
        g.setColor(new Color(211, 159, 108));
        g.fillRect(x + s, y + s, w - 2 * s, s);
        g.setColor(new Color(122, 82, 52));
        g.fillRect(x + s, y + 5 * s, w - 2 * s, s);
        g.setColor(new Color(74, 47, 31));
        for (int cx = x + 7 * s; cx < x + w - 4 * s; cx += 9 * s) {
            g.fillRect(cx, y + 2 * s, s, 3 * s);
        }
        g.setColor(new Color(225, 233, 240));
        g.fillRect(x + 2 * s, y, 6 * s, s);
        g.fillRect(x + w - 10 * s, y, 5 * s, s);
    }
}
