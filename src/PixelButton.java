import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.JButton;

/**
 * Chunky 3D pixel-art button: notched corners, a dark outline, a lit top edge
 * and a thick base. It lifts on hover and sinks when pressed.
 */
public class PixelButton extends JButton {

    public enum Style {
        GREEN(0x6cc24a, 0xa4e87a, 0x449a30, 0x2b6420, 0xffffff),
        BLUE(0x4a8fd6, 0x86bdf2, 0x2f68a8, 0x1f4a7c, 0xffffff),
        RED(0xe0524f, 0xf89189, 0xa8322f, 0x7a2020, 0xffffff),
        STONE(0xe2d6b8, 0xfaf3df, 0xb3a483, 0x7a6b55, 0x3a2f24);

        final Color face, light, shade, depth, text;

        Style(int face, int light, int shade, int depth, int text) {
            this.face = new Color(face);
            this.light = new Color(light);
            this.shade = new Color(shade);
            this.depth = new Color(depth);
            this.text = new Color(text);
        }
    }

    /** 7x7 pixel icons, drawn in the text colour. */
    public enum Icon {
        PLAY(".#.....", ".##....", ".###...", ".####..", ".###...", ".##....", ".#....."),
        GEAR("..#.#..", ".#####.", "##...##", ".#...#.", "##...##", ".#####.", "..#.#.."),
        HELP(".####..", "##..##.", "....##.", "...##..", "..##...", ".......", "..##..."),
        EXIT("##...##", ".##.##.", "..###..", "..###..", ".##.##.", "##...##", "......."),
        SKIN("##...##", "###.###", ".#####.", ".#####.", ".#####.", ".#####.", "......."),
        LEFT("...##..", "..##...", ".##....", "##.....", ".##....", "..##...", "...##.."),
        RIGHT("..##...", "...##..", "....##.", ".....##", "....##.", "...##..", "..##..."),
        PLUS("..##...", "..##...", "######.", "######.", "..##...", "..##...", "......."),
        MINUS(".......", ".......", "######.", "######.", ".......", ".......", "......."),
        BACK("..#....", ".##....", "######.", ".##..#.", "..#..#.", ".....#.", "..###..");

        final String[] rows;

        Icon(String... rows) {
            this.rows = rows;
        }
    }

    private static final Color OUTLINE = new Color(0x1a2230);
    private static final int S = 4; // one art pixel

    private final Style style;
    private final Icon icon;
    private final float fontSize;

    public PixelButton(String text, Style style, Icon icon, float fontSize) {
        super(text);
        this.style = style;
        this.icon = icon;
        this.fontSize = fontSize;
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setFocusable(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth() - getWidth() % S;
        int h = getHeight() - getHeight() % S;
        boolean enabled = isEnabled();
        boolean hover = enabled && getModel().isRollover();
        boolean pressed = enabled && getModel().isPressed() && getModel().isArmed();

        int depth = 2 * S;
        int lift = pressed ? depth : (hover ? 0 : S / 2);
        int top = lift;
        int faceH = h - depth - S;

        // Base (the 3D side), always sitting at the bottom.
        notched(g2, 0, depth, w, h - depth, OUTLINE);
        notched(g2, S, depth + S, w - 2 * S, h - depth - 2 * S, style.depth);

        // Face.
        Color face = !enabled ? new Color(0x8a8f96) : hover ? style.light : style.face;
        notched(g2, 0, top, w, faceH + S, OUTLINE);
        g2.setColor(face);
        g2.fillRect(2 * S, top + S, w - 4 * S, faceH - S);
        g2.fillRect(S, top + 2 * S, w - 2 * S, faceH - 3 * S);
        // Lit top edge and shaded bottom edge.
        g2.setColor(enabled ? style.light : new Color(0xb4b9bf));
        g2.fillRect(2 * S, top + S, w - 4 * S, S);
        g2.fillRect(S, top + 2 * S, S, faceH - 4 * S);
        g2.setColor(enabled ? style.shade : new Color(0x6a6f76));
        g2.fillRect(2 * S, top + faceH - S, w - 4 * S, S);

        // Label (icon + text), centred on the face.
        g2.setFont(Fonts.get(fontSize));
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();
        int iconScale = Math.max(3, Math.round(fontSize / 9f));
        int iconW = icon == null ? 0 : 7 * iconScale;
        int gap = icon != null && !text.isEmpty() ? iconScale * 3 : 0;
        int textW = fm.stringWidth(text);
        int total = iconW + gap + textW;
        int x = (w - total) / 2;
        int cy = top + (faceH + S) / 2;
        Color textColor = enabled ? style.text : new Color(0xd8dce0);
        boolean shadow = style != Style.STONE;
        if (icon != null) {
            int iy = cy - 7 * iconScale / 2;
            if (shadow) drawIcon(g2, x + iconScale, iy + iconScale, iconScale, new Color(0, 0, 0, 90));
            drawIcon(g2, x, iy, iconScale, textColor);
        }
        int tx = x + iconW + gap;
        int ty = cy + fm.getAscent() / 2 - fm.getDescent() / 3;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        if (shadow) {
            g2.setColor(new Color(0, 0, 0, 90));
            g2.drawString(text, tx + 2, ty + 2);
        }
        g2.setColor(textColor);
        g2.drawString(text, tx, ty);
        g2.dispose();
    }

    private void drawIcon(Graphics2D g2, int x, int y, int scale, Color c) {
        g2.setColor(c);
        for (int r = 0; r < icon.rows.length; r++) {
            for (int col = 0; col < icon.rows[r].length(); col++) {
                if (icon.rows[r].charAt(col) == '#') g2.fillRect(x + col * scale, y + r * scale, scale, scale);
            }
        }
    }

    /** Rectangle with one-pixel notched corners. */
    private static void notched(Graphics2D g2, int x, int y, int w, int h, Color c) {
        g2.setColor(c);
        g2.fillRect(x + S, y, w - 2 * S, h);
        g2.fillRect(x, y + S, w, h - 2 * S);
    }

    /** Small helper for panels that want the same icon set as sprites. */
    public static BufferedImage iconImage(Icon icon, int argb) {
        BufferedImage img = new BufferedImage(7, 7, BufferedImage.TYPE_INT_ARGB);
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (icon.rows[r].charAt(c) == '#') img.setRGB(c, r, argb);
            }
        }
        return img;
    }
}
