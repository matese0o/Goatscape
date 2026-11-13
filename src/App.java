import javax.swing.SwingUtilities;

public class App {

    public static void main(String[] args) {
        // Show "Goatscape" instead of "App" in the macOS menu bar.
        System.setProperty("apple.awt.application.name", "Goatscape");

        SwingUtilities.invokeLater(() -> {
            MusicManager music = new MusicManager();
            new GameFrame("Goatscape", music);
        });
    }
}
