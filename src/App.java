import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            // Create music manager
            MusicManager music = new MusicManager();
            
            // Create game window, pass music manager
            GameFrame frame = new GameFrame("Peakfall", music);
            
            frame.setSize(700, 700);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            // Set starting panel to MenuPanel and pass MusicManager
            frame.setContentPane(new MenuPanel(frame, music));
            frame.setVisible(true);
        });
    }
}
