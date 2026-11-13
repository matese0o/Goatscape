import java.util.prefs.Preferences;

/**
 * Saved progress and settings, kept between launches with Java Preferences
 * (a plist on macOS, the registry on Windows).
 */
public final class Save {
    private static final Preferences PREFS = Preferences.userRoot().node("goatscape");

    private Save() {}

    public static int bestScore() {
        return PREFS.getInt("bestScore", 0);
    }

    public static int bestAltitude() {
        return PREFS.getInt("bestAltitude", 0);
    }

    public static int runs() {
        return PREFS.getInt("runs", 0);
    }

    /** Records a finished run, keeping the best score and altitude. */
    public static void recordRun(int score, int altitude) {
        if (score > bestScore()) PREFS.putInt("bestScore", score);
        if (altitude > bestAltitude()) PREFS.putInt("bestAltitude", altitude);
        PREFS.putInt("runs", runs() + 1);
        flush();
    }

    /** Music volume, 0 (off) to 10. */
    public static int musicLevel() {
        return PREFS.getInt("musicLevel", 7);
    }

    public static void setMusicLevel(int level) {
        PREFS.putInt("musicLevel", Math.max(0, Math.min(10, level)));
        flush();
    }

    /** Sound-effect volume, 0 (off) to 10. */
    public static int sfxLevel() {
        return PREFS.getInt("sfxLevel", 8);
    }

    public static void setSfxLevel(int level) {
        PREFS.putInt("sfxLevel", Math.max(0, Math.min(10, level)));
        flush();
    }

    /** The chosen goat skin, falling back to CLASSIC if it is not unlocked. */
    public static Skin skin() {
        try {
            Skin s = Skin.valueOf(PREFS.get("skin", Skin.CLASSIC.name()));
            return s.unlocked() ? s : Skin.CLASSIC;
        } catch (IllegalArgumentException e) {
            return Skin.CLASSIC;
        }
    }

    public static void setSkin(Skin skin) {
        PREFS.put("skin", skin.name());
        flush();
    }

    /** Clears best scores, run count and unlocked skins; keeps volume settings. */
    public static void resetProgress() {
        PREFS.remove("bestScore");
        PREFS.remove("bestAltitude");
        PREFS.remove("runs");
        PREFS.remove("skin");
        flush();
    }

    private static void flush() {
        try {
            PREFS.flush();
        } catch (Exception e) {
            // Saving is best effort; the game still works without it.
        }
    }
}
