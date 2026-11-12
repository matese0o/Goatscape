import java.awt.*;
import javax.swing.JOptionPane;

/** Music and sound volume, display tips and a progress reset. */
public class SettingsPanel extends ScreenPanel {
    private static final int VW = 640, VH = 620;
    private static final boolean MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

    private final PixelButton musicMinus, musicPlus, sfxMinus, sfxPlus, resetBtn, backBtn;

    public SettingsPanel(GameFrame frame, MusicManager music) {
        super(frame, music, VW, VH);
        musicMinus = addButton(new PixelButton("", PixelButton.Style.STONE, PixelButton.Icon.MINUS, 27f), 52, 52);
        musicPlus = addButton(new PixelButton("", PixelButton.Style.STONE, PixelButton.Icon.PLUS, 27f), 52, 52);
        sfxMinus = addButton(new PixelButton("", PixelButton.Style.STONE, PixelButton.Icon.MINUS, 27f), 52, 52);
        sfxPlus = addButton(new PixelButton("", PixelButton.Style.STONE, PixelButton.Icon.PLUS, 27f), 52, 52);
        resetBtn = addButton(new PixelButton("RESET PROGRESS", PixelButton.Style.RED, PixelButton.Icon.EXIT, 26f), 300, 56);
        backBtn = addButton(new PixelButton("BACK", PixelButton.Style.GREEN, PixelButton.Icon.BACK, 30f), 240, 60);

        musicMinus.addActionListener(e -> setMusic(Save.musicLevel() - 1));
        musicPlus.addActionListener(e -> setMusic(Save.musicLevel() + 1));
        sfxMinus.addActionListener(e -> setSfx(Save.sfxLevel() - 1));
        sfxPlus.addActionListener(e -> setSfx(Save.sfxLevel() + 1));
        resetBtn.addActionListener(e -> resetProgress());
        backBtn.addActionListener(e -> back());
    }

    private void setMusic(int level) {
        Save.setMusicLevel(level);
        music.applyMusicVolume();
        music.playSFX("click.wav");
        repaint();
    }

    private void setSfx(int level) {
        Save.setSfxLevel(level);
        music.playSFX("pickup"); // preview at the new volume
        repaint();
    }

    private void resetProgress() {
        music.playSFX("click.wav");
        int choice = JOptionPane.showConfirmDialog(this,
                "Reset best score, best altitude, runs and unlocked skins?\nVolume settings are kept.",
                "Reset progress", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            Save.resetProgress();
            repaint();
        }
    }

    @Override
    protected void layoutButtons() {
        place(musicMinus, 196, 150);
        place(musicPlus, 520, 150);
        place(sfxMinus, 196, 226);
        place(sfxPlus, 520, 226);
        place(resetBtn, (VW - 300) / 2, 420);
        place(backBtn, (VW - 240) / 2, 540);
    }

    @Override
    protected void paintContent(Graphics2D g) {
        MountainArt.drawPanel(g, 0, 0, VW, 520);
        g.setFont(Fonts.get(72f));
        center(g, "SETTINGS", VW / 2, 84, Color.WHITE);

        g.setFont(Fonts.get(32f));
        text(g, "MUSIC", 48, 186, Color.WHITE);
        text(g, "SOUND", 48, 262, Color.WHITE);
        drawBar(g, 260, 156, Save.musicLevel());
        drawBar(g, 260, 232, Save.sfxLevel());

        g.setFont(Fonts.get(28f));
        text(g, "DISPLAY", 48, 334, GOLD);
        g.setFont(Fonts.get(24f));
        text(g, MAC ? "Full screen: green window button or Ctrl + Cmd + F"
                : "Full screen: the window's maximize button", 48, 366, SOFT);
        text(g, "The cliff stretches to fit any window size.", 48, 394, SOFT);

        g.setFont(Fonts.get(20f));
        center(g, "Best " + Save.bestScore() + "   -   " + Save.bestAltitude() + " m   -   "
                + Save.runs() + " runs", VW / 2, 500, MUTED);
    }

    /** Ten chunky volume segments. */
    private static void drawBar(Graphics2D g, int x, int y, int level) {
        int segW = 20, gap = 5, h = 40;
        g.setColor(new Color(0x0d1826));
        g.fillRect(x - 6, y - 6, 10 * (segW + gap) - gap + 12, h + 12);
        for (int i = 0; i < 10; i++) {
            boolean on = i < level;
            int sx = x + i * (segW + gap);
            int sh = 16 + i * 2;
            g.setColor(on ? new Color(0x6cc24a) : new Color(0x2a3a4c));
            g.fillRect(sx, y + h - sh, segW, sh);
            if (on) {
                g.setColor(new Color(0xa4e87a));
                g.fillRect(sx, y + h - sh, segW, 4);
            }
        }
        g.setFont(Fonts.get(26f));
        text(g, level == 0 ? "OFF" : String.valueOf(level), x + 10 * (segW + gap) + 76, y + 30, GOLD);
    }
}
