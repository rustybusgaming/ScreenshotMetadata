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
        private static final int BUTTON_WIDTH = 220;
        private static final int BUTTON_HEIGHT = 24;
        private static final int SPACING = 8;
        private static final int SECTION_PADDING = 20;
        
        private int scrollOffset = 0;

        protected ConfigScreen(Screen parent) {
            super(Text.literal("Screenshot Metadata Config").formatted(Formatting.BOLD));
            this.parent = parent;
        }

        @Override
        protected void init() {
            this.clearChildren();
            ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
            int centerX = this.width / 2;
            int baseY = 70;
            int y = baseY - scrollOffset;

            // ===== OUTPUT FORMATS SECTION =====
            y = drawSection(centerX, y, "Output Formats", 0x88FF88);
            
            y += this.addToggleButton(centerX, y, "PNG Metadata", 
                "Embed metadata in PNG chunks", config.writePngMetadata, 
                button -> {
                    config.writePngMetadata = !config.writePngMetadata;
                    updateButtonText(button, config.writePngMetadata);
                });
            
            y += this.addToggleButton(centerX, y, "XMP Sidecar", 
                "Create .xmp companion files", config.writeXmpSidecar, 
                button -> {
                    config.writeXmpSidecar = !config.writeXmpSidecar;
                    updateButtonText(button, config.writeXmpSidecar);
                });

            y += this.addToggleButton(centerX, y, "JSON Sidecar", 
                "Create .json companion files", config.writeJsonSidecar, 
                button -> {
                    config.writeJsonSidecar = !config.writeJsonSidecar;
                    updateButtonText(button, config.writeJsonSidecar);
                });

            // ===== WORLD DATA SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "World Data", 0x88CCFF);
            
            y += this.addToggleButton(centerX, y, "World Seed", 
                "Include the world seed", config.includeWorldSeed, 
                button -> {
                    config.includeWorldSeed = !config.includeWorldSeed;
                    updateButtonText(button, config.includeWorldSeed);
                });

            y += this.addToggleButton(centerX, y, "Biome Info", 
                "Record biome name and ID", config.includeBiomeInfo, 
                button -> {
                    config.includeBiomeInfo = !config.includeBiomeInfo;
                    updateButtonText(button, config.includeBiomeInfo);
                });

            y += this.addToggleButton(centerX, y, "Coordinates", 
                "Log player position and angles", config.includeCoordinates, 
                button -> {
                    config.includeCoordinates = !config.includeCoordinates;
                    updateButtonText(button, config.includeCoordinates);
                });

            // ===== PLAYER STATUS SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "Player Status", 0xFF88CC);
            
            y += this.addToggleButton(centerX, y, "Health and Hunger", 
                "Track HP, food, and saturation", config.includePlayerStatus, 
                button -> {
                    config.includePlayerStatus = !config.includePlayerStatus;
                    updateButtonText(button, config.includePlayerStatus);
                });

            y += this.addToggleButton(centerX, y, "Potion Effects", 
                "Record active status effects", config.includePotionEffects, 
                button -> {
                    config.includePotionEffects = !config.includePotionEffects;
                    updateButtonText(button, config.includePotionEffects);
                });

            // ===== EQUIPMENT SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "Equipment", 0xFFCC88);
            
            y += this.addToggleButton(centerX, y, "Armor and Items", 
                "Log equipped items and armor", config.includeEquipment, 
                button -> {
                    config.includeEquipment = !config.includeEquipment;
                    updateButtonText(button, config.includeEquipment);
                });

            // ===== PERFORMANCE SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "Performance", 0xFFFF88);
            
            y += this.addToggleButton(centerX, y, "Performance Metrics", 
                "Record render and simulation distance", config.includePerformanceMetrics, 
                button -> {
                    config.includePerformanceMetrics = !config.includePerformanceMetrics;
                    updateButtonText(button, config.includePerformanceMetrics);
                });

            // ===== SAVE BUTTON =====
            y += SECTION_PADDING + 20;
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save and Close").formatted(Formatting.GREEN, Formatting.BOLD), 
                button -> this.close())
                .dimensions(centerX - BUTTON_WIDTH / 2, Math.min(y, this.height - 40), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

            // Reset button
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Reset to Defaults").formatted(Formatting.YELLOW), 
                button -> resetDefaults())
                .dimensions(centerX - BUTTON_WIDTH / 2, this.height - 30, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        }

        private int drawSection(int centerX, int y, String title, int color) {
            // Section title spacing
            return y + 8;
        }

        private int addToggleButton(int centerX, int y, String icon, String description, boolean enabled, java.util.function.Consumer<ButtonWidget> onPress) {
            Text buttonText = createModernToggleText(icon, enabled);
            ButtonWidget button = ButtonWidget.builder(buttonText, btn -> onPress.accept(btn))
                .dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
            this.addDrawableChild(button);
            return BUTTON_HEIGHT + SPACING;
        }

        private static Text createModernToggleText(String icon, boolean enabled) {
            String statusText = enabled ? "[ON]" : "[OFF]";
            Formatting color = enabled ? Formatting.GREEN : Formatting.GRAY;
            return Text.literal(statusText + " ").formatted(color)
                .append(Text.literal(icon).formatted(Formatting.WHITE));
        }

        private static void updateButtonText(ButtonWidget button, boolean enabled) {
            String currentText = button.getMessage().getString();
            // Extract label from current text
            String label = currentText.replaceAll("^\\[ON\\]\\s+|^\\[OFF\\]\\s+", "");
            button.setMessage(createModernToggleText(label, enabled));
        }

        private void resetDefaults() {
            ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
            config.writePngMetadata = true;
            config.writeXmpSidecar = true;
            config.writeJsonSidecar = true;
            config.includeWorldSeed = true;
            config.includePerformanceMetrics = true;
            config.includePlayerStatus = true;
            config.includeEquipment = true;
            config.includePotionEffects = true;
            config.includeCoordinates = true;
            config.includeBiomeInfo = true;
            config.includeWeatherInfo = true;
            ScreenshotMetadataConfig.save();
            this.init();
        }

        @Override
        public void close() {
            ScreenshotMetadataConfig.save();
            this.client.setScreen(parent);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            
            // Modern gradient header background
            context.fill(0, 0, this.width, 65, 0xFF1a1a2e);
            context.fill(0, 65, this.width, 66, 0xFF16213e);
            
            // Main title with shadow
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("Screenshot Metadata Configuration").formatted(Formatting.BOLD, Formatting.AQUA), 
                this.width / 2, 12, 0x88FFFF);
            
            // Subtitle
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("Customize what metadata gets saved with your screenshots").formatted(Formatting.GRAY), 
                this.width / 2, 28, 0xAAAAAA);

            // Version info
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("Version " + ScreenshotMetadataMod.MOD_VERSION).formatted(Formatting.DARK_GRAY), 
                this.width / 2, 45, 0x666666);

            // Draw content
            context.enableScissor(0, 70, this.width, this.height - 70);
            super.render(context, mouseX, mouseY, delta);
            context.disableScissor();

            // Bottom shadow bar
            context.fill(0, this.height - 5, this.width, this.height, 0xFF16213e);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            scrollOffset = Math.max(0, Math.min(scrollOffset - (int)(verticalAmount * 10), 200));
            this.init();
            return true;
        }
    }
}
