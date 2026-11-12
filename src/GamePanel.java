import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class GamePanel extends JPanel implements ActionListener, KeyListener {

    private static final int BUTTON_W = 200;
    private static final int BUTTON_H = 90;
    /** Width of the climbable cliff in the original 700px window. */
    private static final int BASE_PLAY_W = 476;

    private GameFrame frame;
    private MusicManager music;
    private Timer timer;
    private Player player;
    private ArrayList<Rock> rocks;
    private Random rand;

    private boolean gameOver = false;
    private boolean newBest = false;
    private int score = 0;

    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean movingUp = false;
    private boolean movingDown = false;

    private boolean paused = false;
    private JButton resumeBtn;
    private JButton playAgainBtn;
    private JButton exitBtn;

    private int mountainScrollOffset = 0;

    private Font customFont;

    public GamePanel(GameFrame frame, MusicManager music) {
        this.frame = frame;
        this.music = music;

        setFocusable(true);
        requestFocusInWindow();
        setLayout(null);
        addKeyListener(this);

        rocks = new ArrayList<>();
        rand = new Random();

        timer = new Timer(16, this);
        timer.start();

        resumeBtn = imageButton("/assets/resume.png");
        playAgainBtn = imageButton("/assets/play_again.png");
        exitBtn = imageButton("/assets/exit.png");

        resumeBtn.addActionListener(e -> {
            music.playSFX("click.wav");
            resumeGame();
        });
        playAgainBtn.addActionListener(e -> {
            music.playSFX("click.wav");
            frame.setContentPane(new GamePanel(frame, music));
            frame.revalidate();
            SwingUtilities.invokeLater(() -> frame.getContentPane().requestFocusInWindow());
        });
        exitBtn.addActionListener(e -> {
            music.playSFX("click.wav");
            frame.setContentPane(new MenuPanel(frame, music));
            frame.revalidate();
        });

        add(resumeBtn);
        add(playAgainBtn);
        add(exitBtn);

        loadCustomFont();

        SwingUtilities.invokeLater(() -> requestFocusInWindow());

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent evt) {
                if (player == null) initPlayer();
                else keepPlayerOnCliff();
                // Keep pause / game-over buttons centred when the window changes size.
                if (paused) showButtons(resumeBtn, exitBtn);
                else if (gameOver) showButtons(playAgainBtn, exitBtn);
                repaint();
            }
        });
    }

    private JButton imageButton(String path) {
        ImageIcon icon = new ImageIcon(getClass().getResource(path));
        Image scaled = icon.getImage().getScaledInstance(BUTTON_W, BUTTON_H, Image.SCALE_SMOOTH);
        JButton btn = new JButton(new ImageIcon(scaled));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setFocusable(false);
        btn.setVisible(false);
        btn.setSize(BUTTON_W, BUTTON_H);
        return btn;
    }

    private void loadCustomFont() {
        try (InputStream is = getClass().getResourceAsStream("/assets/font/Jersey10-Regular.ttf")) {
            if (is == null) {
                System.err.println("Font resource not found!");
                customFont = new Font("Arial", Font.PLAIN, 20);
                return;
            }
            customFont = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
            customFont = customFont.deriveFont(Font.PLAIN, 20f);
        } catch (Exception e) {
            e.printStackTrace();
            customFont = new Font("Arial", Font.PLAIN, 20);
        }
    }

    private Font font(float size) {
        return customFont != null ? customFont.deriveFont(Font.PLAIN, size)
                : new Font("Arial", Font.BOLD, (int) size);
    }

    private void initPlayer() {
        int playerX = (getWidth() - Player.WIDTH) / 2;
        int playerY = getHeight() - Player.HEIGHT - 60;
        player = new Player(playerX, playerY);
    }

    /** Keeps the goat on the cliff face, including after the window is resized. */
    private void keepPlayerOnCliff() {
        int left = MountainArt.playLeft(getWidth());
        int right = MountainArt.playRight(getWidth());
        player.x = Math.max(left, Math.min(player.x, right - Player.WIDTH));
        player.y = Math.max(0, Math.min(player.y, getHeight() - Player.HEIGHT));
    }

    private void showButtons(JButton... buttons) {
        int centerX = (getWidth() - BUTTON_W) / 2;
        int y = getHeight() / 2 - 40;
        for (JButton btn : buttons) {
            btn.setLocation(centerX, y);
            btn.setVisible(true);
            y += BUTTON_H - 10;
        }
    }

    private void pauseGame() {
        paused = true;
        timer.stop();
        showButtons(resumeBtn, exitBtn);
        repaint();
    }

    private void resumeGame() {
        paused = false;
        timer.start();
        resumeBtn.setVisible(false);
        exitBtn.setVisible(false);
        repaint();

        SwingUtilities.invokeLater(() -> requestFocusInWindow());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !paused && player != null) updateGame();
        repaint();
    }

    private void updateGame() {
        score++;

        mountainScrollOffset += 5;

        if (movingLeft) player.x -= 8;
        if (movingRight) player.x += 8;
        if (movingUp) player.y -= 8;
        if (movingDown) player.y += 8;
        if (movingLeft != movingRight) player.facingLeft = movingLeft;
        player.moving = movingLeft || movingRight || movingUp || movingDown;
        player.update();

        keepPlayerOnCliff();
        int left = MountainArt.playLeft(getWidth());
        int right = MountainArt.playRight(getWidth());

        // One rock per ~40 frames on the original 700px window; wider cliffs get
        // proportionally more rocks so the danger per metre stays the same.
        double spawnChance = (right - left) / (double) BASE_PLAY_W / 40.0;
        if (rand.nextDouble() < spawnChance) {
            int rockWidth = 40 + rand.nextInt(36);
            int rockHeight = 36 + rand.nextInt(32);
            int newX = left + rand.nextInt(Math.max(1, right - left - rockWidth));
            boolean overlap = false;
            for (Rock rock : rocks) {
                if (rock.y < rockHeight + 40 && Math.abs(rock.x - newX) < rockWidth + 20) {
                    overlap = true;
                    break;
                }
            }
            if (!overlap) rocks.add(new Rock(newX, -rockHeight, rockWidth, rockHeight));
        }

        for (Rock rock : rocks) {
            rock.y += 6;
            if (player.getBounds().intersects(rock.getBounds())) {
                endGame();
                return;
            }
        }

        rocks.removeIf(rock -> rock.y > getHeight() + 100);
    }

    private void endGame() {
        gameOver = true;
        timer.stop();
        newBest = score > MenuPanel.getHighscore();
        MenuPanel.updateHighscore(score);
        movingLeft = movingRight = movingUp = movingDown = false;
        showButtons(playAgainBtn, exitBtn);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        MountainArt.drawBackground(g2d, getWidth(), getHeight(), mountainScrollOffset);

        if (player != null) player.draw(g2d);
        for (Rock rock : rocks) rock.draw(g2d);

        // HUD
        MountainArt.drawPanel(g2d, 12, 10, 196, 64);
        g2d.setFont(font(24f));
        MountainArt.drawShadowText(g2d, "SCORE  " + score, 26, 37, Color.WHITE);
        MountainArt.drawShadowText(g2d, "ALTITUDE  " + score / 10 + " m", 26, 62,
                new Color(255, 214, 102));

        g2d.setFont(font(20f));
        String pauseText = "ESC  PAUSE";
        int pauseWidth = g2d.getFontMetrics().stringWidth(pauseText);
        MountainArt.drawPanel(g2d, getWidth() - pauseWidth - 40, 10, pauseWidth + 28, 36);
        MountainArt.drawShadowText(g2d, pauseText, getWidth() - pauseWidth - 26, 35, Color.WHITE);

        if (paused) {
            drawOverlay(g2d, "PAUSED", null, null);
        } else if (gameOver) {
            drawOverlay(g2d, "GAME OVER",
                    "You reached " + score / 10 + " m   -   Score " + score,
                    newBest ? "NEW BEST!" : null);
        }
    }

    private void drawOverlay(Graphics2D g2d, String title, String line, String badge) {
        g2d.setColor(new Color(10, 22, 38, 160));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        int panelW = 420;
        int panelH = line != null ? 170 : 110;
        int panelX = (getWidth() - panelW) / 2;
        int panelY = getHeight() / 2 - 70 - panelH;
        MountainArt.drawPanel(g2d, panelX, panelY, panelW, panelH);

        g2d.setFont(font(64f));
        int tw = g2d.getFontMetrics().stringWidth(title);
        MountainArt.drawShadowText(g2d, title, (getWidth() - tw) / 2, panelY + 72, Color.WHITE);

        if (line != null) {
            g2d.setFont(font(26f));
            int lw = g2d.getFontMetrics().stringWidth(line);
            MountainArt.drawShadowText(g2d, line, (getWidth() - lw) / 2, panelY + 112,
                    new Color(225, 235, 245));
        }
        if (badge != null) {
            g2d.setFont(font(28f));
            int bw = g2d.getFontMetrics().stringWidth(badge);
            MountainArt.drawShadowText(g2d, badge, (getWidth() - bw) / 2, panelY + 148,
                    new Color(255, 214, 102));
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT -> movingLeft = true;
            case KeyEvent.VK_RIGHT -> movingRight = true;
            case KeyEvent.VK_UP -> movingUp = true;
            case KeyEvent.VK_DOWN -> movingDown = true;
            case KeyEvent.VK_ESCAPE -> {
                music.playSFX("click.wav");
                if (!paused) pauseGame();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT -> movingLeft = false;
            case KeyEvent.VK_RIGHT -> movingRight = false;
            case KeyEvent.VK_UP -> movingUp = false;
            case KeyEvent.VK_DOWN -> movingDown = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
