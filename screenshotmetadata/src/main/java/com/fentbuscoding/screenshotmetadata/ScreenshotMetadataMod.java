package com.fentbuscoding.screenshotmetadata;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Screenshot Metadata Mod
 * Automatically adds comprehensive metadata to Minecraft screenshots including:
 * - Player information (username)
 * - World data (dimension, biome)
 * - Player coordinates (X, Y, Z)
 * - Timestamp
 * 
 * Metadata is stored in two formats:
 * - PNG text chunks (embedded in image)
 * - XMP sidecar files (for File Explorer visibility)
 * 
 * @author fentbuscoding
 * @version 1.0.0
 */
public class ScreenshotMetadataMod implements ClientModInitializer {
    public static final String MOD_ID = "screenshotmetadata";
    public static final String MOD_NAME = "Screenshot Metadata";
    public static final String MOD_VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("{} v{} initialized! Screenshots will now include comprehensive metadata.", MOD_NAME, MOD_VERSION);
        LOGGER.info("Metadata includes: Player info, coordinates, world data, biome, and timestamp");
        LOGGER.info("Stored as both PNG text chunks and XMP sidecar files for maximum compatibility");
    }
}
