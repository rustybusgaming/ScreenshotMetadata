package com.fentbuscoding.screenshotmetadata.compat;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import com.fentbuscoding.screenshotmetadata.config.ScreenshotMetadataConfig;
import com.fentbuscoding.screenshotmetadata.config.ScreenshotMetadataConfig.MetadataProfile;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

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
        private static final int PRIVACY_PREVIEW_HEIGHT = 44;
        private static final int PROFILE_BUTTON_HEIGHT = 20;
        private static final int SCROLL_STEP = 16;

        private final List<Section> sections = new ArrayList<>();
        private final List<TooltipEntry> tooltipEntries = new ArrayList<>();
        private final Map<String, Boolean> collapsedSections = new HashMap<>();
        private int appliedScrollOffset = 0;
        private int maxScroll = 0;
        private int headerHeight = HEADER_MAX_HEIGHT;
        private int contentTop = HEADER_MAX_HEIGHT + HEADER_PADDING;
        private EditBox templateField;
        private int templateFieldY = -1;
        private int privacyPreviewY = -1;
        private String templatePreview = "";

        protected ConfigScreen(Screen parent) {
            super(Component.translatable("screen.screenshotmetadata.config.title").withStyle(ChatFormatting.BOLD));
            this.parent = parent;
        }

        @Override
        protected void init() {
            this.clearWidgets();
            this.sections.clear();
            this.tooltipEntries.clear();
            this.templateField = null;
            this.templateFieldY = -1;
            this.privacyPreviewY = -1;
            ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
            updateLayoutMetrics();
            int centerX = this.width / 2;
            int y = contentTop - appliedScrollOffset;

            // ===== PROFILE SECTION =====
            y = drawSection(centerX, y, "profiles",
                Component.translatable("screen.screenshotmetadata.config.section.profiles"),
                0x88FFD2);

            if (!isCollapsed("profiles")) {
                y += this.addProfileButtons(centerX, y, config);
            } else {
                y += 4;
            }

            // ===== OUTPUT FORMATS SECTION =====
            y = drawSection(centerX, y, "output_formats",
                Component.translatable("screen.screenshotmetadata.config.section.output_formats"),
                0x88FF88);
            
            if (!isCollapsed("output_formats")) {
                Component pngLabel = Component.translatable("screen.screenshotmetadata.config.toggle.png");
                y += this.addToggleButton(centerX, y, pngLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.png.desc"), config.writePngMetadata,
                button -> {
                    config.writePngMetadata = !config.writePngMetadata;
                    updateButtonText(button, pngLabel, config.writePngMetadata);
                });
            
                Component embeddedXmpLabel = Component.translatable("screen.screenshotmetadata.config.toggle.embedded_xmp");
                y += this.addToggleButton(centerX, y, embeddedXmpLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.embedded_xmp.desc"), config.writeEmbeddedXmp,
                button -> {
                    config.writeEmbeddedXmp = !config.writeEmbeddedXmp;
                    updateButtonText(button, embeddedXmpLabel, config.writeEmbeddedXmp);
                });

                Component xmpLabel = Component.translatable("screen.screenshotmetadata.config.toggle.xmp");
                y += this.addToggleButton(centerX, y, xmpLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.xmp.desc"), config.writeXmpSidecar,
                button -> {
                    config.writeXmpSidecar = !config.writeXmpSidecar;
                    updateButtonText(button, xmpLabel, config.writeXmpSidecar);
                });

                Component jsonLabel = Component.translatable("screen.screenshotmetadata.config.toggle.json");
                y += this.addToggleButton(centerX, y, jsonLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.json.desc"), config.writeJsonSidecar,
                button -> {
                    config.writeJsonSidecar = !config.writeJsonSidecar;
                    updateButtonText(button, jsonLabel, config.writeJsonSidecar);
                });
            } else {
                y += 4;
            }

            // ===== WORLD DATA SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "world_data",
                Component.translatable("screen.screenshotmetadata.config.section.world_data"),
                0x88CCFF);
            
            if (!isCollapsed("world_data")) {
                Component seedLabel = Component.translatable("screen.screenshotmetadata.config.toggle.world_seed");
                y += this.addToggleButton(centerX, y, seedLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.world_seed.desc"), config.includeWorldSeed,
                button -> {
                    config.includeWorldSeed = !config.includeWorldSeed;
                    markProfileCustom(config);
                    updateButtonText(button, seedLabel, config.includeWorldSeed);
                });

                Component biomeLabel = Component.translatable("screen.screenshotmetadata.config.toggle.biome");
                y += this.addToggleButton(centerX, y, biomeLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.biome.desc"), config.includeBiomeInfo,
                button -> {
                    config.includeBiomeInfo = !config.includeBiomeInfo;
                    markProfileCustom(config);
                    updateButtonText(button, biomeLabel, config.includeBiomeInfo);
                });

                Component experienceLabel = Component.translatable("screen.screenshotmetadata.config.toggle.experience");
                y += this.addToggleButton(centerX, y, experienceLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.experience.desc"), config.includeExperience,
                button -> {
                    config.includeExperience = !config.includeExperience;
                    markProfileCustom(config);
                    updateButtonText(button, experienceLabel, config.includeExperience);
                });

                Component playerStateLabel = Component.translatable("screen.screenshotmetadata.config.toggle.playerstate");
                y += this.addToggleButton(centerX, y, playerStateLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.playerstate.desc"), config.includePlayerState,
                button -> {
                    config.includePlayerState = !config.includePlayerState;
                    markProfileCustom(config);
                    updateButtonText(button, playerStateLabel, config.includePlayerState);
                });

                Component lookTargetLabel = Component.translatable("screen.screenshotmetadata.config.toggle.looktarget");
                y += this.addToggleButton(centerX, y, lookTargetLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.looktarget.desc"), config.includeLookTarget,
                button -> {
                    config.includeLookTarget = !config.includeLookTarget;
                    markProfileCustom(config);
                    updateButtonText(button, lookTargetLabel, config.includeLookTarget);
                });

                Component lightLevelsLabel = Component.translatable("screen.screenshotmetadata.config.toggle.lightlevels");
                y += this.addToggleButton(centerX, y, lightLevelsLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.lightlevels.desc"), config.includeLightLevels,
                button -> {
                    config.includeLightLevels = !config.includeLightLevels;
                    markProfileCustom(config);
                    updateButtonText(button, lightLevelsLabel, config.includeLightLevels);
                });

                                Component coordsLabel = Component.translatable("screen.screenshotmetadata.config.toggle.coordinates");
                y += this.addToggleButton(centerX, y, coordsLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.coordinates.desc"), config.includeCoordinates,
                button -> {
                    config.includeCoordinates = !config.includeCoordinates;
                    markProfileCustom(config);
                    updateButtonText(button, coordsLabel, config.includeCoordinates);
                });

                Component weatherLabel = Component.translatable("screen.screenshotmetadata.config.toggle.weather");
                y += this.addToggleButton(centerX, y, weatherLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.weather.desc"), config.includeWeatherInfo,
                button -> {
                    config.includeWeatherInfo = !config.includeWeatherInfo;
                    markProfileCustom(config);
                    updateButtonText(button, weatherLabel, config.includeWeatherInfo);
                });
            } else {
                y += 4;
            }

            // ===== PRIVACY & NAMING SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "privacy_naming",
                Component.translatable("screen.screenshotmetadata.config.section.privacy"),
                0x88A8FF);

            if (!isCollapsed("privacy_naming")) {
                Component privacyLabel = Component.translatable("screen.screenshotmetadata.config.toggle.privacy");
                y += this.addToggleButton(centerX, y, privacyLabel,
                    Component.translatable("screen.screenshotmetadata.config.toggle.privacy.desc"), config.privacyMode,
                    button -> {
                        config.privacyMode = !config.privacyMode;
                        markProfileCustom(config);
                        updateButtonText(button, privacyLabel, config.privacyMode);
                    });

                Component renameLabel = Component.translatable("screen.screenshotmetadata.config.toggle.rename");
                y += this.addToggleButton(centerX, y, renameLabel,
                    Component.translatable("screen.screenshotmetadata.config.toggle.rename.desc"), config.renameScreenshots,
                    button -> {
                        config.renameScreenshots = !config.renameScreenshots;
                        updateButtonText(button, renameLabel, config.renameScreenshots);
                    });

                y += this.addTemplateEditor(centerX, y, config);
                y += this.addPrivacyPreview(y);
            } else {
                y += 4;
            }

            // ===== PLAYER STATUS SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "player_status",
                Component.translatable("screen.screenshotmetadata.config.section.player_status"),
                0xFF88CC);
            
            if (!isCollapsed("player_status")) {
                Component healthLabel = Component.translatable("screen.screenshotmetadata.config.toggle.player_status");
                y += this.addToggleButton(centerX, y, healthLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.player_status.desc"), config.includePlayerStatus,
                button -> {
                    config.includePlayerStatus = !config.includePlayerStatus;
                    markProfileCustom(config);
                    updateButtonText(button, healthLabel, config.includePlayerStatus);
                });

                Component potionLabel = Component.translatable("screen.screenshotmetadata.config.toggle.potion");
                y += this.addToggleButton(centerX, y, potionLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.potion.desc"), config.includePotionEffects,
                button -> {
                    config.includePotionEffects = !config.includePotionEffects;
                    markProfileCustom(config);
                    updateButtonText(button, potionLabel, config.includePotionEffects);
                });
            } else {
                y += 4;
            }

            // ===== EQUIPMENT SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "equipment",
                Component.translatable("screen.screenshotmetadata.config.section.equipment"),
                0xFFCC88);
            
            if (!isCollapsed("equipment")) {
                Component equipmentLabel = Component.translatable("screen.screenshotmetadata.config.toggle.equipment");
                y += this.addToggleButton(centerX, y, equipmentLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.equipment.desc"), config.includeEquipment,
                button -> {
                    config.includeEquipment = !config.includeEquipment;
                    markProfileCustom(config);
                    updateButtonText(button, equipmentLabel, config.includeEquipment);
                });
            } else {
                y += 4;
            }

            // ===== PERFORMANCE SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "performance",
                Component.translatable("screen.screenshotmetadata.config.section.performance"),
                0xFFFF88);
            
            if (!isCollapsed("performance")) {
                Component perfLabel = Component.translatable("screen.screenshotmetadata.config.toggle.performance");
                y += this.addToggleButton(centerX, y, perfLabel,
                Component.translatable("screen.screenshotmetadata.config.toggle.performance.desc"), config.includePerformanceMetrics,
                button -> {
                    config.includePerformanceMetrics = !config.includePerformanceMetrics;
                    markProfileCustom(config);
                    updateButtonText(button, perfLabel, config.includePerformanceMetrics);
                });
            } else {
                y += 4;
            }

            // ===== SIDECAR EXTRAS SECTION =====
            y += SECTION_PADDING;
            y = drawSection(centerX, y, "sidecar_extras",
                Component.translatable("screen.screenshotmetadata.config.section.sidecar_extras"),
                0x88FFCC);

            if (!isCollapsed("sidecar_extras")) {
                Component modpackLabel = Component.translatable("screen.screenshotmetadata.config.toggle.modpack");
                y += this.addToggleButton(centerX, y, modpackLabel,
                    Component.translatable("screen.screenshotmetadata.config.toggle.modpack.desc"), config.includeModpackContext,
                    button -> {
                        config.includeModpackContext = !config.includeModpackContext;
                        markProfileCustom(config);
                        updateButtonText(button, modpackLabel, config.includeModpackContext);
                    });
            } else {
                y += 4;
            }

            // ===== SAVE BUTTON =====
            y += SECTION_PADDING + 20;
            this.addRenderableWidget(Button.builder(
                Component.translatable("screen.screenshotmetadata.config.save").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), 
                button -> this.onClose())
                .bounds(centerX - BUTTON_WIDTH / 2, Math.min(y, this.height - 40), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

            // Reset button
            this.addRenderableWidget(Button.builder(
                Component.translatable("screen.screenshotmetadata.config.reset").withStyle(ChatFormatting.YELLOW), 
                button -> resetDefaults())
                .bounds(centerX - BUTTON_WIDTH / 2, this.height - 30, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

            int contentHeight = Math.max(0, (y + appliedScrollOffset) - contentTop);
            int viewHeight = Math.max(0, this.height - contentTop - CONTENT_BOTTOM_PADDING);
            maxScroll = Math.max(0, contentHeight - viewHeight);
            appliedScrollOffset = clampInt(appliedScrollOffset, 0, maxScroll);
        }

        private int drawSection(int centerX, int y, String key, Component title, int color) {
            sections.add(new Section(y, title, color));
            addCollapseButton(centerX, y - 2, key);
            return y + SECTION_TITLE_HEIGHT + 6;
        }

        private int addToggleButton(int centerX, int y, Component label, Component description, boolean enabled, java.util.function.Consumer<Button> onPress) {
            Component buttonText = createModernToggleText(label, enabled);
            Button button = Button.builder(buttonText, btn -> onPress.accept(btn))
                .bounds(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
            this.addRenderableWidget(button);
            if (description != null) {
                tooltipEntries.add(new TooltipEntry(button, List.of(description)));
            }
            return BUTTON_HEIGHT + SPACING;
        }

        private int addTemplateEditor(int centerX, int y, ScreenshotMetadataConfig config) {
            int fieldX = centerX - BUTTON_WIDTH / 2;
            templateFieldY = y;
            templateField = new EditBox(
                this.font,
                fieldX,
                y,
                BUTTON_WIDTH,
                TEMPLATE_FIELD_HEIGHT,
                Component.translatable("screen.screenshotmetadata.config.template.field")
            );
            templateField.setHint(Component.translatable("screen.screenshotmetadata.config.template.placeholder"));
            templateField.setMaxLength(120);
            templateField.setValue(config.screenshotNameTemplate == null ? "" : config.screenshotNameTemplate);
            templateField.setResponder(newValue -> {
                config.screenshotNameTemplate = newValue == null ? "" : newValue.trim();
                templatePreview = createTemplatePreview(config.screenshotNameTemplate);
            });
            this.addRenderableWidget(templateField);
            templatePreview = createTemplatePreview(config.screenshotNameTemplate);

            int buttonY = y + TEMPLATE_FIELD_HEIGHT + 2;
            int smallButtonWidth = (BUTTON_WIDTH - 6) / 2;

            Button defaultsButton = Button.builder(
                    Component.translatable("screen.screenshotmetadata.config.template.default").withStyle(ChatFormatting.GRAY),
                    btn -> {
                        config.screenshotNameTemplate = "{date}_{dimension}_X{x}_Z{z}";
                        templateField.setValue(config.screenshotNameTemplate);
                        templatePreview = createTemplatePreview(config.screenshotNameTemplate);
                    })
                .bounds(fieldX, buttonY, smallButtonWidth, 16)
                .build();
            this.addRenderableWidget(defaultsButton);

            Button biomeTimeButton = Button.builder(
                    Component.translatable("screen.screenshotmetadata.config.template.biome_time").withStyle(ChatFormatting.AQUA),
                    btn -> {
                        config.screenshotNameTemplate = "{biome}_{time}";
                        templateField.setValue(config.screenshotNameTemplate);
                        templatePreview = createTemplatePreview(config.screenshotNameTemplate);
                    })
                .bounds(fieldX + smallButtonWidth + 6, buttonY, smallButtonWidth, 16)
                .build();
            this.addRenderableWidget(biomeTimeButton);

            tooltipEntries.add(new TooltipEntry(defaultsButton, List.of(
                Component.translatable("screen.screenshotmetadata.config.template.default.tip")
            )));
            tooltipEntries.add(new TooltipEntry(biomeTimeButton, List.of(
                Component.translatable("screen.screenshotmetadata.config.template.biome_time.tip")
            )));

            return TEMPLATE_FIELD_HEIGHT + 16 + TEMPLATE_HELP_HEIGHT + SPACING;
        }

        private int addPrivacyPreview(int y) {
            privacyPreviewY = y;
            return PRIVACY_PREVIEW_HEIGHT + SPACING;
        }

        private int addProfileButtons(int centerX, int y, ScreenshotMetadataConfig config) {
            int smallButtonWidth = (BUTTON_WIDTH - 12) / 3;
            int x = centerX - BUTTON_WIDTH / 2;

            MetadataProfile active = config.getMetadataProfile();
            Button fullButton = createProfileButton(
                x,
                y,
                smallButtonWidth,
                MetadataProfile.FULL,
                active == MetadataProfile.FULL,
                config
            );
            this.addRenderableWidget(fullButton);
            tooltipEntries.add(new TooltipEntry(fullButton, List.of(
                Component.translatable("screen.screenshotmetadata.config.profile.full.desc")
            )));

            Button lightButton = createProfileButton(
                x + smallButtonWidth + 6,
                y,
                smallButtonWidth,
                MetadataProfile.LIGHTWEIGHT,
                active == MetadataProfile.LIGHTWEIGHT,
                config
            );
            this.addRenderableWidget(lightButton);
            tooltipEntries.add(new TooltipEntry(lightButton, List.of(
                Component.translatable("screen.screenshotmetadata.config.profile.lightweight.desc")
            )));

            Button privacyButton = createProfileButton(
                x + (smallButtonWidth + 6) * 2,
                y,
                smallButtonWidth,
                MetadataProfile.PRIVACY,
                active == MetadataProfile.PRIVACY,
                config
            );
            this.addRenderableWidget(privacyButton);
            tooltipEntries.add(new TooltipEntry(privacyButton, List.of(
                Component.translatable("screen.screenshotmetadata.config.profile.privacy.desc")
            )));

            return PROFILE_BUTTON_HEIGHT + SPACING;
        }

        private Button createProfileButton(int x,
                                                 int y,
                                                 int width,
                                                 MetadataProfile profile,
                                                 boolean selected,
                                                 ScreenshotMetadataConfig config) {
            Component label = profileLabel(profile, selected);
            return Button.builder(label, btn -> {
                    config.applyProfile(profile);
                    ScreenshotMetadataConfig.save();
                    this.init();
                })
                .bounds(x, y, width, PROFILE_BUTTON_HEIGHT)
                .build();
        }

        private Component profileLabel(MetadataProfile profile, boolean selected) {
            ChatFormatting color = selected ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            String key = switch (profile) {
                case FULL -> "screen.screenshotmetadata.config.profile.full";
                case LIGHTWEIGHT -> "screen.screenshotmetadata.config.profile.lightweight";
                case PRIVACY -> "screen.screenshotmetadata.config.profile.privacy";
                case CUSTOM -> "screen.screenshotmetadata.config.profile.custom";
            };
            return Component.literal(selected ? "> " : "")
                .withStyle(color)
                .append(Component.translatable(key).withStyle(ChatFormatting.WHITE));
        }

        private static Component createModernToggleText(Component label, boolean enabled) {
            String statusKey = enabled
                ? "screen.screenshotmetadata.toggle.on"
                : "screen.screenshotmetadata.toggle.off";
            ChatFormatting color = enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            return Component.literal("[")
                .withStyle(color)
                .append(Component.translatable(statusKey).withStyle(color))
                .append(Component.literal("] ").withStyle(color))
                .append(label.copy().withStyle(ChatFormatting.WHITE));
        }

        private static void updateButtonText(Button button, Component label, boolean enabled) {
            button.setMessage(createModernToggleText(label, enabled));
        }

        private static void markProfileCustom(ScreenshotMetadataConfig config) {
            config.setMetadataProfile(MetadataProfile.CUSTOM);
        }

        private void resetDefaults() {
            ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
            config.writePngMetadata = true;
            config.writeEmbeddedXmp = true;
            config.writeXmpSidecar = true;
            config.writeJsonSidecar = true;
            config.applyProfile(MetadataProfile.FULL);
            config.renameScreenshots = false;
            config.screenshotNameTemplate = "{date}_{dimension}_X{x}_Z{z}";
            ScreenshotMetadataConfig.save();
            this.init();
        }

        @Override
        public void onClose() {
            ScreenshotMetadataConfig.save();
            this.minecraft.setScreenAndShow(parent);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            // Modern header background (smaller to avoid cutting content)
            context.fill(0, 0, this.width, headerHeight, 0xCC14192B);
            context.fill(0, headerHeight - 1, this.width, headerHeight, 0xFF1F2750);

            int centerX = this.width / 2;
            boolean compactHeader = headerHeight <= 42;
            if (compactHeader) {
                context.centeredText(this.font,
                    Component.translatable("screen.screenshotmetadata.config.header.compact", ScreenshotMetadataMod.MOD_VERSION)
                        .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA),
                    centerX, 10, 0xA0EFFF);
            } else {
                context.centeredText(this.font,
                    Component.translatable("screen.screenshotmetadata.config.header.title")
                        .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA),
                    centerX, 10, 0x88FFFF);

                context.centeredText(this.font,
                    Component.translatable("screen.screenshotmetadata.config.header.subtitle")
                        .withStyle(ChatFormatting.GRAY),
                    centerX, 26, 0xAAAAAA);

                context.centeredText(this.font,
                    Component.translatable("screen.screenshotmetadata.config.header.version", ScreenshotMetadataMod.MOD_VERSION)
                        .withStyle(ChatFormatting.DARK_GRAY),
                    centerX, 40, 0x666666);
            }

            // Draw content with scissor for scrolling
            context.enableScissor(0, contentTop, this.width, this.height - 6);
            renderSections(context);
            super.extractRenderState(context, mouseX, mouseY, delta);
            renderPrivacyPreview(context);
            renderTemplateEditorHelp(context);
            context.disableScissor();

            renderTooltipIfHovered(context, mouseX, mouseY);

            // Bottom shadow bar
            context.fill(0, this.height - 5, this.width, this.height, 0xFF16213e);
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            // Avoid applyBlur to prevent "Can only blur once per frame" crashes.
            this.extractTransparentBackground(context);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            int nextOffset = clampInt(appliedScrollOffset - (int) Math.round(verticalAmount * SCROLL_STEP), 0, maxScroll);
            if (nextOffset != appliedScrollOffset) {
                appliedScrollOffset = nextOffset;
                this.init();
            }
            return true;
        }

        private void renderSections(GuiGraphicsExtractor context) {
            int centerX = this.width / 2;
            for (Section section : sections) {
                int y = section.y;
                if (y < contentTop - 24 || y > this.height) {
                    continue;
                }
                int textColor = 0xFF000000 | section.color;
                context.centeredText(this.font,
                    section.title.copy().withStyle(ChatFormatting.BOLD),
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

        private static int clampInt(int value, int min, int max) {
            return Math.max(min, Math.min(value, max));
        }

        private void renderTooltipIfHovered(GuiGraphicsExtractor context, int mouseX, int mouseY) {
            for (TooltipEntry entry : tooltipEntries) {
                if (entry.button.isMouseOver(mouseX, mouseY)) {
                    context.setTooltipForNextFrame(entry.lines.stream()
                        .map(net.minecraft.network.chat.Component::getVisualOrderText)
                        .toList(), mouseX, mouseY);
                    return;
                }
            }
        }

        private void renderPrivacyPreview(GuiGraphicsExtractor context) {
            if (privacyPreviewY < 0) {
                return;
            }
            int y = privacyPreviewY;
            if (y < contentTop - 16 || y > this.height - 40) {
                return;
            }

            ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
            String stateValue = config.privacyMode
                ? Component.translatable("screen.screenshotmetadata.toggle.on").getString()
                : Component.translatable("screen.screenshotmetadata.toggle.off").getString();

            int x = this.width / 2 - BUTTON_WIDTH / 2;
            int titleColor = config.privacyMode ? 0x78D0A0 : 0x9CA3AF;
            context.text(
                this.font,
                Component.translatable("screen.screenshotmetadata.config.privacy.preview.title", stateValue)
                    .withStyle(ChatFormatting.BOLD),
                x,
                y,
                titleColor
            );
            context.text(
                this.font,
                Component.translatable("screen.screenshotmetadata.config.privacy.preview.coords")
                    .withStyle(ChatFormatting.DARK_GRAY),
                x,
                y + 12,
                0x777777
            );
            context.text(
                this.font,
                Component.translatable("screen.screenshotmetadata.config.privacy.preview.server")
                    .withStyle(ChatFormatting.DARK_GRAY),
                x,
                y + 22,
                0x777777
            );
            context.text(
                this.font,
                Component.translatable("screen.screenshotmetadata.config.privacy.preview.seed")
                    .withStyle(ChatFormatting.DARK_GRAY),
                x,
                y + 32,
                0x777777
            );
        }

        private void renderTemplateEditorHelp(GuiGraphicsExtractor context) {
            if (templateField == null || templateFieldY < 0) {
                return;
            }
            int y = templateFieldY + TEMPLATE_FIELD_HEIGHT + 20;
            if (y < contentTop - 16 || y > this.height - 20) {
                return;
            }

            context.text(
                this.font,
                Component.translatable("screen.screenshotmetadata.config.template.tokens")
                    .withStyle(ChatFormatting.DARK_GRAY),
                templateField.getX(),
                y,
                0x777777
            );
            context.text(
                this.font,
                Component.translatable("screen.screenshotmetadata.config.template.preview", templatePreview)
                    .withStyle(ChatFormatting.GRAY),
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
            Component label = Component.literal(collapsed ? "+" : "-").withStyle(ChatFormatting.GRAY);
            Button button = Button.builder(label, btn -> {
                    collapsedSections.put(key, !collapsed);
                    this.init();
                })
                .bounds(centerX + SECTION_LINE_WIDTH + 8, y, SECTION_TOGGLE_SIZE, SECTION_TOGGLE_SIZE)
                .build();
            this.addRenderableWidget(button);
            tooltipEntries.add(new TooltipEntry(button, List.of(
                Component.translatable(collapsed
                    ? "screen.screenshotmetadata.config.section.expand"
                    : "screen.screenshotmetadata.config.section.collapse")
            )));
        }

        private boolean isCollapsed(String key) {
            return collapsedSections.getOrDefault(key, false);
        }

    }

    private static final class Section {
        private final int y;
        private final Component title;
        private final int color;

        private Section(int y, Component title, int color) {
            this.y = y;
            this.title = title;
            this.color = color;
        }
    }

    private static final class TooltipEntry {
        private final Button button;
        private final List<Component> lines;

        private TooltipEntry(Button button, List<Component> lines) {
            this.button = button;
            this.lines = lines;
        }
    }

}
