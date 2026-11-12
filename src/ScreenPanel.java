import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.*;

/**
 * Base for full-screen menus (settings, how to play). Content is laid out on a
 * fixed virtual canvas that is centred and scaled down to fit small windows.
 */
public abstract class ScreenPanel extends JPanel {
    protected final GameFrame frame;
    protected final MusicManager music;
    private final int vw, vh;

    protected ScreenPanel(GameFrame frame, MusicManager music, int virtualW, int virtualH) {
        this.frame = frame;
        this.music = music;
        this.vw = virtualW;
        this.vh = virtualH;
        setLayout(null);
        setFocusable(true);
        MountainArt.setBiome(0);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "back");
        getActionMap().put("back", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                back();
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutButtons();
                repaint();
            }
        });
        SwingUtilities.invokeLater(this::layoutButtons);
    }

    protected double scale() {
        return Math.min(1.0, Math.min(getWidth() / (double) vw, getHeight() / (double) vh));
    }

    private int ox() {
        return (int) ((getWidth() - vw * scale()) / 2);
    }

    private int oy() {
        return (int) ((getHeight() - vh * scale()) / 2);
    }

    /** Positions a button at virtual coordinates. */
    protected void place(JComponent c, int vx, int vy) {
        c.setLocation(ox() + (int) (vx * scale()), oy() + (int) (vy * scale()));
    }

    protected PixelButton addButton(PixelButton b, int w, int h) {
        b.setSize(w, h);
        add(b);
        return b;
    }

    protected void back() {
        music.playSFX("click.wav");
        frame.showPanel(new MenuPanel(frame, music));
    }

    protected abstract void layoutButtons();

    /** Paints the screen in virtual coordinates. */
    protected abstract void paintContent(Graphics2D g);

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        MountainArt.drawBackground(g2, getWidth(), getHeight(), 0);
        g2.setColor(new Color(10, 22, 38, 120));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.translate(ox(), oy());
        g2.scale(scale(), scale());
        paintContent(g2);
        g2.dispose();
    }

    protected static void center(Graphics2D g, String text, int cx, int y, Color c) {
        int w = g.getFontMetrics().stringWidth(text);
        MountainArt.drawShadowText(g, text, cx - w / 2, y, c);
    }

    protected static void text(Graphics2D g, String text, int x, int y, Color c) {
        MountainArt.drawShadowText(g, text, x, y, c);
    }

    protected static final Color GOLD = new Color(255, 214, 102);
    protected static final Color SOFT = new Color(225, 235, 245);
    protected static final Color MUTED = new Color(170, 195, 220);
}
