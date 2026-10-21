package com.jarvis.addon.audio;

import com.jarvis.addon.JarvisAddon;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Plays WAV files bundled inside the jar. Playback happens off the game thread. */
public final class SoundPlayer {
    private static final byte[] MISSING = new byte[0];
    private static final Map<JarvisSound, byte[]> CACHE = new ConcurrentHashMap<>();

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "jarvis-audio");
        t.setDaemon(true);
        return t;
    });

    private SoundPlayer() {}

    /** @param volume 0.0 - 1.0 */
    public static void play(JarvisSound sound, float volume) {
        if (volume <= 0f) return;
        POOL.execute(() -> {
            byte[] data = CACHE.computeIfAbsent(sound, SoundPlayer::load);
            if (data == MISSING) return;

            try (AudioInputStream raw = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
                 AudioInputStream in = toPcm16(raw)) {

                Clip clip = AudioSystem.getClip();
                clip.open(in);

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float db = (float) (20.0 * Math.log10(Math.max(volume, 0.0001f)));
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
                }

                clip.addLineListener(e -> {
                    if (e.getType() == LineEvent.Type.STOP) clip.close();
                });
                clip.start();
            } catch (Exception e) {
                JarvisAddon.LOG.error("Failed to play {}", sound.path(), e);
            }
        });
    }

    private static byte[] load(JarvisSound sound) {
        try (InputStream is = SoundPlayer.class.getResourceAsStream(sound.path())) {
            if (is == null) {
                JarvisAddon.LOG.warn("Sound file not found in jar: {}", sound.path());
                return MISSING;
            }
            return is.readAllBytes();
        } catch (Exception e) {
            JarvisAddon.LOG.error("Failed to read {}", sound.path(), e);
            return MISSING;
        }
    }

    /** Converts odd formats (24-bit, float, ...) to 16-bit PCM so Clip can open them. */
    private static AudioInputStream toPcm16(AudioInputStream in) {
        AudioFormat f = in.getFormat();
        if (f.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && f.getSampleSizeInBits() == 16) return in;
        AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.getSampleRate(), 16,
            f.getChannels(), f.getChannels() * 2, f.getSampleRate(), false);
        return AudioSystem.getAudioInputStream(target, in);
    }
}
