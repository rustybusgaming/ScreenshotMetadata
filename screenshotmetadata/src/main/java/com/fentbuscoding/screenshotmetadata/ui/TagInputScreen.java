package com.fentbuscoding.screenshotmetadata.ui;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TagInputScreen extends Screen {
    private static final int FIELD_WIDTH = 260;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;
    private TextFieldWidget tagField;

    public TagInputScreen(Screen parent) {
        super(Text.literal("Screenshot Tags"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int fieldX = centerX - FIELD_WIDTH / 2;
        int fieldY = this.height / 2 - 20;

        tagField = new TextFieldWidget(this.textRenderer, fieldX, fieldY, FIELD_WIDTH, FIELD_HEIGHT, Text.literal("Tags"));
        tagField.setMaxLength(256);
        String pending = ScreenshotMetadataMod.getPendingTags();
        if (pending != null) {
            tagField.setText(pending);
        }
        this.addDrawableChild(tagField);
        this.setFocused(tagField);

        int buttonY = fieldY + 30;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save").formatted(Formatting.GREEN),
                button -> {
                    ScreenshotMetadataMod.setPendingTags(tagField.getText());
                    closeScreen();
                })
            .dimensions(centerX - BUTTON_WIDTH - 6, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Clear").formatted(Formatting.YELLOW),
                button -> {
                    ScreenshotMetadataMod.setPendingTags(null);
                    closeScreen();
                })
            .dimensions(centerX + 6, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
    }

    private void closeScreen() {
        MinecraftClient client = this.client;
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("Add comma-separated tags for the next screenshot").formatted(Formatting.GRAY),
            centerX, this.height / 2 - 50, 0xAAAAAA);

    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Avoid applyBlur to prevent "Can only blur once per frame" crashes.
        this.renderInGameBackground(context);
    }

    @Override
    public void close() {
        closeScreen();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
