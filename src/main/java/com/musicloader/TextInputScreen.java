package com.musicloader;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Окно "Добавить текст": введённый текст показывается снизу в меню паузы. */
public class TextInputScreen extends Screen {
    private final Screen parent;
    private EditBox box;

    public TextInputScreen(Screen parent) {
        super(Component.literal("Добавить текст"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        box = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 10, 200, 20,
                Component.literal("Текст"));
        box.setMaxLength(100);
        box.setValue(MusicLibrary.loadText());
        this.addRenderableWidget(box);
        this.setInitialFocus(box);

        this.addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> {
            MusicLibrary.saveText(box.getValue().strip());
            this.onClose();
        }).bounds(this.width / 2 - 100, this.height / 2 + 20, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> this.onClose())
                .bounds(this.width / 2 + 2, this.height / 2 + 20, 98, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, "Этот текст будет виден снизу в меню паузы",
                this.width / 2 - 100, this.height / 2 - 26, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
