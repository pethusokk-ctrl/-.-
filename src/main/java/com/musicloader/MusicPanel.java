package com.musicloader;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Строит музыкальную панель на экране паузы по чертежу пользователя. */
public final class MusicPanel {
    private static final Map<Path, Identifier> ICONS = new HashMap<>();
    private static final Map<Path, Boolean> ICON_FAILED = new HashMap<>();

    private MusicPanel() {}

    public static void build(Screen screen, Consumer<AbstractWidget> add) {
        int w = screen.width;
        int h = screen.height;

        int leftSpace = w / 2 - 102;                // место слева от ванильных кнопок
        int arrow = 16;                              // ширина кнопок-стрелок
        int radius = Math.max(20, Math.min(40, (leftSpace - 2 * (arrow + 4) - 6) / 2));
        int cx = leftSpace / 2;
        int cy = h / 2 - 10;

        // < и > по бокам кружка
        add.accept(Button.builder(Component.literal("<"), b -> MusicPlayer.previous())
                .bounds(cx - radius - arrow - 3, cy - 10, arrow, 20).build());
        add.accept(Button.builder(Component.literal(">"), b -> MusicPlayer.next())
                .bounds(cx + radius + 3, cy - 10, arrow, 20).build());

        // треугольник "остановить/продолжить" сверху
        add.accept(Button.builder(Component.literal(MusicPlayer.isPlaying() ? "||" : "▶"), b -> {
            MusicPlayer.togglePause();
            b.setMessage(Component.literal(MusicPlayer.isPlaying() ? "||" : "▶"));
        }).bounds(cx - 12, cy - radius - 26, 24, 20).build());

        // кнопка "Добавить текст" (необязательно)
        add.accept(Button.builder(Component.literal("Добавить текст"),
                        b -> ScreenUtil.open(new TextInputScreen(screen)))
                .bounds(6, 56, Math.min(110, leftSpace - 12), 20).build());

        // полоса прогресса снизу (клик = перемотка)
        int barX = 6;
        int barW = Math.max(60, leftSpace - 12);
        int barY = h - 22;
        add.accept(Button.builder(Component.empty(), b -> {
            Minecraft mc = Minecraft.getInstance();
            Window win = mc.getWindow();
            double mouseX = mc.mouseHandler.xpos() * win.getGuiScaledWidth() / (double) win.getScreenWidth();
            MusicPlayer.seek((mouseX - barX) / barW);
        }).bounds(barX, barY, barW, 8).build());

        // невидимый для кликов виджет, который рисует всё остальное
        add.accept(new PanelWidget(cx, cy, radius, barX, barY, barW, w, h));
    }

    private static Identifier iconFor(Path path) {
        if (path == null || ICON_FAILED.containsKey(path)) return null;
        Identifier cached = ICONS.get(path);
        if (cached != null) return cached;
        try (InputStream in = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(in);
            Identifier id = Identifier.fromNamespaceAndPath(MusicLoaderClient.MOD_ID,
                    "icon/" + Integer.toHexString(path.toString().hashCode()));
            Minecraft.getInstance().getTextureManager().register(id,
                    new DynamicTexture(() -> "musicloader icon", image));
            ICONS.put(path, id);
            return id;
        } catch (Exception e) {
            MusicLoaderClient.LOGGER.warn("Иконка не загрузилась: {}", path);
            ICON_FAILED.put(path, true);
            return null;
        }
    }

    private static String time(int sec) {
        return (sec / 60) + ":" + String.format("%02d", sec % 60);
    }

    private static final class PanelWidget extends AbstractWidget {
        private final int cx, cy, r, barX, barY, barW, sw, sh;

        PanelWidget(int cx, int cy, int r, int barX, int barY, int barW, int sw, int sh) {
            super(0, 0, 0, 0, Component.empty()); // нулевая область - клики не перехватывает
            this.cx = cx; this.cy = cy; this.r = r;
            this.barX = barX; this.barY = barY; this.barW = barW;
            this.sw = sw; this.sh = sh;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            var font = Minecraft.getInstance().font;

            // инструкция сверху слева
            g.text(font, "Инструкция", 6, 6, 0xFFFFFFFF, true);
            g.text(font, "Кинь .wav в папку musicloader", 6, 18, 0xFFAAAAAA, true);
            g.text(font, "или перетащи файл в окно.", 6, 28, 0xFFAAAAAA, true);
            g.text(font, "Иконка: .png с тем же именем", 6, 38, 0xFFAAAAAA, true);

            // кружок: белое кольцо + тёмная заливка
            for (int dy = -r; dy <= r; dy++) {
                int outer = (int) Math.sqrt((double) r * r - (double) dy * dy);
                g.fill(cx - outer, cy + dy, cx + outer, cy + dy + 1, 0xFFFFFFFF);
                int ri = r - 1;
                if (Math.abs(dy) <= ri) {
                    int inner = (int) Math.sqrt((double) ri * ri - (double) dy * dy);
                    g.fill(cx - inner, cy + dy, cx + inner, cy + dy + 1, 0xFF202020);
                }
            }

            var song = MusicPlayer.current();

            // иконка песни; если её нет - пустое место
            if (song != null && song.icon() != null) {
                Identifier icon = iconFor(song.icon());
                if (icon != null) {
                    int size = (int) (r * 1.4);
                    g.blit(RenderPipelines.GUI_TEXTURED, icon, cx - size / 2, cy - size / 2,
                            0f, 0f, size, size, size, size);
                }
            }

            // название, автор, таймкод
            String title = song == null ? "Нет песни" : song.title();
            String author = song == null ? "Выбери стрелками" : song.author();
            g.text(font, title, barX, barY - 30, 0xFFFFFFFF, true);
            g.text(font, author, barX, barY - 20, 0xFFAAAAAA, true);
            g.text(font, time(MusicPlayer.positionSeconds()) + " / " + time(MusicPlayer.lengthSeconds()),
                    barX, barY - 10, 0xFFAAAAAA, true);

            // зелёная заливка прогресса поверх кнопки-полосы
            int fill = (int) (barW * MusicPlayer.progress());
            g.fill(barX + 1, barY + 1, barX + barW - 1, barY + 7, 0xFF303030);
            g.fill(barX + 1, barY + 1, barX + Math.max(1, fill), barY + 7, 0xFF7FD400);

            // свой текст снизу по центру
            String text = MusicLibrary.loadText();
            if (!text.isEmpty()) {
                g.text(font, text, (sw - font.width(text)) / 2, sh - 14, 0xFFFFFFFF, true);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {}
    }
}
