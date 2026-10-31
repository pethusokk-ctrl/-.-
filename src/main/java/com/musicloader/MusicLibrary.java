package com.musicloader;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Папка .minecraft/musicloader: песни, иконки (одноимённые .png) и сохранённый текст. */
public final class MusicLibrary {
    public static final Path DIR = FabricLoader.getInstance().getGameDir().resolve("musicloader");
    private static final Path TEXT_FILE = DIR.resolve("text.txt");

    public record Song(Path file, String title, String author, Path icon) {}

    private static final List<Song> songs = new ArrayList<>();

    private MusicLibrary() {}

    public static void init() {
        try {
            Files.createDirectories(DIR);
            Path readme = DIR.resolve("ИНСТРУКЦИЯ.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme, String.join("\n",
                        "Music Loader - инструкция",
                        "1. Кинь песню (.wav) в эту папку или перетащи файл мышкой в окно паузы (Esc).",
                        "2. Название файла: \"Автор - Название.wav\" (если нет тире, автор будет Неизвестен).",
                        "3. Иконка песни (необязательно): .png с тем же именем, например \"Автор - Название.png\".",
                        "   Если иконки нет, в кружке будет пустое место.",
                        "4. Esc -> кружок с музыкой. Стрелки влево/вправо - выбор песни, треугольник - пауза.",
                        "5. Кнопка \"Добавить текст\" - твой текст будет показан снизу меню паузы.",
                        "Поддерживаются WAV. mp3/ogg сначала сконвертируй в WAV.",
                        "").replace("\n", System.lineSeparator()), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            MusicLoaderClient.LOGGER.error("Не удалось создать папку musicloader", e);
        }
        reload();
    }

    public static synchronized void reload() {
        songs.clear();
        if (!Files.isDirectory(DIR)) return;
        try (Stream<Path> s = Files.list(DIR)) {
            s.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".wav"))
                    .sorted()
                    .forEach(p -> songs.add(toSong(p)));
        } catch (IOException e) {
            MusicLoaderClient.LOGGER.error("Не удалось прочитать папку musicloader", e);
        }
    }

    private static Song toSong(Path file) {
        String name = file.getFileName().toString();
        String base = name.substring(0, name.lastIndexOf('.'));
        String author = "Неизвестен";
        String title = base;
        int dash = base.indexOf(" - ");
        if (dash > 0) {
            author = base.substring(0, dash).trim();
            title = base.substring(dash + 3).trim();
        }
        Path icon = DIR.resolve(base + ".png");
        return new Song(file, title, author, Files.exists(icon) ? icon : null);
    }

    public static synchronized List<Song> songs() {
        return List.copyOf(songs);
    }

    /** Копирует перетащенные файлы в папку musicloader. */
    public static void importFiles(List<Path> files) {
        for (Path p : files) {
            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!(n.endsWith(".wav") || n.endsWith(".png"))) continue;
            try {
                Files.copy(p, DIR.resolve(p.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                MusicLoaderClient.LOGGER.error("Не удалось скопировать " + p, e);
            }
        }
        reload();
    }

    public static String loadText() {
        try {
            return Files.exists(TEXT_FILE) ? Files.readString(TEXT_FILE, StandardCharsets.UTF_8).strip() : "";
        } catch (IOException e) {
            return "";
        }
    }

    public static void saveText(String text) {
        try {
            Files.writeString(TEXT_FILE, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            MusicLoaderClient.LOGGER.error("Не удалось сохранить текст", e);
        }
    }
}
