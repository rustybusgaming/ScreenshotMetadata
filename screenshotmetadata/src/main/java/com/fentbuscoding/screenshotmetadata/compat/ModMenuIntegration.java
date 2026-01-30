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
        return NoConfigScreen::new;
    }

    private static class NoConfigScreen extends Screen {
        private final Screen parent;

        protected NoConfigScreen(Screen parent) {
            super(Text.literal(ScreenshotMetadataMod.MOD_NAME));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int buttonWidth = 240;
            int buttonHeight = 20;
            int x = (this.width - buttonWidth) / 2;
            int y = this.height / 2 - 30;

            ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();

            this.addDrawableChild(ButtonWidget.builder(toggleLabel("png metadata", config.writePngMetadata), button -> {
                config.writePngMetadata = !config.writePngMetadata;
                button.setMessage(toggleLabel("png metadata", config.writePngMetadata));
            }).dimensions(x, y, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(toggleLabel("xmp sidecar", config.writeXmpSidecar), button -> {
                config.writeXmpSidecar = !config.writeXmpSidecar;
                button.setMessage(toggleLabel("xmp sidecar", config.writeXmpSidecar));
            }).dimensions(x, y + 24, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(toggleLabel("json sidecar", config.writeJsonSidecar), button -> {
                config.writeJsonSidecar = !config.writeJsonSidecar;
                button.setMessage(toggleLabel("json sidecar", config.writeJsonSidecar));
            }).dimensions(x, y + 48, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(toggleLabel("include world seed", config.includeWorldSeed), button -> {
                config.includeWorldSeed = !config.includeWorldSeed;
                button.setMessage(toggleLabel("include world seed", config.includeWorldSeed));
            }).dimensions(x, y + 76, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                .dimensions(x, y + 120, buttonWidth, buttonHeight)
                .build());
        }

        @Override
        public void close() {
            ScreenshotMetadataConfig.save();
            this.client.setScreen(parent);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("metadata outputs").formatted(Formatting.GRAY), this.width / 2, this.height / 2 - 60, 0xAAAAAA);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("turn off what you don’t want to save").formatted(Formatting.DARK_GRAY), this.width / 2, this.height / 2 - 48, 0x888888);
            super.render(context, mouseX, mouseY, delta);
        }

        private static Text toggleLabel(String label, boolean enabled) {
            return Text.literal(label + ": " + (enabled ? "on" : "off"));
        }
    }
}
