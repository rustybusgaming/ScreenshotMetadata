package com.fentbuscoding.screenshotmetadata;

import com.fentbuscoding.screenshotmetadata.platform.Platform;
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
 * Metadata is stored as PNG text chunks, embedded XMP and XMP/JSON sidecar files.
 *
 * <p>This class is loader-neutral; each loader's entrypoint calls {@link #init()}.
 *
 * @author fentbuscoding
 */
public final class ScreenshotMetadataMod {
    public static final String MOD_ID = "screenshotmetadata";
    public static final String MOD_NAME = "Screenshot Metadata";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    public static final String MOD_VERSION = resolveVersion();

    private ScreenshotMetadataMod() {
    }

    public static void init() {
        LOGGER.info("{} v{} initialized on {}.", MOD_NAME, MOD_VERSION, Platform.get().getLoaderName());
    }

    private static String resolveVersion() {
        try {
            String version = Platform.get().getModVersion(MOD_ID);
            return version != null ? version : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
