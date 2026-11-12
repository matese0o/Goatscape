import javax.swing.*;

public class GameFrame extends JFrame {

    private MusicManager music;

    public GameFrame(String title, MusicManager musicManager) {
        setTitle(title);
        this.music = musicManager;

        int gameHeight = 700;
        setSize(gameHeight, gameHeight);

        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Start with menu panel
        setContentPane(new MenuPanel(this, music));

        setVisible(true);
    }

    public MusicManager getMusicManager() {
        return music;
    }
}
