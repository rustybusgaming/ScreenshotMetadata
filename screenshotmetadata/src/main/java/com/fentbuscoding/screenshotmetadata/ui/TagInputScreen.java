package com.fentbuscoding.screenshotmetadata.ui;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import com.fentbuscoding.screenshotmetadata.config.ScreenshotMetadataConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class TagInputScreen extends Screen {
    private static final int FIELD_WIDTH = 260;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PRESET_BUTTON_WIDTH = 80;
    private static final int PRESET_BUTTON_HEIGHT = 18;
    private static final int PRESET_BUTTON_GAP = 6;
    private static final int PRESET_ROW_GAP = 4;
    private static final int PRESETS_PER_ROW = 3;

    private final Screen parent;
    private TextFieldWidget tagField;
    private List<String> presetTags = List.of();
    private int presetsLabelY = -1;
    private int presetsStartY = -1;
    private int presetsHelpY = -1;
    private boolean hasPresets = false;

    public TagInputScreen(Screen parent) {
        super(Text.translatable("screen.screenshotmetadata.tags.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int lineHeight = this.textRenderer.fontHeight;
        int centerX = this.width / 2;
        int fieldX = centerX - FIELD_WIDTH / 2;
        presetTags = sanitizePresets(ScreenshotMetadataConfig.get().tagPresets);
        hasPresets = !presetTags.isEmpty();

        int presetRows = hasPresets ? (int) Math.ceil(presetTags.size() / (double) PRESETS_PER_ROW) : 0;
        int presetsHeight = lineHeight + 4;
        if (hasPresets) {
            presetsHeight += (presetRows * PRESET_BUTTON_HEIGHT) + Math.max(0, presetRows - 1) * PRESET_ROW_GAP;
        } else {
            presetsHeight += lineHeight;
        }

        int contentHeight = FIELD_HEIGHT + 10 + presetsHeight + 10 + BUTTON_HEIGHT;
        int startY = (this.height - contentHeight) / 2;
        int fieldY = startY;

        tagField = new TextFieldWidget(
            this.textRenderer,
            fieldX,
            fieldY,
            FIELD_WIDTH,
            FIELD_HEIGHT,
            Text.translatable("screen.screenshotmetadata.tags.field")
        );
        tagField.setPlaceholder(Text.translatable("screen.screenshotmetadata.tags.placeholder"));
        tagField.setMaxLength(256);
        String pending = ScreenshotMetadataMod.getPendingTags();
        if (pending != null) {
            tagField.setText(pending);
        }
        this.addDrawableChild(tagField);
        this.setFocused(tagField);

        presetsLabelY = fieldY + FIELD_HEIGHT + 10;
        presetsStartY = presetsLabelY + lineHeight + 4;
        presetsHelpY = presetsStartY;

        if (hasPresets) {
            addPresetButtons(centerX, presetsStartY);
        }

        int buttonY = presetsStartY + (hasPresets
            ? presetRows * PRESET_BUTTON_HEIGHT + Math.max(0, presetRows - 1) * PRESET_ROW_GAP
            : lineHeight) + 10;
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.screenshotmetadata.tags.save").formatted(Formatting.GREEN),
                button -> {
                    ScreenshotMetadataMod.setPendingTags(tagField.getText());
                    closeScreen();
                })
            .dimensions(centerX - BUTTON_WIDTH - 6, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.screenshotmetadata.tags.clear").formatted(Formatting.YELLOW),
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
            Text.translatable("screen.screenshotmetadata.tags.subtitle").formatted(Formatting.GRAY),
            centerX, this.height / 2 - 50, 0xAAAAAA);

        if (presetsLabelY > 0) {
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.screenshotmetadata.tags.presets").formatted(Formatting.DARK_GRAY),
                centerX,
                presetsLabelY,
                0x888888
            );
        }

        if (!hasPresets && presetsHelpY > 0) {
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.screenshotmetadata.tags.presets.empty").formatted(Formatting.GRAY),
                centerX,
                presetsHelpY,
                0x9B9B9B
            );
        }

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

    private void addPresetButtons(int centerX, int startY) {
        int total = presetTags.size();
        int index = 0;
        int row = 0;
        while (index < total) {
            int rowCount = Math.min(PRESETS_PER_ROW, total - index);
            int rowWidth = rowCount * PRESET_BUTTON_WIDTH + (rowCount - 1) * PRESET_BUTTON_GAP;
            int rowX = centerX - rowWidth / 2;
            int rowY = startY + row * (PRESET_BUTTON_HEIGHT + PRESET_ROW_GAP);
            for (int i = 0; i < rowCount; i++) {
                String preset = presetTags.get(index);
                int x = rowX + i * (PRESET_BUTTON_WIDTH + PRESET_BUTTON_GAP);
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal(preset),
                        button -> appendPresetTag(preset))
                    .dimensions(x, rowY, PRESET_BUTTON_WIDTH, PRESET_BUTTON_HEIGHT)
                    .build());
                index++;
            }
            row++;
        }
    }

    private void appendPresetTag(String preset) {
        if (preset == null || preset.isBlank()) {
            return;
        }
        List<String> tags = parseTags(tagField.getText());
        boolean exists = tags.stream().anyMatch(existing -> existing.equalsIgnoreCase(preset));
        if (!exists) {
            tags.add(preset.trim());
        }
        tagField.setText(String.join(", ", tags));
        tagField.setCursorToEnd(false);
    }

    private static List<String> parseTags(String rawTags) {
        List<String> tags = new ArrayList<>();
        if (rawTags == null || rawTags.isBlank()) {
            return tags;
        }
        String[] parts = rawTags.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            boolean exists = tags.stream().anyMatch(existing -> existing.equalsIgnoreCase(trimmed));
            if (!exists) {
                tags.add(trimmed);
            }
        }
        return tags;
    }

    private static List<String> sanitizePresets(List<String> presets) {
        List<String> cleaned = new ArrayList<>();
        if (presets == null) {
            return cleaned;
        }
        for (String preset : presets) {
            if (preset == null) {
                continue;
            }
            String trimmed = preset.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            boolean exists = cleaned.stream().anyMatch(existing -> existing.equalsIgnoreCase(trimmed));
            if (!exists) {
                cleaned.add(trimmed);
            }
        }
        return cleaned;
    }
}
