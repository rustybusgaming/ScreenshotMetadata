package com.fentbuscoding.screenshotmetadata.config;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScreenshotMetadataConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "screenshotmetadata.json";
    private static final String SCHEMA_FIELD = "configSchemaVersion";
    private static final int CURRENT_CONFIG_SCHEMA_VERSION = 1;

    private static ScreenshotMetadataConfig instance;
    private static boolean loaded;

    public boolean writePngMetadata = true;
    public boolean writeEmbeddedXmp = true;
    public boolean writeXmpSidecar = true;
    public boolean writeJsonSidecar = true;
    public boolean includeWorldSeed = true;
    public boolean privacyMode = false;
    public boolean renameScreenshots = false;
    public String screenshotNameTemplate = "{date}_{dimension}_X{x}_Z{z}";

    // Metadata filtering options
    public boolean includePerformanceMetrics = true;
    public boolean includePlayerStatus = true;
    public boolean includeEquipment = true;
    public boolean includePotionEffects = true;
    public boolean includeCoordinates = true;
    public boolean includeBiomeInfo = true;
    public boolean includeWeatherInfo = true;
    public boolean includeModpackContext = true;
    public String metadataProfile = MetadataProfile.FULL.id;
    public int configSchemaVersion = CURRENT_CONFIG_SCHEMA_VERSION;

    public static ScreenshotMetadataConfig get() {
        if (instance == null || !loaded) {
            load();
        }
        if (instance != null) {
            instance.normalize();
        }
        return instance;
    }

    public static void load() {
        Path configPath = getConfigPath();
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                JsonObject root = parsed != null && parsed.isJsonObject()
                    ? parsed.getAsJsonObject()
                    : new JsonObject();
                int loadedSchemaVersion = readSchemaVersion(root);

                instance = GSON.fromJson(root, ScreenshotMetadataConfig.class);
                if (instance == null) {
                    instance = new ScreenshotMetadataConfig();
                }
                boolean migrated = instance.migrate(loadedSchemaVersion);
                instance.normalize();
                loaded = true;
                if (migrated) {
                    save();
                }
            } catch (IOException | JsonSyntaxException e) {
                ScreenshotMetadataMod.LOGGER.warn("Failed to read config, using defaults: {}", e.getMessage());
                instance = new ScreenshotMetadataConfig();
                instance.normalize();
                loaded = true;
            }
        } else {
            instance = new ScreenshotMetadataConfig();
            instance.normalize();
            loaded = true;
            save();
        }
    }

    public static void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            ScreenshotMetadataMod.LOGGER.warn("Failed to create config directory: {}", e.getMessage());
        }
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(get(), writer);
        } catch (IOException e) {
            ScreenshotMetadataMod.LOGGER.warn("Failed to write config: {}", e.getMessage());
        }
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public MetadataProfile getMetadataProfile() {
        return MetadataProfile.fromId(metadataProfile);
    }

    public void setMetadataProfile(MetadataProfile profile) {
        metadataProfile = profile == null ? MetadataProfile.FULL.id : profile.id;
    }

    public void applyProfile(MetadataProfile profile) {
        MetadataProfile resolved = profile == null ? MetadataProfile.FULL : profile;
        switch (resolved) {
            case FULL -> {
                includeWorldSeed = true;
                includePerformanceMetrics = true;
                includePlayerStatus = true;
                includeEquipment = true;
                includePotionEffects = true;
                includeCoordinates = true;
                includeBiomeInfo = true;
                includeWeatherInfo = true;
                includeModpackContext = true;
                privacyMode = false;
            }
            case LIGHTWEIGHT -> {
                includeWorldSeed = false;
                includePerformanceMetrics = false;
                includePlayerStatus = false;
                includeEquipment = false;
                includePotionEffects = false;
                includeCoordinates = true;
                includeBiomeInfo = true;
                includeWeatherInfo = false;
                includeModpackContext = false;
                privacyMode = false;
            }
            case PRIVACY -> {
                includeWorldSeed = true;
                includePerformanceMetrics = true;
                includePlayerStatus = true;
                includeEquipment = true;
                includePotionEffects = true;
                includeCoordinates = true;
                includeBiomeInfo = true;
                includeWeatherInfo = true;
                includeModpackContext = false;
                privacyMode = true;
            }
        }
        metadataProfile = resolved.id;
    }

    private boolean migrate(int loadedSchemaVersion) {
        boolean migrated = false;

        if (loadedSchemaVersion < 1) {
            metadataProfile = inferProfileIdFromCurrentSettings();
            migrated = true;
        }

        if (configSchemaVersion != CURRENT_CONFIG_SCHEMA_VERSION) {
            configSchemaVersion = CURRENT_CONFIG_SCHEMA_VERSION;
            migrated = true;
        }

        return migrated;
    }

    private void normalize() {
        if (metadataProfile == null || metadataProfile.isBlank()) {
            metadataProfile = MetadataProfile.FULL.id;
        } else {
            MetadataProfile profile = MetadataProfile.fromId(metadataProfile);
            if (profile == MetadataProfile.FULL && !"full".equalsIgnoreCase(metadataProfile.trim())) {
                metadataProfile = MetadataProfile.FULL.id;
            }
        }

        if (configSchemaVersion <= 0 || configSchemaVersion > CURRENT_CONFIG_SCHEMA_VERSION) {
            configSchemaVersion = CURRENT_CONFIG_SCHEMA_VERSION;
        }
    }

    private String inferProfileIdFromCurrentSettings() {
        if (matchesProfile(MetadataProfile.FULL)) {
            return MetadataProfile.FULL.id;
        }
        if (matchesProfile(MetadataProfile.LIGHTWEIGHT)) {
            return MetadataProfile.LIGHTWEIGHT.id;
        }
        if (matchesProfile(MetadataProfile.PRIVACY)) {
            return MetadataProfile.PRIVACY.id;
        }
        return MetadataProfile.CUSTOM.id;
    }

    private boolean matchesProfile(MetadataProfile profile) {
        return switch (profile) {
            case FULL -> includeWorldSeed
                && includePerformanceMetrics
                && includePlayerStatus
                && includeEquipment
                && includePotionEffects
                && includeCoordinates
                && includeBiomeInfo
                && includeWeatherInfo
                && includeModpackContext
                && !privacyMode;
            case LIGHTWEIGHT -> !includeWorldSeed
                && !includePerformanceMetrics
                && !includePlayerStatus
                && !includeEquipment
                && !includePotionEffects
                && includeCoordinates
                && includeBiomeInfo
                && !includeWeatherInfo
                && !includeModpackContext
                && !privacyMode;
            case PRIVACY -> includeWorldSeed
                && includePerformanceMetrics
                && includePlayerStatus
                && includeEquipment
                && includePotionEffects
                && includeCoordinates
                && includeBiomeInfo
                && includeWeatherInfo
                && !includeModpackContext
                && privacyMode;
            case CUSTOM -> false;
        };
    }

    private static int readSchemaVersion(JsonObject root) {
        if (root == null || !root.has(SCHEMA_FIELD)) {
            return 0;
        }
        try {
            return root.get(SCHEMA_FIELD).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }

    public enum MetadataProfile {
        FULL("full"),
        LIGHTWEIGHT("lightweight"),
        PRIVACY("privacy"),
        CUSTOM("custom");

        public final String id;

        MetadataProfile(String id) {
            this.id = id;
        }

        public static MetadataProfile fromId(String raw) {
            if (raw == null || raw.isBlank()) {
                return FULL;
            }
            for (MetadataProfile profile : values()) {
                if (profile.id.equalsIgnoreCase(raw.trim())) {
                    return profile;
                }
            }
            return FULL;
        }
    }
}
