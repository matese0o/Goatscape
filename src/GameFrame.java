import java.awt.Dimension;
import java.awt.Image;
import java.awt.Taskbar;
import java.net.URL;
import javax.swing.*;

public class GameFrame extends JFrame {

    private static final int START_SIZE = 700;

    private MusicManager music;

    public GameFrame(String title, MusicManager musicManager) {
        setTitle(title);
        this.music = musicManager;

        setAppIcon();

        // Resizable, with the green button / Ctrl+Cmd+F entering macOS full screen.
        setResizable(true);
        getRootPane().putClientProperty("apple.awt.fullscreenable", true);
        setMinimumSize(new Dimension(START_SIZE, START_SIZE));
        setSize(START_SIZE, START_SIZE);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // Open filling the screen; the game adapts to whatever size the window is.
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Start with menu panel
        setContentPane(new MenuPanel(this, music));

        setVisible(true);
    }

    /** Uses the Peakfall logo for the window, the Windows taskbar and the macOS Dock. */
    private void setAppIcon() {
        URL url = getClass().getResource("/assets/icon.png");
        if (url == null) return;
        Image icon = new ImageIcon(url).getImage();
        setIconImage(icon);
        if (Taskbar.isTaskbarSupported()
                && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().setIconImage(icon);
        }
    }

    public MusicManager getMusicManager() {
        return music;
    }
}
