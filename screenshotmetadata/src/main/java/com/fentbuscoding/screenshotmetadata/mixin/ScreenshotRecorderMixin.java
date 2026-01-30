package com.fentbuscoding.screenshotmetadata.mixin;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import com.fentbuscoding.screenshotmetadata.config.ScreenshotMetadataConfig;
import com.fentbuscoding.screenshotmetadata.metadata.JsonSidecarWriter;
import com.fentbuscoding.screenshotmetadata.metadata.PngMetadataWriter;
import com.fentbuscoding.screenshotmetadata.metadata.XmpSidecarWriter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Mixin to intercept screenshot saving and add comprehensive metadata.
 * Uses async processing to avoid blocking the game thread.
 */
@Mixin(ScreenshotRecorder.class)
public class ScreenshotRecorderMixin {

    private static final String SCREENSHOTS_DIR = "screenshots";
    
    @Inject(method = "saveScreenshot(Ljava/io/File;Lnet/minecraft/client/gl/Framebuffer;Ljava/util/function/Consumer;)V", 
            at = @At("TAIL"))
    private static void onScreenshotSaved(File gameDirectory, 
                                         net.minecraft.client.gl.Framebuffer framebuffer, 
                                         java.util.function.Consumer<net.minecraft.text.Text> messageReceiver, 
                                         CallbackInfo ci) {
        // Process metadata asynchronously to avoid blocking the game thread
        CompletableFuture.runAsync(() -> {
            try {
                processScreenshotMetadata(gameDirectory);
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.error("Unexpected error in screenshot metadata processing", e);
            }
        }, Util.getIoWorkerExecutor());
    }
    
