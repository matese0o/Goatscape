import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class GamePanel extends JPanel implements ActionListener, KeyListener {

    private static final int BUTTON_W = 260;
    private static final int BUTTON_H = 64;
    /** Width of the climbable cliff in the original 700px window. */
    private static final int BASE_PLAY_W = 476;
    private static final int MAX_LIVES = 3;
    private static final int COUNTDOWN_STEP = 50; // frames per countdown number
    private static final int DASH_FRAMES = 8;
    private static final int DASH_COOLDOWN = 90;
    private static final int SLOWMO_FRAMES = 300;

    /** Altitude (m) where each zone starts, and its name. */
    private static final int[] BIOME_START = {0, 150, 350, 600};
    private static final String[] BIOME_NAME = {"PINE FOREST", "RED CLIFFS", "SNOWFIELDS", "STARRY SUMMIT"};

    private static final BufferedImage HEART = MountainArt.sprite(new String[]{
        ".KK.KK.",
        "KRWKRRK",
        "KRRRRRK",
        ".KRRRK.",
        "..KRK..",
        "...K...",
    }, "KRW", 0x3a0d14, 0xe8414f, 0xffb3b8);
    private static final BufferedImage HEART_EMPTY = MountainArt.sprite(new String[]{
        ".KK.KK.",
        "KRRKRRK",
        "KRRRRRK",
        ".KRRRK.",
        "..KRK..",
        "...K...",
    }, "KR", 0x3a0d14, 0x5a3038);

    private final GameFrame frame;
    private final MusicManager music;
    private final Timer timer;
    private final Random rand = new Random();
    private final Skin skin = Save.skin();

    private Player player;
    private final List<Rock> rocks = new ArrayList<>();
    private final List<Warning> warnings = new ArrayList<>();
    private final List<Avalanche> avalanches = new ArrayList<>();
    private final List<IcePatch> icePatches = new ArrayList<>();
    private final List<Pickup> pickups = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private boolean gameOver, paused, newBest;
    /** Showing the how-to-play card; the countdown starts when the player presses START. */
    private boolean briefing;
    private Rock briefRock, briefBoulder;
    private IcePatch briefIce;
    private Skin unlockedSkin; // a skin this run unlocked, if any

    private double climbed; // frames climbed (slower in slow-mo); altitude = climbed / 10
    private int bonus;      // points from edelweiss
    private int lives = MAX_LIVES;
    private boolean helmet;
    private int invuln, slowmo, dashTime, dashCooldown, shake;
    private int dashDx, dashDy;
    private int countdown = 3 * COUNTDOWN_STEP;
    private int goTime;
    private int biome;
    private String bannerTitle = "", bannerSub = "";
    private int bannerTime;
    private double scroll;
    private int tick, lastRumble = -999, hoofTick;
    private int nextAvalanche = 500, nextPickup = 300, nextIce = 200;

    private boolean movingLeft, movingRight, movingUp, movingDown;

    private final PixelButton resumeBtn, playAgainBtn, menuBtn, startBtn;

    /** Starts straight into the countdown (used by PLAY AGAIN). */
    public GamePanel(GameFrame frame, MusicManager music) {
        this(frame, music, false);
    }

    /** @param showBriefing show the how-to-play card first, with a START button */
    public GamePanel(GameFrame frame, MusicManager music, boolean showBriefing) {
        this.frame = frame;
        this.briefing = showBriefing;
        this.music = music;

        setFocusable(true);
        setLayout(null);
        addKeyListener(this);
        MountainArt.setBiome(0);

        resumeBtn = button("RESUME", PixelButton.Style.GREEN, PixelButton.Icon.PLAY);
        playAgainBtn = button("PLAY AGAIN", PixelButton.Style.GREEN, PixelButton.Icon.PLAY);
        menuBtn = button("MENU", PixelButton.Style.STONE, PixelButton.Icon.BACK);
        startBtn = new PixelButton("START CLIMB", PixelButton.Style.GREEN, PixelButton.Icon.PLAY, 40f);
        startBtn.setSize(320, 72);
        add(startBtn);
        startBtn.addActionListener(e -> {
            music.playSFX("click.wav");
            startCountdown();
        });
        resumeBtn.addActionListener(e -> {
            music.playSFX("click.wav");
            setPaused(false);
        });
        playAgainBtn.addActionListener(e -> {
            music.playSFX("click.wav");
            restart();
        });
        menuBtn.addActionListener(e -> {
            music.playSFX("click.wav");
            leave(new MenuPanel(frame, music));
        });

        timer = new Timer(16, this);
        timer.start();
        if (!briefing) music.playSFX("beep");

        SwingUtilities.invokeLater(this::requestFocusInWindow);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                if (player == null) initPlayer();
                else keepPlayerOnCliff();
                layoutButtons();
                repaint();
            }
        });
    }

    private PixelButton button(String text, PixelButton.Style style, PixelButton.Icon icon) {
        PixelButton b = new PixelButton(text, style, icon, 34f);
        b.setSize(BUTTON_W, BUTTON_H);
        b.setVisible(false);
        add(b);
        return b;
    }

    private void startCountdown() {
        if (!briefing) return;
        briefing = false;
        countdown = 3 * COUNTDOWN_STEP;
        music.playSFX("beep");
        layoutButtons();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void restart() {
        leave(new GamePanel(frame, music));
    }

    /** Stops this panel's game loop and switches to another screen. */
    private void leave(JComponent next) {
        timer.stop();
        frame.showPanel(next);
    }

    private void initPlayer() {
        int playerX = (getWidth() - Player.WIDTH) / 2;
        int playerY = getHeight() - Player.HEIGHT - 60;
        player = new Player(playerX, playerY, skin);
    }

    // ------------------------------------------------------------------ state

    private int altitude() {
        return (int) (climbed / 10);
    }

    private int score() {
        return (int) climbed + bonus;
    }

    private int left() {
        return MountainArt.playLeft(getWidth());
    }

    private int right() {
        return MountainArt.playRight(getWidth());
    }

    /** Keeps the goat on the cliff face, including after the window is resized. */
    private void keepPlayerOnCliff() {
        int left = left();
        int right = right();
        if (player.x < left) { player.x = left; player.vx = 0; }
        if (player.x > right - Player.WIDTH) { player.x = right - Player.WIDTH; player.vx = 0; }
        if (player.y < 0) { player.y = 0; player.vy = 0; }
        if (player.y > getHeight() - Player.HEIGHT) { player.y = getHeight() - Player.HEIGHT; player.vy = 0; }
    }

    private void setPaused(boolean p) {
        if (gameOver) return;
        paused = p;
        layoutButtons();
        if (!p) SwingUtilities.invokeLater(this::requestFocusInWindow);
        repaint();
    }

    private void layoutButtons() {
        startBtn.setVisible(briefing);
        startBtn.setLocation((getWidth() - startBtn.getWidth()) / 2, briefingTop() + BRIEF_H + 18);
        resumeBtn.setVisible(paused && !gameOver);
        playAgainBtn.setVisible(gameOver);
        menuBtn.setVisible(paused || gameOver);
        int x = (getWidth() - BUTTON_W) / 2;
        int y = getHeight() / 2 + 10;
        JButton first = gameOver ? playAgainBtn : resumeBtn;
        first.setLocation(x, y);
        menuBtn.setLocation(x, y + BUTTON_H + 14);
    }

    // ------------------------------------------------------------------- loop

    @Override
    public void actionPerformed(ActionEvent e) {
        tick++;
        if (!paused && !gameOver && !briefing && player != null) {
            if (countdown > 0) updateCountdown();
            else updateGame();
        }
        if (!paused) updateEffects();
        repaint();
    }

    private void updateCountdown() {
        countdown--;
        if (countdown > 0 && countdown % COUNTDOWN_STEP == 0) music.playSFX("beep");
        if (countdown == 0) {
            music.playSFX("go");
            goTime = 45;
        }
    }

    private void updateEffects() {
        particles.removeIf(p -> !p.update());
        if (shake > 0) shake--;
        if (goTime > 0) goTime--;
        if (bannerTime > 0) bannerTime--;
    }

    private void updateGame() {
        double ws = slowmo > 0 ? 0.5 : 1.0; // world speed
        climbed += ws;
        scroll += 5 * ws;
        int alt = altitude();
        updateBiome(alt);
        movePlayer();

        int left = left();
        int right = right();
        spawnHazards(alt, ws, left, right);

        for (Iterator<Warning> it = warnings.iterator(); it.hasNext();) {
            Warning w = it.next();
            w.timer -= ws;
            if (w.timer <= 0) {
                it.remove();
                launch(w, alt);
            }
        }

        for (Rock r : rocks) {
            r.y += r.speed * ws;
            if (r.vx != 0) {
                r.x += r.vx * ws;
                if (r.x < left || r.x + r.width > right) {
                    r.x = Math.max(left, Math.min(r.x, right - r.width));
                    r.vx = -r.vx;
                    music.playSFX("bounce");
                    burst(r.x + (r.vx > 0 ? 0 : r.width), r.y + r.height / 2.0,
                            new int[]{0x8a6a4a, 0xb4c3cf}, 6, 3);
                }
            }
        }
        for (Avalanche a : avalanches) {
            a.y += a.speed * ws;
            if (tick % 2 == 0) {
                int x = a.left + rand.nextInt(Math.max(1, a.right - a.left));
                if (x < a.gapX || x > a.gapX + a.gapW) {
                    particles.add(new Particle(x, a.y + Avalanche.HEIGHT, rand.nextDouble() * 2 - 1,
                            rand.nextDouble() * 2, 30, 0xffffff, 4, true));
                }
            }
        }
        for (IcePatch ice : icePatches) ice.y += 5 * ws;
        for (Pickup p : pickups) p.y += 5 * ws;

        checkCollisions();
        if (gameOver) return;

        if (invuln > 0) invuln--;
        if (slowmo > 0) slowmo--;
        if (dashCooldown > 0) dashCooldown--;

        int bottom = getHeight() + 150;
        rocks.removeIf(r -> r.y > bottom);
        avalanches.removeIf(a -> a.y > bottom);
        icePatches.removeIf(i -> i.y > bottom);
        pickups.removeIf(p -> p.y > bottom);
    }

    private void updateBiome(int alt) {
        int b = 0;
        for (int i = 0; i < BIOME_START.length; i++) if (alt >= BIOME_START[i]) b = i;
        if (b != biome) {
            biome = b;
            MountainArt.setBiome(b);
            bannerTitle = BIOME_NAME[b];
            bannerSub = BIOME_START[b] + " m";
            bannerTime = 160;
            music.playSFX("unlock");
        }
    }

    private boolean onIce() {
        Point feet = new Point(player.centerX(), (int) player.y + Player.HEIGHT - 8);
        for (IcePatch ice : icePatches) if (ice.getBounds().contains(feet)) return true;
        return false;
    }

    private void movePlayer() {
        int ix = (movingRight ? 1 : 0) - (movingLeft ? 1 : 0);
        int iy = (movingDown ? 1 : 0) - (movingUp ? 1 : 0);
        boolean ice = onIce();

        if (dashTime > 0) {
            dashTime--;
            player.vx = dashDx * 22;
            player.vy = dashDy * 22;
            particles.add(new Particle(player.centerX(), player.centerY(), 0, 0, 14, 0xffffff, 8, false));
        } else if (ice) {
            // Momentum: input only nudges the goat, and it keeps sliding.
            player.vx = Math.max(-11, Math.min(11, player.vx * 0.95 + ix * 0.7));
            player.vy = Math.max(-11, Math.min(11, player.vy * 0.95 + iy * 0.7));
            if (tick % 6 == 0 && (Math.abs(player.vx) > 2 || Math.abs(player.vy) > 2)) {
                particles.add(new Particle(player.centerX(), player.y + Player.HEIGHT, 0, -0.5, 18, 0xd6f1fb, 4, false));
            }
        } else {
            player.vx = ix * 8;
            player.vy = iy * 8;
        }
        player.x += player.vx;
        player.y += player.vy;
        if (dashTime > 0) player.facingLeft = dashDx < 0 || (dashDx == 0 && player.facingLeft);
        else if (ix != 0) player.facingLeft = ix < 0;
        player.moving = ix != 0 || iy != 0 || dashTime > 0;
        player.update();
        keepPlayerOnCliff();

        if ((ix != 0 || iy != 0) && dashTime == 0 && !ice) {
            if (hoofTick++ % 12 == 0) music.playSFX("hoof");
        } else {
            hoofTick = 0;
        }
    }

    private void tryDash() {
        if (player == null || countdown > 0 || paused || gameOver || dashTime > 0 || dashCooldown > 0) return;
        dashDx = (movingRight ? 1 : 0) - (movingLeft ? 1 : 0);
        dashDy = (movingDown ? 1 : 0) - (movingUp ? 1 : 0);
        if (dashDx == 0 && dashDy == 0) dashDx = player.facingLeft ? -1 : 1;
        dashTime = DASH_FRAMES;
        dashCooldown = DASH_COOLDOWN;
        music.playSFX("dash");
    }

    // ---------------------------------------------------------------- hazards

    private double fallSpeed(int alt) {
        return 4.5 + Math.min(6, alt / 100.0);
    }

    private void spawnHazards(int alt, double ws, int left, int right) {
        double widthFactor = (right - left) / (double) BASE_PLAY_W;
        double ramp = 1 + Math.min(1.2, alt / 400.0); // up to 2.2x as many rocks
        if (rand.nextDouble() < widthFactor * ramp / 50.0 * ws) queueRock(alt, left, right);

        if (alt >= 300 && rand.nextDouble() < ws && --nextAvalanche <= 0) {
            int gapW = Player.WIDTH * 3 + 40;
            int gapX = left + rand.nextInt(Math.max(1, right - left - gapW));
            warnings.add(new Warning(left, right, MountainArt.snap(gapX), gapW, 130));
            nextAvalanche = 900 + rand.nextInt(600) - Math.min(300, alt / 4);
        }

        if (alt >= 80 && rand.nextDouble() < ws && --nextIce <= 0) {
            int w = 140 + rand.nextInt(120);
            int h = 60 + rand.nextInt(50);
            int x = left + rand.nextInt(Math.max(1, right - left - w));
            icePatches.add(new IcePatch(x, -h, w, h, rand));
            nextIce = (biome == 2 ? 140 : 320) + rand.nextInt(200);
        }

        if (rand.nextDouble() < ws && --nextPickup <= 0) {
            double r = rand.nextDouble();
            Pickup.Kind kind = r < 0.55 ? Pickup.Kind.EDELWEISS
                    : (r < 0.78 && !helmet) ? Pickup.Kind.HELMET : Pickup.Kind.SLOWMO;
            int x = left + 20 + rand.nextInt(Math.max(1, right - left - 80));
            pickups.add(new Pickup(kind, x, -40));
            nextPickup = 300 + rand.nextInt(300);
        }
    }

    private void queueRock(int alt, int left, int right) {
        double r = rand.nextDouble();
        Rock.Kind kind = (alt >= 120 && r < 0.10) ? Rock.Kind.BOULDER : r < 0.42 ? Rock.Kind.PEBBLE : Rock.Kind.ROCK;
        int w, h, delay;
        switch (kind) {
            case PEBBLE -> { w = 24 + rand.nextInt(10); h = 22 + rand.nextInt(8); delay = 45; }
            case BOULDER -> { w = 88 + rand.nextInt(24); h = 80 + rand.nextInt(20); delay = 70; }
            default -> { w = 40 + rand.nextInt(36); h = 36 + rand.nextInt(32); delay = 55; }
        }
        int x = left + rand.nextInt(Math.max(1, right - left - w));
        for (Warning wn : warnings) {
            if (!wn.isAvalanche() && Math.abs(wn.x - x) < Math.max(w, wn.w) + 20) return;
        }
        for (Rock rock : rocks) {
            if (rock.y < h + 40 && Math.abs(rock.x - x) < w + 20) return;
        }
        warnings.add(new Warning(kind, x, w, h, delay));
    }

    private void launch(Warning w, int alt) {
        double fall = fallSpeed(alt);
        if (w.isAvalanche()) {
            avalanches.add(new Avalanche(w.left, w.right, w.gapX, w.gapW, fall * 0.6));
            music.playSFX("avalanche");
            shake = Math.max(shake, 12);
            return;
        }
        double speed = switch (w.kind) {
            case PEBBLE -> fall * 1.45;
            case BOULDER -> fall * 0.8;
            default -> fall;
        };
        double vx = w.kind == Rock.Kind.BOULDER ? (rand.nextBoolean() ? 1 : -1) * (2.5 + rand.nextDouble() * 1.5) : 0;
        rocks.add(new Rock(w.kind, w.x, -w.h, w.w, w.h, speed, vx));
        if (w.kind == Rock.Kind.BOULDER || tick - lastRumble > 25) {
            music.playSFX("rumble");
            lastRumble = tick;
        }
    }

    // ------------------------------------------------------------- collisions

    private void checkCollisions() {
        Rectangle pb = player.getBounds();
        boolean vulnerable = invuln == 0 && dashTime == 0;
        for (Iterator<Rock> it = rocks.iterator(); it.hasNext() && vulnerable;) {
            Rock r = it.next();
            if (pb.intersects(r.getBounds())) {
                it.remove();
                burst(r.x + r.width / 2.0, r.y + r.height / 2.0,
                        new int[]{0xb4c3cf, 0x7f91a1, 0x52626f, 0xeef4f8}, 16, 6);
                hit();
                vulnerable = false;
            }
        }
        for (Avalanche a : avalanches) {
            if (vulnerable && a.hits(pb)) {
                burst(player.centerX(), player.centerY(), new int[]{0xffffff, 0xe6eef7}, 20, 6);
                hit();
                vulnerable = false;
            }
        }
        if (gameOver) return;
        for (Iterator<Pickup> it = pickups.iterator(); it.hasNext();) {
            Pickup p = it.next();
            if (pb.intersects(p.getBounds())) {
                it.remove();
                collect(p);
            }
        }
    }

    private void hit() {
        shake = 16;
        burst(player.centerX(), player.y + Player.HEIGHT, new int[]{0x8a6a4a, 0xa8744a, 0xd3b48c}, 18, 5);
        if (helmet) {
            helmet = false;
            invuln = 60;
            music.playSFX("shield");
            burst(player.centerX(), player.y, new int[]{0xffc93c, 0xfff3b0}, 14, 5);
            particles.add(Particle.text(player.centerX(), player.y - 10, "HELMET SAVED YOU!", 0xffd23f));
            return;
        }
        lives--;
        music.playSFX("hit");
        if (lives <= 0) {
            endGame();
            return;
        }
        invuln = 130;
    }

    private void collect(Pickup p) {
        music.playSFX("pickup");
        int cx = (int) p.x + 18;
        burst(cx, p.y + 18, new int[]{0xffffff, 0xffd23f, 0xfff6c4}, 14, 4);
        switch (p.kind) {
            case EDELWEISS -> {
                bonus += 250;
                particles.add(Particle.text(cx, p.y, "+250", 0xffd23f));
            }
            case HELMET -> {
                helmet = true;
                particles.add(Particle.text(cx, p.y, "HELMET!", 0xffc93c));
            }
            case SLOWMO -> {
                slowmo = SLOWMO_FRAMES;
                particles.add(Particle.text(cx, p.y, "SLOW-MO!", 0x7fd3ff));
            }
        }
    }

    private void burst(double x, double y, int[] colors, int n, double speed) {
        for (int i = 0; i < n; i++) {
            double a = rand.nextDouble() * Math.PI * 2;
            double v = speed * (0.4 + rand.nextDouble());
            particles.add(new Particle(x, y, Math.cos(a) * v, Math.sin(a) * v - 2, 30 + rand.nextInt(20),
                    colors[rand.nextInt(colors.length)], 4 + 4 * rand.nextInt(2), true));
        }
    }

    private void endGame() {
        gameOver = true;
        movingLeft = movingRight = movingUp = movingDown = false;
        int score = score();
        int alt = altitude();
        int oldBestAlt = Save.bestAltitude();
        newBest = score > Save.bestScore();
        Save.recordRun(score, alt);
        for (Skin s : Skin.values()) {
            if (s.unlockAltitude > oldBestAlt && s.unlockAltitude <= alt) unlockedSkin = s;
        }
        music.playSFX("bleat");
        if (unlockedSkin != null) music.playSFX("unlock");
        layoutButtons();
    }

    // ----------------------------------------------------------------- render

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        MountainArt.drawBackground(g2d, getWidth(), getHeight(), (int) scroll);

        int sx = 0, sy = 0;
        if (shake > 0) {
            int m = Math.max(1, shake / 4);
            sx = (rand.nextInt(2 * m + 1) - m) * MountainArt.P;
            sy = (rand.nextInt(2 * m + 1) - m) * MountainArt.P;
        }
        g2d.translate(sx, sy);
        for (IcePatch ice : icePatches) ice.draw(g2d);
        for (Pickup p : pickups) p.draw(g2d, tick);
        for (Rock rock : rocks) rock.draw(g2d);
        for (Avalanche a : avalanches) a.draw(g2d, tick);
        if (player != null && !gameOver) drawPlayer(g2d);
        for (Particle p : particles) p.draw(g2d);
        g2d.translate(-sx, -sy);

        for (Warning w : warnings) w.draw(g2d);

        if (slowmo > 0) {
            g2d.setColor(new Color(90, 170, 255, 34));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        drawHud(g2d);
        if (bannerTime > 0 && !gameOver) drawBanner(g2d);
        if (briefing) drawBriefing(g2d);
        else if (countdown > 0 || goTime > 0) drawCountdown(g2d);

        if (paused) {
            drawOverlay(g2d, "PAUSED", null, null, null);
        } else if (gameOver) {
            drawOverlay(g2d, "GAME OVER",
                    "You reached " + altitude() + " m  -  Score " + score(),
                    newBest ? "NEW BEST!" : null,
                    unlockedSkin != null ? "NEW SKIN UNLOCKED: " + unlockedSkin.label : null);
        }
        g2d.dispose();
    }

    private void drawPlayer(Graphics2D g2d) {
        boolean flicker = invuln > 0 && dashTime == 0 && (invuln / 4) % 2 == 0;
        if (!flicker) player.draw(g2d);
        if (helmet) {
            // A golden shield ring around the goat.
            int s = MountainArt.P;
            int cx = MountainArt.snap(player.centerX());
            int cy = MountainArt.snap(player.centerY());
            int r = Player.WIDTH / 2 + 3 * s;
            float pulse = 0.45f + 0.2f * (float) Math.sin(tick / 5.0);
            Composite old = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
            g2d.setColor(new Color(0xffd23f));
            for (int a = 0; a < 360; a += 6) {
                int px = MountainArt.snap(cx + (int) (Math.cos(Math.toRadians(a)) * r));
                int py = MountainArt.snap(cy + (int) (Math.sin(Math.toRadians(a)) * r * 0.75));
                g2d.fillRect(px, py, s, s);
            }
            g2d.setComposite(old);
        }
    }

    private void drawHud(Graphics2D g2d) {
        int s = MountainArt.P;
        MountainArt.drawPanel(g2d, 12, 10, 248, 128);
        g2d.setFont(Fonts.get(26f));
        MountainArt.drawShadowText(g2d, "SCORE  " + score(), 28, 40, Color.WHITE);
        MountainArt.drawShadowText(g2d, "ALTITUDE  " + altitude() + " m", 28, 66, new Color(255, 214, 102));

        for (int i = 0; i < MAX_LIVES; i++) {
            MountainArt.drawSprite(g2d, i < lives ? HEART : HEART_EMPTY, 28 + i * 30, 78, 3);
        }
        if (helmet) MountainArt.drawSprite(g2d, Pickup.HELMET_IMG, 124, 78, 3);
        if (slowmo > 0) MountainArt.drawSprite(g2d, Pickup.SLOWMO_IMG, 164, 74, 3);

        // Dash cooldown bar.
        g2d.setFont(Fonts.get(20f));
        MountainArt.drawShadowText(g2d, "DASH", 28, 124, Color.WHITE);
        int bx = 76, by = 110, bw = 160, bh = 4 * s;
        g2d.setColor(new Color(0x0d1826));
        g2d.fillRect(bx, by, bw, bh);
        double ready = 1 - dashCooldown / (double) DASH_COOLDOWN;
        g2d.setColor(ready >= 1 ? new Color(0x6cc24a) : new Color(0x4a8fd6));
        g2d.fillRect(bx + s / 2, by + s / 2, (int) ((bw - s) * ready), bh - s);
        if (ready >= 1) {
            g2d.setColor(new Color(0xa4e87a));
            g2d.fillRect(bx + s / 2, by + s / 2, bw - s, s / 2);
        }

        g2d.setFont(Fonts.get(20f));
        String pauseText = "ESC  PAUSE";
        int pauseWidth = g2d.getFontMetrics().stringWidth(pauseText);
        MountainArt.drawPanel(g2d, getWidth() - pauseWidth - 40, 10, pauseWidth + 28, 36);
        MountainArt.drawShadowText(g2d, pauseText, getWidth() - pauseWidth - 26, 35, Color.WHITE);

        if (slowmo > 0) {
            g2d.setFont(Fonts.get(30f));
            String t = "SLOW-MO  " + (slowmo / 60 + 1);
            int tw = g2d.getFontMetrics().stringWidth(t);
            MountainArt.drawShadowText(g2d, t, (getWidth() - tw) / 2, getHeight() - 30, new Color(0x7fd3ff));
        }
    }

    private void drawBanner(Graphics2D g2d) {
        g2d.setFont(Fonts.get(56f));
        int tw = g2d.getFontMetrics().stringWidth(bannerTitle);
        int w = Math.max(360, tw + 80);
        int x = (getWidth() - w) / 2;
        int y = 120;
        MountainArt.drawPanel(g2d, x, y, w, 110);
        MountainArt.drawShadowText(g2d, bannerTitle, (getWidth() - tw) / 2, y + 60, Color.WHITE);
        g2d.setFont(Fonts.get(28f));
        int sw = g2d.getFontMetrics().stringWidth(bannerSub);
        MountainArt.drawShadowText(g2d, bannerSub, (getWidth() - sw) / 2, y + 94, new Color(255, 214, 102));
    }

    private static final int BRIEF_W = 680, BRIEF_H = 430;

    private int briefingTop() {
        return Math.max(10, (getHeight() - BRIEF_H - 100) / 2);
    }

    /** The how-to-play card shown after PLAY, before the countdown. */
    private void drawBriefing(Graphics2D g) {
        g.setColor(new Color(10, 22, 38, 150));
        g.fillRect(0, 0, getWidth(), getHeight());
        int px = (getWidth() - BRIEF_W) / 2;
        int py = briefingTop();
        int cx = getWidth() / 2;
        Color gold = new Color(255, 214, 102), soft = new Color(225, 235, 245);
        MountainArt.drawPanel(g, px, py, BRIEF_W, BRIEF_H);

        g.setFont(Fonts.get(56f));
        centerText(g, "HOW TO PLAY", cx, py + 62, Color.WHITE);

        // Controls.
        String[][] keys = {{"ARROWS / WASD", "move"}, {"SPACE", "dash"}, {"ESC", "pause"}};
        for (int i = 0; i < keys.length; i++) {
            int kx = px + BRIEF_W * (2 * i + 1) / 6;
            g.setFont(Fonts.get(28f));
            centerText(g, keys[i][0], kx, py + 108, gold);
            g.setFont(Fonts.get(22f));
            centerText(g, keys[i][1], kx, py + 132, soft);
        }

        g.setColor(new Color(239, 240, 220, 70));
        g.fillRect(px + 30, py + 150, BRIEF_W - 60, 4);

        // Hazards to avoid.
        if (briefRock == null) {
            briefRock = new Rock(Rock.Kind.ROCK, 0, 0, 40, 36, 0, 0);
            briefBoulder = new Rock(Rock.Kind.BOULDER, 0, 0, 52, 44, 0, 0);
            briefIce = new IcePatch(0, 0, 60, 36, new Random(4));
        }
        g.setFont(Fonts.get(30f));
        MountainArt.drawShadowText(g, "AVOID", px + 30, py + 202, new Color(0xff8a80));
        int[] ax = {px + 200, px + 320, px + 440, px + 570};
        int iy = py + 178;
        placeRock(g, briefRock, ax[0], iy);
        placeRock(g, briefBoulder, ax[1], iy);
        HelpPanel.drawAvalancheIcon(g, ax[2] - 30, iy + 2);
        briefIce.x = ax[3] - 30;
        briefIce.y = iy + 4;
        briefIce.draw(g);
        String[] avoid = {"ROCKS", "BOULDERS", "AVALANCHES", "ICE"};
        g.setFont(Fonts.get(20f));
        for (int i = 0; i < 4; i++) centerText(g, avoid[i], ax[i], py + 246, soft);

        // Pickups to grab.
        g.setFont(Fonts.get(30f));
        MountainArt.drawShadowText(g, "GRAB", px + 30, py + 302, new Color(0xa4e87a));
        int[] gx = {px + 230, px + 390, px + 550};
        BufferedImage[] imgs = {Pickup.EDELWEISS_IMG, Pickup.HELMET_IMG, Pickup.SLOWMO_IMG};
        String[] grab = {"+250 POINTS", "BLOCKS 1 HIT", "SLOW MOTION"};
        for (int i = 0; i < 3; i++) {
            BufferedImage img = imgs[i];
            MountainArt.drawSprite(g, img, gx[i] - img.getWidth() * 2, py + 270 + (36 - img.getHeight() * 4) / 2, 4);
            g.setFont(Fonts.get(20f));
            centerText(g, grab[i], gx[i], py + 334, soft);
        }

        g.setColor(new Color(239, 240, 220, 70));
        g.fillRect(px + 30, py + 352, BRIEF_W - 60, 4);
        g.setFont(Fonts.get(26f));
        centerText(g, "Watch the flashing signs at the top - rocks drop there.", cx, py + 386, Color.WHITE);
        g.setFont(Fonts.get(22f));
        centerText(g, "3 hearts. Climb as high as you can!     ENTER = START", cx, py + 414, gold);
    }

    private static void placeRock(Graphics2D g, Rock r, int cx, int y) {
        r.x = cx - r.width / 2.0;
        r.y = y + (44 - r.height) / 2.0;
        r.draw(g);
    }

    private static void centerText(Graphics2D g, String text, int cx, int y, Color c) {
        MountainArt.drawShadowText(g, text, cx - g.getFontMetrics().stringWidth(text) / 2, y, c);
    }

    private void drawCountdown(Graphics2D g2d) {
        String big = countdown > 0 ? String.valueOf((countdown + COUNTDOWN_STEP - 1) / COUNTDOWN_STEP) : "CLIMB!";
        float size = countdown > 0 ? 180f - (COUNTDOWN_STEP - 1 - (countdown - 1) % COUNTDOWN_STEP) * 0.8f : 130f;
        g2d.setFont(Fonts.get(size));
        int bw = g2d.getFontMetrics().stringWidth(big);
        int cy = getHeight() / 2 - 20;
        MountainArt.drawShadowText(g2d, big, (getWidth() - bw) / 2, cy,
                countdown > 0 ? Color.WHITE : new Color(0xa4e87a));
        if (countdown > 0) {
            g2d.setFont(Fonts.get(28f));
            String hint = "ARROWS / WASD TO MOVE    SPACE TO DASH";
            int hw = g2d.getFontMetrics().stringWidth(hint);
            MountainArt.drawPanel(g2d, (getWidth() - hw) / 2 - 20, cy + 36, hw + 40, 44);
            MountainArt.drawShadowText(g2d, hint, (getWidth() - hw) / 2, cy + 67, Color.WHITE);
        }
    }

    private void drawOverlay(Graphics2D g2d, String title, String line, String badge, String badge2) {
        g2d.setColor(new Color(10, 22, 38, 160));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        int panelW = 520;
        int panelH = 110 + (line != null ? 46 : 0) + (badge != null ? 38 : 0) + (badge2 != null ? 38 : 0);
        int panelX = (getWidth() - panelW) / 2;
        int panelY = getHeight() / 2 - 16 - panelH;
        MountainArt.drawPanel(g2d, panelX, panelY, panelW, panelH);

        g2d.setFont(Fonts.get(64f));
        int tw = g2d.getFontMetrics().stringWidth(title);
        MountainArt.drawShadowText(g2d, title, (getWidth() - tw) / 2, panelY + 72, Color.WHITE);
        int y = panelY + 72;
        if (line != null) {
            y += 42;
            g2d.setFont(Fonts.get(28f));
            int lw = g2d.getFontMetrics().stringWidth(line);
            MountainArt.drawShadowText(g2d, line, (getWidth() - lw) / 2, y, new Color(225, 235, 245));
        }
        g2d.setFont(Fonts.get(30f));
        for (String b : new String[]{badge, badge2}) {
            if (b == null) continue;
            y += 38;
            int bw = g2d.getFontMetrics().stringWidth(b);
            MountainArt.drawShadowText(g2d, b, (getWidth() - bw) / 2, y, new Color(255, 214, 102));
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public void keyPressed(KeyEvent e) {
        if (briefing) {
            int k = e.getKeyCode();
            if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
                music.playSFX("click.wav");
                startCountdown();
            }
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> movingLeft = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> movingRight = true;
            case KeyEvent.VK_UP, KeyEvent.VK_W -> movingUp = true;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> movingDown = true;
            case KeyEvent.VK_SPACE -> tryDash();
            case KeyEvent.VK_ESCAPE, KeyEvent.VK_P -> {
                if (!gameOver) {
                    music.playSFX("click.wav");
                    setPaused(!paused);
                }
            }
            case KeyEvent.VK_ENTER -> {
                if (gameOver) restart();
            }
            default -> { }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> movingLeft = false;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> movingRight = false;
            case KeyEvent.VK_UP, KeyEvent.VK_W -> movingUp = false;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> movingDown = false;
            default -> { }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
