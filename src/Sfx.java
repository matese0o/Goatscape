import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.sound.sampled.AudioFormat;

/** Retro sound effects synthesised in code, so no extra audio files are needed. */
public final class Sfx {
    static final float RATE = 22050f;
    static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);

    private static final Map<String, byte[]> CACHE = new HashMap<>();

    private Sfx() {}

    /** PCM data for a named effect, or null if the name is not a synthesised effect. */
    public static synchronized byte[] get(String name) {
        if (!CACHE.containsKey(name)) CACHE.put(name, make(name));
        return CACHE.get(name);
    }

    private static byte[] make(String name) {
        return switch (name) {
            case "rumble" -> rumble(0.45, 0.55);
            case "avalanche" -> rumble(1.3, 0.6);
            case "hoof" -> hoof();
            case "bleat" -> bleat();
            case "pickup" -> notes(new double[]{784, 1047, 1568}, 0.07, 0.22);
            case "unlock" -> notes(new double[]{523, 659, 784, 1047, 1319}, 0.09, 0.22);
            case "hit" -> hit();
            case "shield" -> shield();
            case "dash" -> dash();
            case "beep" -> notes(new double[]{660}, 0.13, 0.22);
            case "go" -> notes(new double[]{990, 1320}, 0.16, 0.24);
            case "bounce" -> thud(0.12, 140, 0.45);
            default -> null;
        };
    }

    private static double[] buffer(double seconds) {
        return new double[(int) (RATE * seconds)];
    }

    private static byte[] rumble(double seconds, double amp) {
        Random r = new Random(7);
        double[] s = buffer(seconds);
        double brown = 0, low = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            brown = (brown + (r.nextDouble() - 0.5) * 0.18) * 0.985;
            low += (r.nextDouble() - 0.5 - low) * 0.06;
            double env = Math.min(1, t / 0.03) * Math.exp(-t * 3.2 / seconds);
            s[i] = (brown * 2.2 + low * 1.4 + Math.sin(2 * Math.PI * 48 * t) * 0.25) * env * amp;
        }
        return pcm(s);
    }

    private static byte[] hoof() {
        Random r = new Random(3);
        double[] s = buffer(0.06);
        double prev = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            double n = r.nextDouble() - 0.5;
            double hp = n - prev;
            prev = n;
            s[i] = (hp * 0.5 * Math.exp(-t * 90) + Math.sin(2 * Math.PI * 1500 * t) * 0.25 * Math.exp(-t * 140)) * 0.6;
        }
        return pcm(s);
    }

    /** "Meh-eh-eh": a falling sawtooth with a fast tremolo, softened by a low-pass filter. */
    private static byte[] bleat() {
        double[] s = buffer(0.85);
        double phase = 0, lp = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            double f = 470 - 70 * t + Math.sin(2 * Math.PI * 5 * t) * 12;
            phase += f / RATE;
            double saw = 2 * (phase - Math.floor(phase)) - 1;
            double trem = 0.55 + 0.45 * Math.sin(2 * Math.PI * 10.5 * t);
            double env = Math.min(1, t / 0.04) * Math.min(1, (0.85 - t) / 0.25);
            lp += (saw - lp) * 0.32;
            s[i] = lp * trem * env * 0.55;
        }
        return pcm(s);
    }

    private static byte[] notes(double[] freqs, double each, double amp) {
        double[] s = buffer(freqs.length * each + 0.05);
        for (int n = 0; n < freqs.length; n++) {
            int start = (int) (n * each * RATE);
            int len = (int) ((each + 0.05) * RATE);
            for (int i = 0; i < len && start + i < s.length; i++) {
                double t = i / RATE;
                double sq = Math.sin(2 * Math.PI * freqs[n] * t) >= 0 ? 1 : -1;
                s[start + i] += sq * amp * Math.exp(-t * 18);
            }
        }
        return pcm(s);
    }

    private static byte[] hit() {
        Random r = new Random(11);
        double[] s = buffer(0.32);
        double phase = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            phase += (170 - 110 * t / 0.32) / RATE;
            double body = Math.sin(2 * Math.PI * phase) * Math.exp(-t * 9);
            double crack = (r.nextDouble() - 0.5) * Math.exp(-t * 45);
            s[i] = (body * 0.8 + crack * 0.9) * 0.8;
        }
        return pcm(s);
    }

    private static byte[] shield() {
        double[] s = buffer(0.45);
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            s[i] = (Math.sin(2 * Math.PI * 1320 * t) + Math.sin(2 * Math.PI * 1980 * t) * 0.6) * 0.22 * Math.exp(-t * 7);
        }
        return pcm(s);
    }

    private static byte[] dash() {
        Random r = new Random(5);
        double[] s = buffer(0.22);
        double lp = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            double k = 0.04 + 0.5 * (t / 0.22);
            lp += ((r.nextDouble() - 0.5) - lp) * k;
            s[i] = lp * Math.sin(Math.PI * t / 0.22) * 1.6;
        }
        return pcm(s);
    }

    private static byte[] thud(double seconds, double freq, double amp) {
        double[] s = buffer(seconds);
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            s[i] = Math.sin(2 * Math.PI * freq * t * (1 - t)) * amp * Math.exp(-t * 25);
        }
        return pcm(s);
    }

    private static byte[] pcm(double[] s) {
        byte[] out = new byte[s.length * 2];
        for (int i = 0; i < s.length; i++) {
            int v = (int) Math.round(Math.max(-1, Math.min(1, s[i])) * 32767);
            out[2 * i] = (byte) v;
            out[2 * i + 1] = (byte) (v >> 8);
        }
        return out;
    }
}