    /**
     * Processes the screenshot metadata addition
     */
    private static void processScreenshotMetadata(File gameDirectory) {
        try {
            ScreenshotMetadataMod.LOGGER.debug("Processing screenshot metadata...");
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null) {
                ScreenshotMetadataMod.LOGGER.warn("Cannot add metadata: client or player is null");
                return;
            }
            if (gameDirectory == null) {
                ScreenshotMetadataMod.LOGGER.warn("Cannot add metadata: game directory is null");
                return;
            }
            
            // Find the most recent screenshot file
            File screenshotFile = findNewestScreenshot(gameDirectory);
            if (screenshotFile == null) {
                ScreenshotMetadataMod.LOGGER.warn("No screenshot file found to add metadata to");
                return;
            }
            
            // Collect comprehensive metadata
            Map<String, String> metadata = collectMetadata(client);
            if (metadata.isEmpty()) {
                ScreenshotMetadataMod.LOGGER.warn("No metadata collected");
                return;
            }
            
            // Add metadata using both methods
            addMetadataToScreenshot(screenshotFile, metadata);
            
            ScreenshotMetadataMod.LOGGER.info("Successfully added metadata to screenshot: {}", screenshotFile.getName());
            
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.error("Failed to process screenshot metadata", e);
        }
    }
    
    /**
     * Finds the newest screenshot file in the screenshots directory
     */
    private static File findNewestScreenshot(File gameDirectory) {
        File screenshotsDir = new File(gameDirectory, SCREENSHOTS_DIR);
        if (!screenshotsDir.exists() || !screenshotsDir.isDirectory()) {
            ScreenshotMetadataMod.LOGGER.warn("Screenshots directory not found: {}", screenshotsDir.getPath());
            return null;
        }
        
        File[] files = screenshotsDir.listFiles((dir, name) ->
            name.toLowerCase().endsWith(".png") && !name.startsWith("."));

        if (files == null || files.length == 0) {
            return null;
        }

        return Arrays.stream(files)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);
    }
    
    /**
     * Collects comprehensive metadata from the game state
     */
    private static Map<String, String> collectMetadata(MinecraftClient client) {
        Map<String, String> metadata = new HashMap<>();
        
        try {
            // Player information
            if (client.getSession() != null && client.getSession().getUsername() != null) {
                metadata.put("Username", client.getSession().getUsername());
            }
            if (client.player != null && client.player.getUuid() != null) {
                metadata.put("PlayerUuid", client.player.getUuid().toString());
            }
            
            // Player coordinates
            if (client.player != null) {
                metadata.put("X", String.valueOf((int) client.player.getX()));
                metadata.put("Y", String.valueOf((int) client.player.getY()));
                metadata.put("Z", String.valueOf((int) client.player.getZ()));
                metadata.put("Yaw", String.format("%.1f", client.player.getYaw()));
                metadata.put("Pitch", String.format("%.1f", client.player.getPitch()));
                metadata.put("Facing", getFacingDirection(client.player.getYaw()));
            }
            
            // World and biome information
            if (client.world != null && client.player != null) {
                String worldKey = client.world.getRegistryKey().getValue().toString();
                metadata.put("World", worldKey);
                metadata.put("DimensionId", worldKey);
                metadata.put("Dimension", formatDimensionName(worldKey));

                String biomeName = getBiomeName(client);
                if (biomeName != null && !biomeName.isEmpty()) {
                    metadata.put("Biome", biomeName);
                }
                String biomeId = getBiomeId(client);
                if (biomeId != null && !biomeId.isEmpty()) {
                    metadata.put("BiomeId", biomeId);
                }

                long timeOfDay = client.world.getTimeOfDay() % 24000L;
                metadata.put("TimeOfDayTicks", String.valueOf(timeOfDay));
                metadata.put("TimeOfDay", formatTimeOfDay(timeOfDay));
            }

            // Server / world info
            if (client.isInSingleplayer()) {
                if (client.getServer() != null && client.getServer().getSaveProperties() != null) {
                    metadata.put("WorldName", client.getServer().getSaveProperties().getLevelName());
                }
                if (ScreenshotMetadataConfig.get().includeWorldSeed
                    && client.getServer() != null && client.getServer().getOverworld() != null) {
                    metadata.put("WorldSeed", String.valueOf(client.getServer().getOverworld().getSeed()));
                }
                metadata.put("ServerType", "Singleplayer");
            } else if (client.getCurrentServerEntry() != null) {
                metadata.put("ServerType", "Multiplayer");
                metadata.put("ServerName", client.getCurrentServerEntry().name);
                metadata.put("ServerAddress", client.getCurrentServerEntry().address);
            }
            
            // Timestamp
            metadata.put("Timestamp", Instant.now().toString());
                metadata.put("LocalTime", OffsetDateTime.now(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            
            // Game version info
            metadata.put("MinecraftVersion", client.getGameVersion());
            metadata.put("ModVersion", ScreenshotMetadataMod.MOD_VERSION);
            metadata.put("ModId", ScreenshotMetadataMod.MOD_ID);
            
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.error("Error collecting metadata", e);
        }
        
        return metadata;
    }
    
    /**
     * Extracts and cleans up the biome name for better readability
     */
    private static String getBiomeName(MinecraftClient client) {
        try {
            RegistryEntry<Biome> biomeEntry = client.world.getBiome(client.player.getBlockPos());
            return biomeEntry.getKey()
                    .map(key -> formatBiomeName(key.getValue().getPath()))
                    .orElse("Unknown");
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.debug("Could not extract biome name", e);
            return "Unknown";
        }
    }

    /**
     * Extracts the biome registry id
     */
    private static String getBiomeId(MinecraftClient client) {
        try {
            RegistryEntry<Biome> biomeEntry = client.world.getBiome(client.player.getBlockPos());
            return biomeEntry.getKey()
                    .map(key -> key.getValue().toString())
                    .orElse("Unknown");
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.debug("Could not extract biome id", e);
            return "Unknown";
        }
    }
    
    /**
     * Formats biome name from snake_case to Title Case
     */
    private static String formatBiomeName(String biomeName) {
        if (biomeName == null || biomeName.isEmpty()) {
            return "Unknown";
        }
        
        String[] words = biomeName.replace("_", " ").split(" ");
        StringBuilder titleCase = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                titleCase.append(Character.toUpperCase(word.charAt(0)))
                         .append(word.substring(1).toLowerCase())
                         .append(" ");
            }
        }
        
        return titleCase.toString().trim();
    }

    /**
     * Formats dimension id to a friendly name
     */
    private static String formatDimensionName(String dimensionId) {
        if (dimensionId == null || dimensionId.isEmpty()) {
            return "Unknown";
        }
        switch (dimensionId) {
            case "minecraft:overworld":
                return "Overworld";
            case "minecraft:the_nether":
                return "Nether";
            case "minecraft:the_end":
                return "The End";
            default:
                String path = dimensionId.contains(":") ? dimensionId.split(":", 2)[1] : dimensionId;
                return formatBiomeName(path);
        }
    }

    /**
     * Converts in-game time (0-23999) to 24h time
     */
    private static String formatTimeOfDay(long timeOfDay) {
        int hours = (int) ((timeOfDay / 1000 + 6) % 24);
        int minutes = (int) ((timeOfDay % 1000) * 60 / 1000);
        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     * Converts yaw to a readable facing direction
     */
    private static String getFacingDirection(float yaw) {
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
     * Adds metadata to the screenshot using both PNG and XMP methods
     */
    private static void addMetadataToScreenshot(File screenshotFile, Map<String, String> metadata) {
        ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
        // Add PNG embedded metadata
        if (config.writePngMetadata) {
            try {
                PngMetadataWriter.writeMetadata(screenshotFile, metadata);
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.error("Failed to write PNG metadata to {}", screenshotFile.getName(), e);
            }
        }
        
        // Create XMP sidecar file
        if (config.writeXmpSidecar) {
            try {
                XmpSidecarWriter.writeSidecarFile(screenshotFile, metadata);
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.error("Failed to create XMP sidecar for {}", screenshotFile.getName(), e);
            }
        }

        // Create JSON sidecar file for easy parsing
        if (config.writeJsonSidecar) {
            try {
                JsonSidecarWriter.writeSidecarFile(screenshotFile, metadata);
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.error("Failed to create JSON sidecar for {}", screenshotFile.getName(), e);
            }
        }
    }
}
