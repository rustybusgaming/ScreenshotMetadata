package com.fentbuscoding.screenshotmetadata.compat;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import com.fentbuscoding.screenshotmetadata.config.ScreenshotMetadataConfig;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }

    private static class ConfigScreen extends Screen {
        private final Screen parent;
        private static final int BUTTON_WIDTH = 200;
        private static final int BUTTON_HEIGHT = 20;
        private static final int SPACING = 12;

        protected ConfigScreen(Screen parent) {
            super(Text.literal("Screenshot Metadata Config"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
            int centerX = this.width / 2;
            int startY = 70;
            int y = startY;

            // Metadata Output Format Section
            y += this.addToggleButton(centerX, y, "PNG Metadata", config.writePngMetadata, 
                button -> {
                    config.writePngMetadata = !config.writePngMetadata;
                    updateButtonText(button, config.writePngMetadata);
                });
            
            y += SPACING;
            y += this.addToggleButton(centerX, y, "XMP Sidecar", config.writeXmpSidecar, 
                button -> {
                    config.writeXmpSidecar = !config.writeXmpSidecar;
                    updateButtonText(button, config.writeXmpSidecar);
                });
            
            y += SPACING;
            y += this.addToggleButton(centerX, y, "JSON Sidecar", config.writeJsonSidecar, 
                button -> {
                    config.writeJsonSidecar = !config.writeJsonSidecar;
                    updateButtonText(button, config.writeJsonSidecar);
                });

            // Metadata Content Section
            y += 30;
            y += this.addToggleButton(centerX, y, "Include World Seed", config.includeWorldSeed, 
                button -> {
                    config.includeWorldSeed = !config.includeWorldSeed;
                    updateButtonText(button, config.includeWorldSeed);
                });

            // Done button
            y += 35;
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Done").formatted(Formatting.GREEN), button -> this.close())
                .dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        }

        private int addToggleButton(int centerX, int y, String label, boolean enabled, java.util.function.Consumer<ButtonWidget> onPress) {
            Text buttonText = createToggleText(label, enabled);
            ButtonWidget button = ButtonWidget.builder(buttonText, btn -> onPress.accept(btn))
                .dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
            this.addDrawableChild(button);
            return BUTTON_HEIGHT;
        }

        private static Text createToggleText(String label, boolean enabled) {
            String statusText = enabled ? "✓ ON" : "✗ OFF";
            Formatting color = enabled ? Formatting.GREEN : Formatting.RED;
            return Text.literal(label + ": ").append(Text.literal(statusText).formatted(color));
        }

        private static void updateButtonText(ButtonWidget button, boolean enabled) {
            String currentText = button.getMessage().getString();
            String label = currentText.split(":")[0].trim();
            button.setMessage(createToggleText(label, enabled));
        }

        @Override
        public void close() {
            ScreenshotMetadataConfig.save();
            this.client.setScreen(parent);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            
            // Header
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Metadata Output Formats").formatted(Formatting.GOLD), this.width / 2, 50, 0xFFD700);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Metadata Content").formatted(Formatting.GOLD), this.width / 2, 145, 0xFFD700);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }
}
