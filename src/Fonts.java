import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

/** The game's pixel font (Jersey 10), loaded once and shared. */
public final class Fonts {
    private static Font base;

    private Fonts() {}

    public static Font get(float size) {
        if (base == null) base = load();
        return base.deriveFont(Font.PLAIN, size);
    }

    private static Font load() {
        try (InputStream is = Fonts.class.getResourceAsStream("/assets/font/Jersey10-Regular.ttf")) {
            if (is != null) {
                Font f = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
                return f;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Font("Arial", Font.BOLD, 20);
    }
}
