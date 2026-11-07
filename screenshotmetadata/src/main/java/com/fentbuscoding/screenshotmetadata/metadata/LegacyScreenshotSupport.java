package com.fentbuscoding.screenshotmetadata.metadata;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import com.fentbuscoding.screenshotmetadata.config.ScreenshotMetadataConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The game-independent half of the Babric and Better than Adventure builds.
 *
 * <p>Their mixins know exactly which file the game just wrote and read the game
 * state on the render thread; everything after that (renaming, the mod list and the
 * PNG/XMP/JSON writers) happens here, off the render thread.
 */
public final class LegacyScreenshotSupport {
    private static final int MAX_MOD_LIST_ENTRIES = 200;

    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ScreenshotMetadata-Writer");
        thread.setDaemon(true);
        return thread;
    });

    private LegacyScreenshotSupport() {
    }

    /**
     * Adds the fields every build records regardless of the game state.
     */
    public static void putCommonMetadata(Map<String, String> metadata, String minecraftVersion) {
        metadata.put("Timestamp", Instant.now().toString());
        metadata.put("LocalTime", OffsetDateTime.now(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        if (minecraftVersion != null && !minecraftVersion.isBlank()) {
            metadata.put("MinecraftVersion", minecraftVersion);
        }
        metadata.put("ModVersion", ScreenshotMetadataMod.MOD_VERSION);
        metadata.put("ModId", ScreenshotMetadataMod.MOD_ID);
    }

    /**
     * Writes the metadata for a screenshot the game has already finished saving.
     */
    public static void processAsync(File screenshotFile, Map<String, String> metadata, List<String> resourcePacks) {
        if (screenshotFile == null || metadata == null || metadata.isEmpty()) {
            return;
        }
        WRITER.execute(() -> {
            try {
                File target = maybeRenameScreenshot(screenshotFile, metadata);
                writeAll(target, metadata, collectJsonSidecarContext(resourcePacks));
                ScreenshotMetadataMod.LOGGER.info("Successfully added metadata to screenshot: {}", target.getName());
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.error("Failed to process screenshot metadata", e);
            }
        });
    }

    private static void writeAll(File screenshotFile, Map<String, String> metadata, JsonSidecarContext context) {
        ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
        if (config.writePngMetadata) {
            try {
                PngMetadataWriter.writeMetadata(screenshotFile, metadata);
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.error("Failed to write PNG metadata to {}", screenshotFile.getName(), e);
            }
        }
        if (config.writeEmbeddedXmp) {
            try {
                PngXmpWriter.writeMetadata(screenshotFile, metadata);
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.error("Failed to write embedded XMP to {}", screenshotFile.getName(), e);
            }
        }
        if (config.writeXmpSidecar) {
            try {
                XmpSidecarWriter.writeSidecarFile(screenshotFile, metadata);
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.error("Failed to create XMP sidecar for {}", screenshotFile.getName(), e);
            }
        }
        if (config.writeJsonSidecar) {
            try {
                JsonSidecarWriter.writeSidecarFile(screenshotFile, metadata, context);
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.error("Failed to create JSON sidecar for {}", screenshotFile.getName(), e);
            }
        }
    }

    private static JsonSidecarContext collectJsonSidecarContext(List<String> resourcePacks) {
        ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
        if (!config.writeJsonSidecar || !config.includeModpackContext) {
            return null;
        }

        List<String> modEntries = new ArrayList<>();
        int modCount = -1;
        boolean truncated = false;
        try {
            List<ModContainer> mods = new ArrayList<>(FabricLoader.getInstance().getAllMods());
            mods.sort(Comparator.comparing(mod -> mod.getMetadata().getId()));
            modCount = mods.size();
            for (ModContainer mod : mods) {
                if (modEntries.size() >= MAX_MOD_LIST_ENTRIES) {
                    truncated = true;
                    break;
                }
                modEntries.add(mod.getMetadata().getId() + "@" + mod.getMetadata().getVersion().getFriendlyString());
            }
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.debug("Could not collect mod list", e);
        }

        // Neither Beta 1.7.3 nor BTA has a shader pack loader to ask.
        return new JsonSidecarContext(resourcePacks, "None", modEntries, modCount, truncated);
    }

    public static int roundToNearest(int value, int step) {
        if (step <= 0) {
            return value;
        }
        return Math.round(value / (float) step) * step;
    }

    public static String hashSeed(long seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Long.toString(seed).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.debug("Could not hash world seed", e);
            return "unknown";
        }
    }

    /**
     * Converts yaw to a compass direction. Beta 1.7.3 and BTA use the same yaw
     * convention as modern Minecraft: 0 faces south.
     */
    public static String getFacingDirection(float yaw) {
        int index = Math.floorMod(Math.round(yaw / 45f), 8);
        return switch (index) {
            case 0 -> "South";
            case 1 -> "Southwest";
            case 2 -> "West";
            case 3 -> "Northwest";
            case 4 -> "North";
            case 5 -> "Northeast";
            case 6 -> "East";
            case 7 -> "Southeast";
            default -> "Unknown";
        };
    }

    /**
     * Converts in-game time (0-23999) to 24h time; tick 0 is 06:00.
     */
    public static String formatTimeOfDay(long timeOfDay) {
        long ticks = Math.floorMod(timeOfDay, 24000L);
        int hours = (int) ((ticks / 1000 + 6) % 24);
        int minutes = (int) ((ticks % 1000) * 60 / 1000);
        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     * Turns ids such as {@code snowy_forest} or {@code SNOWY-FOREST} into "Snowy Forest".
     */
    public static String toTitleCase(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] words = value.trim().replace('-', '_').replace(' ', '_').toLowerCase().split("_+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.length() == 0 ? null : result.toString();
    }

    public static void putIfPresent(Map<String, String> metadata, String key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.put(key, value.trim());
        }
    }

    private static File maybeRenameScreenshot(File screenshotFile, Map<String, String> metadata) {
        ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
        if (!config.renameScreenshots) {
            return screenshotFile;
        }
        String template = config.screenshotNameTemplate;
        if (template == null || template.isBlank()) {
            return screenshotFile;
        }

        String baseName = sanitizeFileName(applyTemplate(template, metadata));
        if (baseName.isBlank()) {
            return screenshotFile;
        }

        File parent = screenshotFile.getParentFile();
        File target = new File(parent, baseName + ".png");
        if (target.equals(screenshotFile)) {
            return screenshotFile;
        }
        int suffix = 1;
        while (target.exists()) {
            target = new File(parent, baseName + "_" + suffix + ".png");
            suffix++;
        }

        try {
            Files.move(screenshotFile.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
            return target;
        } catch (Exception atomicFailure) {
            try {
                Files.move(screenshotFile.toPath(), target.toPath());
                return target;
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.warn("Failed to rename screenshot {} to {}: {}",
                    screenshotFile.getName(), target.getName(), e.getMessage());
                return screenshotFile;
            }
        }
    }

    private static String applyTemplate(String template, Map<String, String> metadata) {
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"));
        return template
            .replace("{datetime}", date + "_" + time)
            .replace("{date}", date)
            .replace("{time}", time)
            .replace("{dimension}", metadata.getOrDefault("Dimension", "Unknown"))
            .replace("{biome}", metadata.getOrDefault("Biome", "Unknown"))
            .replace("{x}", metadata.getOrDefault("X", "NA"))
            .replace("{y}", metadata.getOrDefault("Y", "NA"))
            .replace("{z}", metadata.getOrDefault("Z", "NA"))
            .replace("{world}", metadata.getOrDefault("WorldName", "World"))
            .replace("{player}", metadata.getOrDefault("Username", "Player"));
    }

    private static String sanitizeFileName(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
