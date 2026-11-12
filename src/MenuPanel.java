import java.awt.*;
import java.io.InputStream;
import javax.swing.*;

public class MenuPanel extends JPanel {

    private GameFrame frame;
    private MusicManager music; // Music manager reference
    private static int highestScore = 0;

    private JButton playBtn;
    private JButton exitBtn;

    private Font customFont;

    public MenuPanel(GameFrame frame, MusicManager music) {
        this.frame = frame;
        this.music = music;

        setLayout(null);

        loadCustomFont();

        // Start menu music
        this.music.stopMusic();
        this.music.playMusic("menu-music.wav", true);

        // Create Play button
        ImageIcon playIcon = new ImageIcon(getClass().getResource("/assets/play_button.png"));
        Image scaledPlayImg = playIcon.getImage().getScaledInstance(200, 90, Image.SCALE_SMOOTH);

        ImageIcon exitIcon = new ImageIcon(getClass().getResource("/assets/exit.png"));
        Image scaledExitImg = exitIcon.getImage().getScaledInstance(200, 90, Image.SCALE_SMOOTH);

        playBtn = new JButton(new ImageIcon(scaledPlayImg));
        exitBtn = new JButton(new ImageIcon(scaledExitImg));

        playBtn.setBorderPainted(false);
        playBtn.setContentAreaFilled(false);
        playBtn.setFocusPainted(false);
        playBtn.setOpaque(false);
        playBtn.setSize(200, 60);
        exitBtn.setBorderPainted(false);
        exitBtn.setContentAreaFilled(false);
        exitBtn.setFocusPainted(false);
        exitBtn.setOpaque(false);
        exitBtn.setSize(200, 60);

        add(playBtn);
        add(exitBtn);

        // Button actions
        playBtn.addActionListener(e -> {
            music.playSFX("click.wav");
            startGame();
        });

        exitBtn.addActionListener(e -> {
            music.playSFX("click.wav");
            System.exit(0);
        });

        // Recenter components when resized
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                centerComponents();
                repaint();
            }
        });

        SwingUtilities.invokeLater(this::centerComponents);
    }

    private void centerComponents() {
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        int spacing = 10;
        int startY = (panelHeight / 2) + 92;

        playBtn.setLocation((panelWidth - playBtn.getWidth()) / 2, startY);
        exitBtn.setLocation((panelWidth - exitBtn.getWidth()) / 2, startY + playBtn.getHeight() + spacing);
    }

    private void startGame() {
        music.stopMusic();
        music.playMusic("game-music.wav", true);
        music.setVolume(-10.0f); // lower volume

        frame.setContentPane(new GamePanel(frame, music));
        frame.revalidate();

        // ⭐ FIX: Give GamePanel keyboard focus again
        SwingUtilities.invokeLater(() -> {
            frame.getContentPane().setFocusable(true);
            frame.getContentPane().requestFocusInWindow();
        });
    }

    public static void updateHighscore(int score) {
        if (score > highestScore) highestScore = score;
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

    public static int getHighscore() {
        return highestScore;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        MountainArt.drawBackground(g2d, getWidth(), getHeight(), 0);

        // Title card
        int cardW = 460;
        int cardX = (getWidth() - cardW) / 2;
        MountainArt.drawPanel(g2d, cardX, 70, cardW, 180);

        g2d.setFont(customFont != null
                ? customFont.deriveFont(Font.PLAIN, 84f)
                : new Font("Arial", Font.BOLD, 72));
        String title = "PEAKFALL";
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        MountainArt.drawShadowText(g2d, title, (getWidth() - titleWidth) / 2, 150, Color.WHITE);

        g2d.setFont(customFont != null
                ? customFont.deriveFont(Font.PLAIN, 28f)
                : new Font("Arial", Font.PLAIN, 24));
        String subtitle = "Climb high. Dodge the rocks.";
        int subtitleWidth = g2d.getFontMetrics().stringWidth(subtitle);
        MountainArt.drawShadowText(g2d, subtitle, (getWidth() - subtitleWidth) / 2, 190,
                new Color(225, 235, 245));

        String scoreText = "BEST  " + highestScore;
        int scoreWidth = g2d.getFontMetrics().stringWidth(scoreText);
        MountainArt.drawShadowText(g2d, scoreText, (getWidth() - scoreWidth) / 2, 228,
                new Color(255, 214, 102));

        // Hero goat standing on a boulder ledge.
        int scale = 6;
        int goatW = MountainArt.goatWidth(scale);
        int goatH = MountainArt.goatHeight(scale);
        int goatX = MountainArt.snap((getWidth() - goatW) / 2);
        int ledgeY = MountainArt.snap(getHeight() / 2 + 44);
        drawLedge(g2d, goatX - 36, ledgeY, goatW + 72);
        MountainArt.drawGoat(g2d, goatX, ledgeY - goatH, false, scale);
        g2d.dispose();
    }

    private void loadCustomFont() {
        try (InputStream is = getClass().getResourceAsStream("/assets/font/Jersey10-Regular.ttf")) {
            if (is == null) {
                System.err.println("Font resource not found!");
                customFont = new Font("Arial", Font.PLAIN, 24);
                return;
            }
            customFont = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
            customFont = customFont.deriveFont(Font.PLAIN, 24f);  // set initial size
        } catch (Exception e) {
            e.printStackTrace();
            // fallback to Arial if loading fails
            customFont = new Font("Arial", Font.PLAIN, 24);
        }
    }
    
}
