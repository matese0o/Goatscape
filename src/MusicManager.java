import java.net.URL;
import javax.sound.sampled.*;

public class MusicManager {

    private Clip clip;

      // Default volume in decibels (0 = original volume)
    private float volumeDb = -10.0f; // Negative dB lowers the volume

    public void setVolume(float volumeDb) {
        this.volumeDb = volumeDb;

        if (clip != null) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(volumeDb);
        }
    }




    public void playMusic(String fileName, boolean loop) {
        try {
            // Use the filename parameter instead of hardcoding
            URL url = getClass().getResource("/assets/audio/" + fileName);

            if (url == null) {
                System.out.println("Music file not found: " + fileName);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
            clip = AudioSystem.getClip();
            clip.open(audioStream);

            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopMusic() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public void playSoundEffect(String fileName) {
    try {
        URL url = getClass().getResource("/assets/audio/" + fileName);

        if (url == null) {
            System.out.println("Sound effect not found: " + fileName);
            return;
        }

        AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
        Clip sfxClip = AudioSystem.getClip();
        sfxClip.open(audioStream);
        sfxClip.start();

    } catch (Exception e) {
        e.printStackTrace();
    }
}

public void playSFX(String fileName) {
    try {
        URL url = getClass().getResource("/assets/audio/" + fileName);
        if (url == null) {
            System.out.println("SFX file not found: " + fileName);
            return;
        }

        AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
        Clip sfxClip = AudioSystem.getClip();
        sfxClip.open(audioStream);
        sfxClip.start();

    } catch (Exception e) {
        e.printStackTrace();
    }
}



}
