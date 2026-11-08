package com.duedate.service;

import javax.sound.sampled.*;
import java.awt.Toolkit;
import java.io.ByteArrayInputStream;

/**
 * Generates an elegant notification chime synthesized in real-time.
 */
public class SoundService {

    private static byte[] chimeAudioData = null;
    private static AudioFormat audioFormat = null;

    static {
        try {
            audioFormat = new AudioFormat(44100, 16, 1, true, false);
            chimeAudioData = generateChimeBytes(audioFormat);
        } catch (Exception e) {
            System.err.println("Could not pre-synthesize chime: " + e.getMessage());
        }
    }

    /**
     * Rings the notification bell/chime.
     */
    public static void ringBell() {
        new Thread(() -> {
            if (chimeAudioData != null && audioFormat != null) {
                try {
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioFormat, chimeAudioData, 0, chimeAudioData.length);
                    clip.start();
                    return;
                } catch (Exception ignored) {
                    // Fallback to system beep
                }
            }
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (Exception ignored) {}
        }, "DueDate-BellSound-Thread").start();
    }

    private static byte[] generateChimeBytes(AudioFormat format) {
        float sampleRate = format.getSampleRate();
        int totalSamples = (int) (sampleRate * 0.7); // 700ms duration
        byte[] buffer = new byte[totalSamples * 2];

        // Dual-tone harmonic chime: 587.33 Hz (D5) followed quickly by 880 Hz (A5)
        double f1 = 587.33;
        double f2 = 880.00;

        for (int i = 0; i < totalSamples; i++) {
            double time = i / (double) sampleRate;

            // Exponential decay envelope
            double envelope = Math.exp(-time * 5.0);

            // Phase blend
            double wave1 = Math.sin(2 * Math.PI * f1 * time);
            double wave2 = (time > 0.1) ? Math.sin(2 * Math.PI * f2 * (time - 0.1)) * 0.8 : 0;

            double sample = (wave1 + wave2) * 0.5 * envelope;
            short pcm = (short) (sample * 24000); // Scale to 16-bit PCM

            buffer[i * 2] = (byte) (pcm & 0xFF);
            buffer[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
        }

        return buffer;
    }
}
