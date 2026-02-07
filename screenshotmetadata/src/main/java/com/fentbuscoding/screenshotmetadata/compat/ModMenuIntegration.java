package com.fentbuscoding.screenshotmetadata.compat;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import com.fentbuscoding.screenshotmetadata.config.ScreenshotMetadataConfig;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        private static final int HEADER_MIN_HEIGHT = 28;
        private static final int HEADER_MAX_HEIGHT = 40;
        private static final int HEADER_PADDING = 6;
        private static final int CONTENT_BOTTOM_PADDING = 56;
        private static final int SECTION_TITLE_HEIGHT = 12;
        private static final int SECTION_LINE_WIDTH = 140;
        private static final int SECTION_TOGGLE_SIZE = 16;
        private static final int TEMPLATE_FIELD_HEIGHT = 20;
        private static final int TEMPLATE_HELP_HEIGHT = 26;
        private static final int SCROLL_STEP = 16;
        private static final float SCROLL_SMOOTHING = 12.0f;

        private final List<Section> sections = new ArrayList<>();
        private final List<TooltipEntry> tooltipEntries = new ArrayList<>();
        private final Map<String, Boolean> collapsedSections = new HashMap<>();
        private float scrollOffset = 0f;
        private float targetScrollOffset = 0f;
        private int appliedScrollOffset = 0;
        private int maxScroll = 0;
        private int headerHeight = HEADER_MAX_HEIGHT;
        private int contentTop = HEADER_MAX_HEIGHT + HEADER_PADDING;
        private long lastScrollUpdateMs = 0L;
        private TextFieldWidget templateField;
        private int templateFieldY = -1;
        private String templatePreview = "";

        protected ConfigScreen(Screen parent) {
            super(Text.literal("Screenshot Metadata Config").formatted(Formatting.BOLD));
            this.parent = parent;
        }

        @Override
        protected void init() {
            this.clearChildren();
            this.sections.clear();
            this.tooltipEntries.clear();
            this.templateField = null;
            this.templateFieldY = -1;
            ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
            updateLayoutMetrics();
            int centerX = this.width / 2;
            int y = contentTop - appliedScrollOffset;

            // ===== OUTPUT FORMATS SECTION =====
            y = drawSection(centerX, y, "output_formats", "Output Formats", 0x88FF88);
            
            if (!isCollapsed("output_formats")) {
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
            } else {
                y += 4;
            }

            // ===== WORLD DATA SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "world_data", "World Data", 0x88CCFF);
            
            if (!isCollapsed("world_data")) {
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

                y += this.addToggleButton(centerX, y, "Weather Info",
                "Record rain, thunder, and weather state", config.includeWeatherInfo,
                button -> {
                    config.includeWeatherInfo = !config.includeWeatherInfo;
                    updateButtonText(button, config.includeWeatherInfo);
                });
            } else {
                y += 4;
            }

            // ===== PRIVACY & NAMING SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "privacy_naming", "Privacy & Naming", 0x88A8FF);

            if (!isCollapsed("privacy_naming")) {
                y += this.addToggleButton(centerX, y, "Privacy Mode",
                    "Obfuscate coords, hide server IP, and hash world seed", config.privacyMode,
                    button -> {
                        config.privacyMode = !config.privacyMode;
                        updateButtonText(button, config.privacyMode);
                    });

                y += this.addToggleButton(centerX, y, "Custom Filenames",
                    "Enable screenshot name templates", config.renameScreenshots,
                    button -> {
                        config.renameScreenshots = !config.renameScreenshots;
                        updateButtonText(button, config.renameScreenshots);
                    });

                y += this.addTemplateEditor(centerX, y, config);
            } else {
                y += 4;
            }

            // ===== PLAYER STATUS SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "player_status", "Player Status", 0xFF88CC);
            
            if (!isCollapsed("player_status")) {
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
            } else {
                y += 4;
            }

            // ===== EQUIPMENT SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "equipment", "Equipment", 0xFFCC88);
            
            if (!isCollapsed("equipment")) {
                y += this.addToggleButton(centerX, y, "Armor and Items", 
                "Log equipped items and armor", config.includeEquipment, 
                button -> {
                    config.includeEquipment = !config.includeEquipment;
                    updateButtonText(button, config.includeEquipment);
                });
            } else {
                y += 4;
            }

            // ===== PERFORMANCE SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "performance", "Performance", 0xFFFF88);
            
            if (!isCollapsed("performance")) {
                y += this.addToggleButton(centerX, y, "Performance Metrics", 
                "Record render and simulation distance", config.includePerformanceMetrics, 
                button -> {
                    config.includePerformanceMetrics = !config.includePerformanceMetrics;
                    updateButtonText(button, config.includePerformanceMetrics);
                });
            } else {
                y += 4;
            }

            // ===== SIDECAR EXTRAS SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "sidecar_extras", "Sidecar Extras", 0x88FFCC);

            if (!isCollapsed("sidecar_extras")) {
                y += this.addToggleButton(centerX, y, "Modpack Context",
                    "Add resource packs, shaders, and mod list to JSON only", config.includeModpackContext,
                    button -> {
                        config.includeModpackContext = !config.includeModpackContext;
                        updateButtonText(button, config.includeModpackContext);
                    });
            } else {
                y += 4;
            }

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

            int contentHeight = Math.max(0, (y + appliedScrollOffset) - contentTop);
            int viewHeight = Math.max(0, this.height - contentTop - CONTENT_BOTTOM_PADDING);
            maxScroll = Math.max(0, contentHeight - viewHeight);
            targetScrollOffset = clampFloat(targetScrollOffset, 0f, maxScroll);
            scrollOffset = clampFloat(scrollOffset, 0f, maxScroll);
            appliedScrollOffset = clampInt(Math.round(scrollOffset), 0, maxScroll);
        }

        private int drawSection(int centerX, int y, String key, String title, int color) {
            sections.add(new Section(y, title, color));
            addCollapseButton(centerX, y - 2, key);
            return y + SECTION_TITLE_HEIGHT + 6;
        }

        private int addToggleButton(int centerX, int y, String icon, String description, boolean enabled, java.util.function.Consumer<ButtonWidget> onPress) {
            Text buttonText = createModernToggleText(icon, enabled);
            ButtonWidget button = ButtonWidget.builder(buttonText, btn -> onPress.accept(btn))
                .dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
            this.addDrawableChild(button);
            if (description != null && !description.isBlank()) {
                tooltipEntries.add(new TooltipEntry(button, List.of(Text.literal(description))));
            }
            return BUTTON_HEIGHT + SPACING;
        }

        private int addTemplateEditor(int centerX, int y, ScreenshotMetadataConfig config) {
            int fieldX = centerX - BUTTON_WIDTH / 2;
            templateFieldY = y;
            templateField = new TextFieldWidget(
                this.textRenderer,
                fieldX,
                y,
                BUTTON_WIDTH,
                TEMPLATE_FIELD_HEIGHT,
                Text.literal("Filename Template")
            );
            templateField.setPlaceholder(Text.literal("{date}_{dimension}_X{x}_Z{z}"));
            templateField.setMaxLength(120);
            templateField.setText(config.screenshotNameTemplate == null ? "" : config.screenshotNameTemplate);
            templateField.setChangedListener(newValue -> {
                config.screenshotNameTemplate = newValue == null ? "" : newValue.trim();
                templatePreview = createTemplatePreview(config.screenshotNameTemplate);
            });
            this.addDrawableChild(templateField);
            templatePreview = createTemplatePreview(config.screenshotNameTemplate);

            int buttonY = y + TEMPLATE_FIELD_HEIGHT + 2;
            int smallButtonWidth = (BUTTON_WIDTH - 6) / 2;

            ButtonWidget defaultsButton = ButtonWidget.builder(
                    Text.literal("Default").formatted(Formatting.GRAY),
                    btn -> {
                        config.screenshotNameTemplate = "{date}_{dimension}_X{x}_Z{z}";
                        templateField.setText(config.screenshotNameTemplate);
                        templatePreview = createTemplatePreview(config.screenshotNameTemplate);
                    })
                .dimensions(fieldX, buttonY, smallButtonWidth, 16)
                .build();
            this.addDrawableChild(defaultsButton);

            ButtonWidget biomeTimeButton = ButtonWidget.builder(
                    Text.literal("Biome+Time").formatted(Formatting.AQUA),
                    btn -> {
                        config.screenshotNameTemplate = "{biome}_{time}";
                        templateField.setText(config.screenshotNameTemplate);
                        templatePreview = createTemplatePreview(config.screenshotNameTemplate);
                    })
                .dimensions(fieldX + smallButtonWidth + 6, buttonY, smallButtonWidth, 16)
                .build();
            this.addDrawableChild(biomeTimeButton);

            tooltipEntries.add(new TooltipEntry(defaultsButton, List.of(
                Text.literal("Use the recommended default template")
            )));
            tooltipEntries.add(new TooltipEntry(biomeTimeButton, List.of(
                Text.literal("Quick preset for biome + time naming")
            )));

            return TEMPLATE_FIELD_HEIGHT + 16 + TEMPLATE_HELP_HEIGHT + SPACING;
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
            config.includeModpackContext = true;
            config.privacyMode = false;
            config.renameScreenshots = false;
            config.screenshotNameTemplate = "{date}_{dimension}_X{x}_Z{z}";
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
            updateSmoothScroll();

            // Modern header background (smaller to avoid cutting content)
            context.fill(0, 0, this.width, headerHeight, 0xCC14192B);
            context.fill(0, headerHeight - 1, this.width, headerHeight, 0xFF1F2750);

            int centerX = this.width / 2;
            boolean compactHeader = headerHeight <= 42;
            if (compactHeader) {
                context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Screenshot Metadata v" + ScreenshotMetadataMod.MOD_VERSION)
                        .formatted(Formatting.BOLD, Formatting.AQUA),
                    centerX, 10, 0xA0EFFF);
            } else {
                context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Screenshot Metadata Configuration").formatted(Formatting.BOLD, Formatting.AQUA),
                    centerX, 10, 0x88FFFF);

                context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Customize what metadata gets saved with your screenshots").formatted(Formatting.GRAY),
                    centerX, 26, 0xAAAAAA);

                context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Version " + ScreenshotMetadataMod.MOD_VERSION).formatted(Formatting.DARK_GRAY),
                    centerX, 40, 0x666666);
            }

            // Draw content with scissor for scrolling
            context.enableScissor(0, contentTop, this.width, this.height - 6);
            renderSections(context);
            super.render(context, mouseX, mouseY, delta);
            renderTemplateEditorHelp(context);
            context.disableScissor();

            renderTooltipIfHovered(context, mouseX, mouseY);

            // Bottom shadow bar
            context.fill(0, this.height - 5, this.width, this.height, 0xFF16213e);
        }

        @Override
        public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
            // Avoid applyBlur to prevent "Can only blur once per frame" crashes.
            this.renderInGameBackground(context);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            targetScrollOffset = clampFloat(targetScrollOffset - (float) (verticalAmount * SCROLL_STEP), 0f, maxScroll);
            return true;
        }

        private void renderSections(DrawContext context) {
            int centerX = this.width / 2;
            for (Section section : sections) {
                int y = section.y;
                if (y < contentTop - 24 || y > this.height) {
                    continue;
                }
                int textColor = 0xFF000000 | section.color;
                context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(section.title).formatted(Formatting.BOLD),
                    centerX, y, textColor);
                int lineY = y + SECTION_TITLE_HEIGHT;
                context.fill(centerX - SECTION_LINE_WIDTH, lineY, centerX + SECTION_LINE_WIDTH, lineY + 1, 0x33FFFFFF);
            }
        }

        private void updateLayoutMetrics() {
            int targetHeader = Math.max(HEADER_MIN_HEIGHT, Math.min(HEADER_MAX_HEIGHT, this.height / 6));
            headerHeight = targetHeader;
            contentTop = headerHeight + HEADER_PADDING;
        }

        private void updateSmoothScroll() {
            long nowMs = Util.getMeasuringTimeMs();
            if (lastScrollUpdateMs == 0L) {
                lastScrollUpdateMs = nowMs;
                return;
            }
            float deltaSeconds = (nowMs - lastScrollUpdateMs) / 1000f;
            lastScrollUpdateMs = nowMs;

            float t = 1f - (float) Math.exp(-SCROLL_SMOOTHING * deltaSeconds);
            scrollOffset += (targetScrollOffset - scrollOffset) * t;

            int nextApplied = clampInt(Math.round(scrollOffset), 0, maxScroll);
            if (nextApplied != appliedScrollOffset) {
                appliedScrollOffset = nextApplied;
                this.init();
            }
        }

        private static int clampInt(int value, int min, int max) {
            return Math.max(min, Math.min(value, max));
        }

        private static float clampFloat(float value, float min, float max) {
            return Math.max(min, Math.min(value, max));
        }

        private void renderTooltipIfHovered(DrawContext context, int mouseX, int mouseY) {
            for (TooltipEntry entry : tooltipEntries) {
                if (entry.button.isMouseOver(mouseX, mouseY)) {
                    context.drawTooltip(this.textRenderer, entry.lines, mouseX, mouseY);
                    return;
                }
            }
        }

        private void renderTemplateEditorHelp(DrawContext context) {
            if (templateField == null || templateFieldY < 0) {
                return;
            }
            int y = templateFieldY + TEMPLATE_FIELD_HEIGHT + 20;
            if (y < contentTop - 16 || y > this.height - 20) {
                return;
            }

            context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("{date} {time} {dimension} {biome} {x} {y} {z} {world} {player}")
                    .formatted(Formatting.DARK_GRAY),
                templateField.getX(),
                y,
                0x777777
            );
            context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Preview: " + templatePreview).formatted(Formatting.GRAY),
                templateField.getX(),
                y + 10,
                0x9B9B9B
            );
        }

        private String createTemplatePreview(String template) {
            String value = template == null || template.isBlank()
                ? "{date}_{dimension}_X{x}_Z{z}"
                : template;
            String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"));
            value = value.replace("{date}", date);
            value = value.replace("{time}", time);
            value = value.replace("{dimension}", "Overworld");
            value = value.replace("{biome}", "Cherry_Grove");
            value = value.replace("{x}", "65");
            value = value.replace("{y}", "92");
            value = value.replace("{z}", "-88");
            value = value.replace("{world}", "New_World");
            value = value.replace("{player}", "Player");
            value = value.replaceAll("[\\\\/:*?\"<>|]", "_");
            return value + ".png";
        }

        private void addCollapseButton(int centerX, int y, String key) {
            boolean collapsed = isCollapsed(key);
            Text label = Text.literal(collapsed ? "+" : "-").formatted(Formatting.GRAY);
            ButtonWidget button = ButtonWidget.builder(label, btn -> {
                    collapsedSections.put(key, !collapsed);
                    this.init();
                })
                .dimensions(centerX + SECTION_LINE_WIDTH + 8, y, SECTION_TOGGLE_SIZE, SECTION_TOGGLE_SIZE)
                .build();
            this.addDrawableChild(button);
            tooltipEntries.add(new TooltipEntry(button, List.of(
                Text.literal(collapsed ? "Expand section" : "Collapse section")
            )));
        }

        private boolean isCollapsed(String key) {
            return collapsedSections.getOrDefault(key, false);
        }

    }

    private static final class Section {
        private final int y;
        private final String title;
        private final int color;

        private Section(int y, String title, int color) {
            this.y = y;
            this.title = title;
            this.color = color;
        }
    }

    private static final class TooltipEntry {
        private final ButtonWidget button;
        private final List<Text> lines;

        private TooltipEntry(ButtonWidget button, List<Text> lines) {
            this.button = button;
            this.lines = lines;
        }
    }

}
