package com.musicloader;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.util.List;

/** Простой плеер на javax.sound (WAV). Музыка продолжает играть и после закрытия меню паузы. */
public final class MusicPlayer {
    private static Clip clip;
    private static int index = -1;
    private static boolean userPaused = false;

    private MusicPlayer() {}

    public static MusicLibrary.Song current() {
        List<MusicLibrary.Song> list = MusicLibrary.songs();
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    public static boolean isPlaying() {
        return clip != null && clip.isRunning();
    }

    public static void select(int newIndex) {
        List<MusicLibrary.Song> list = MusicLibrary.songs();
        if (list.isEmpty()) return;
        index = Math.floorMod(newIndex, list.size());
        play(list.get(index));
    }

    public static void next() { select(index + 1); }

    public static void previous() { select(index - 1); }

    private static void play(MusicLibrary.Song song) {
        stopClip();
        try (AudioInputStream in = AudioSystem.getAudioInputStream(song.file().toFile())) {
            clip = AudioSystem.getClip();
            clip.open(in);
            clip.addLineListener(e -> {
                // песня закончилась сама -> следующая
                if (e.getType() == LineEvent.Type.STOP && clip != null
                        && !userPaused && clip.getMicrosecondPosition() >= clip.getMicrosecondLength()) {
                    next();
                }
            });
            userPaused = false;
            clip.start();
        } catch (Exception e) {
            MusicLoaderClient.LOGGER.error("Не удалось воспроизвести " + song.file(), e);
            clip = null;
        }
    }

    public static void togglePause() {
        if (clip == null) {
            if (index < 0) select(0); else select(index);
            return;
        }
        if (clip.isRunning()) {
            userPaused = true;
            clip.stop();
        } else {
            userPaused = false;
            clip.start();
        }
    }

    public static void seek(double fraction) {
        if (clip == null) return;
        fraction = Math.max(0, Math.min(0.999, fraction));
        clip.setMicrosecondPosition((long) (clip.getMicrosecondLength() * fraction));
    }

    public static double progress() {
        if (clip == null || clip.getMicrosecondLength() <= 0) return 0;
        return (double) clip.getMicrosecondPosition() / clip.getMicrosecondLength();
    }

    public static int positionSeconds() {
        return clip == null ? 0 : (int) (clip.getMicrosecondPosition() / 1_000_000L);
    }

    public static int lengthSeconds() {
        return clip == null ? 0 : (int) (clip.getMicrosecondLength() / 1_000_000L);
    }

    private static void stopClip() {
        if (clip != null) {
            Clip old = clip;
            clip = null;
            old.stop();
            old.close();
        }
    }

    public static void shutdown() {
        userPaused = true;
        stopClip();
    }
}
