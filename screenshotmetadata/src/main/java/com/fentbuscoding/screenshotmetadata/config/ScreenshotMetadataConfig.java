package com.fentbuscoding.screenshotmetadata.config;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private static ScreenshotMetadataConfig instance;

    public boolean writePngMetadata = true;
    public boolean writeXmpSidecar = true;
    public boolean writeJsonSidecar = true;
    public boolean includeWorldSeed = true;

    public static ScreenshotMetadataConfig get() {
        if (instance == null) {
            instance = new ScreenshotMetadataConfig();
        }
        return instance;
    }

    public static void load() {
        Path configPath = getConfigPath();
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                instance = GSON.fromJson(reader, ScreenshotMetadataConfig.class);
                if (instance == null) {
                    instance = new ScreenshotMetadataConfig();
                }
            } catch (IOException | JsonSyntaxException e) {
                ScreenshotMetadataMod.LOGGER.warn("Failed to read config, using defaults: {}", e.getMessage());
                instance = new ScreenshotMetadataConfig();
            }
        } else {
            instance = new ScreenshotMetadataConfig();
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
}
