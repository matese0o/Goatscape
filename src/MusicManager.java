import java.net.URL;
import javax.sound.sampled.*;

/** Background music and sound effects, with volumes from {@link Save}. */
public class MusicManager {

    /** Loudness of the music at full volume, in decibels. */
    private static final float MUSIC_BASE_DB = -8f;

    private Clip clip;
    private String current;

    /** Starts a track, unless that same track is already playing. */
    public void playMusic(String fileName, boolean loop) {
        if (fileName.equals(current) && clip != null && clip.isRunning()) return;
        stopMusic();
        try {
            URL url = getClass().getResource("/assets/audio/" + fileName);
            if (url == null) {
                System.out.println("Music file not found: " + fileName);
                return;
            }
            clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(url));
            current = fileName;
            applyMusicVolume();
            if (loop) clip.loop(Clip.LOOP_CONTINUOUSLY);
            else clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopMusic() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
        current = null;
    }

    /** Re-reads the music volume setting and applies it to the playing track. */
    public void applyMusicVolume() {
        setGain(clip, MUSIC_BASE_DB, Save.musicLevel());
    }

    /** Plays a synthesised effect (see {@link Sfx}) or a WAV file from assets/audio. */
    public void playSFX(String name) {
        int level = Save.sfxLevel();
        if (level <= 0) return;
        try {
            Clip sfx = AudioSystem.getClip();
            byte[] pcm = Sfx.get(name);
            if (pcm != null) {
                sfx.open(Sfx.FORMAT, pcm, 0, pcm.length);
            } else {
                URL url = getClass().getResource("/assets/audio/" + name);
                if (url == null) {
                    System.out.println("SFX not found: " + name);
                    sfx.close();
                    return;
                }
                sfx.open(AudioSystem.getAudioInputStream(url));
            }
            setGain(sfx, 0f, level);
            sfx.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) sfx.close();
            });
            sfx.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Sets a line's gain from a 0-10 volume level. */
    private static void setGain(Line line, float baseDb, int level) {
        if (line == null || !line.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        float db = level <= 0 ? gain.getMinimum()
                : baseDb + 20f * (float) Math.log10(level / 10.0);
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
    }
}
