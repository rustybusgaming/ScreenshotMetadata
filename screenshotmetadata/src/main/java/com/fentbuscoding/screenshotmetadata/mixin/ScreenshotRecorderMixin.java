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
    private static final ThreadLocal<File> LAST_SCREENSHOT_FILE = new ThreadLocal<>();
    
            @Inject(method = "saveScreenshot(Ljava/io/File;Lnet/minecraft/client/gl/Framebuffer;Ljava/util/function/Consumer;)V", 
                at = @At("HEAD"), require = 0)
    private static void captureScreenshotFile(File gameDirectory, 
                                             net.minecraft.client.gl.Framebuffer framebuffer, 
                                             java.util.function.Consumer<net.minecraft.text.Text> messageReceiver, 
                                             CallbackInfo ci) {
        try {
            File screenshotsDir = new File(gameDirectory, SCREENSHOTS_DIR);
            File[] existingFiles = screenshotsDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".png") && !name.startsWith("."));
            
            if (existingFiles != null && existingFiles.length > 0) {
                File newest = Arrays.stream(existingFiles)
                    .max(Comparator.comparing(File::getName)
                        .thenComparingLong(File::lastModified))
                    .orElse(null);
                LAST_SCREENSHOT_FILE.set(newest);
            }
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.debug("Could not capture pre-save state: {}", e.getMessage());
        }
    }
    
        @Inject(method = "saveScreenshot(Ljava/io/File;Lnet/minecraft/client/gl/Framebuffer;Ljava/util/function/Consumer;)V", 
            at = @At("TAIL"), require = 0)
    private static void onScreenshotSaved(File gameDirectory, 
                                         net.minecraft.client.gl.Framebuffer framebuffer, 
                                         java.util.function.Consumer<net.minecraft.text.Text> messageReceiver, 
                                         CallbackInfo ci) {
        File preSaveNewest = LAST_SCREENSHOT_FILE.get();
        LAST_SCREENSHOT_FILE.remove();
        
        // Process metadata asynchronously to avoid blocking the game thread
        CompletableFuture.runAsync(() -> {
            try {
                processScreenshotMetadata(gameDirectory, preSaveNewest);
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.error("Unexpected error in screenshot metadata processing", e);
            }
        }, Util.getIoWorkerExecutor());
    }
    
    /**
     * Processes the screenshot metadata addition
     */
    private static void processScreenshotMetadata(File gameDirectory, File preSaveNewest) {
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
            
            // Find the most recent screenshot file (wait briefly for file to finish writing)
            File screenshotFile = waitForNewestScreenshot(gameDirectory, preSaveNewest);
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
    private static File findNewestScreenshot(File gameDirectory, File preSaveNewest) {
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

        // Filter to only files newer than the pre-save newest file
        if (preSaveNewest != null) {
            final String preSaveName = preSaveNewest.getName();
            files = Arrays.stream(files)
                .filter(f -> f.getName().compareTo(preSaveName) > 0 || 
                            (f.getName().equals(preSaveName) && f.lastModified() > preSaveNewest.lastModified()))
                .toArray(File[]::new);
            
            if (files.length == 0) {
                ScreenshotMetadataMod.LOGGER.debug("No new files found after: {}", preSaveName);
                return null;
            }
        }

        return Arrays.stream(files)
            .max(Comparator.comparing(File::getName)
                .thenComparingLong(File::lastModified))
            .orElse(null);
    }

    /**
     * Waits briefly for the newest screenshot file to appear and finish writing.
     * Uses exponential backoff retry strategy.
     */
    private static File waitForNewestScreenshot(File gameDirectory, File preSaveNewest) {
        final int maxAttempts = 15;
        final long initialSleepMillis = 100L;
        final double backoffMultiplier = 1.5;
        final long maxSleepMillis = 1000L;

        File candidate = null;
        long sleepMillis = initialSleepMillis;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            candidate = findNewestScreenshot(gameDirectory, preSaveNewest);
            if (candidate != null && isFileStable(candidate)) {
                ScreenshotMetadataMod.LOGGER.debug("Found screenshot file on attempt {}", attempt + 1);
                return candidate;
            }
            
            try {
                Thread.sleep(sleepMillis);
                sleepMillis = Math.min((long)(sleepMillis * backoffMultiplier), maxSleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Fallback: Check common locations if screenshot not found in screenshots dir
        if (candidate == null) {
            candidate = findScreenshotInFallbackLocations(gameDirectory, preSaveNewest);
        }

        return candidate;
    }

    /**
     * Attempts to find screenshot in common fallback locations if primary method fails.
     */
    private static File findScreenshotInFallbackLocations(File gameDirectory, File preSaveNewest) {
        File[] fallbackDirs = {
            gameDirectory,  // Game directory root
            new File(System.getProperty("user.home"), "Downloads"),  // Downloads folder
            new File(System.getProperty("java.io.tmpdir"))  // System temp directory
        };

        long preSaveTime = preSaveNewest != null ? preSaveNewest.lastModified() : System.currentTimeMillis() - 5000;

        for (File fallbackDir : fallbackDirs) {
            if (!fallbackDir.exists() || !fallbackDir.isDirectory()) {
                continue;
            }

            File[] pngFiles = fallbackDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".png") && 
                !name.startsWith(".") && 
                new File(dir, name).lastModified() > preSaveTime);

            if (pngFiles != null && pngFiles.length > 0) {
                File newest = Arrays.stream(pngFiles)
                    .max(Comparator.comparingLong(File::lastModified))
                    .orElse(null);
                
                if (newest != null && isFileStable(newest)) {
                    ScreenshotMetadataMod.LOGGER.debug("Found screenshot in fallback location: {}", newest.getAbsolutePath());
                    return newest;
                }
            }
        }

        return null;
    }

    private static boolean isFileStable(File file) {
        try {
            if (!file.exists()) {
                return false;
            }
            long size1 = file.length();
            long time1 = file.lastModified();
            if (size1 <= 0L) {
                return false;
            }
            Thread.sleep(100L);
            long size2 = file.length();
            long time2 = file.lastModified();
            return size1 == size2 && time1 == time2;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Collects comprehensive metadata from the game state
     */
    private static Map<String, String> collectMetadata(MinecraftClient client) {
        Map<String, String> metadata = new HashMap<>();
        ScreenshotMetadataConfig config = ScreenshotMetadataConfig.get();
        
        try {
            // Player information
            if (client.getSession() != null && client.getSession().getUsername() != null) {
                metadata.put("Username", client.getSession().getUsername());
            }
            if (client.player != null && client.player.getUuid() != null) {
                metadata.put("PlayerUuid", client.player.getUuid().toString());
            }
            
            // Player coordinates
            if (config.includeCoordinates && client.player != null) {
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

                if (config.includeBiomeInfo) {
                    String biomeName = getBiomeName(client);
                    if (biomeName != null && !biomeName.isEmpty()) {
                        metadata.put("Biome", biomeName);
                    }
                    String biomeId = getBiomeId(client);
                    if (biomeId != null && !biomeId.isEmpty()) {
                        metadata.put("BiomeId", biomeId);
                    }
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
                if (config.includeWorldSeed
                    && client.getServer() != null && client.getServer().getOverworld() != null) {
                    metadata.put("WorldSeed", String.valueOf(client.getServer().getOverworld().getSeed()));
                }
                metadata.put("ServerType", "Singleplayer");
            } else if (client.getCurrentServerEntry() != null) {
                metadata.put("ServerType", "Multiplayer");
                metadata.put("ServerName", client.getCurrentServerEntry().name);
                String serverAddress = client.getCurrentServerEntry().address;
                if (serverAddress != null && !serverAddress.toLowerCase().contains("realms")) {
                    metadata.put("ServerAddress", serverAddress);
                }
            }
            
            // Timestamp
            metadata.put("Timestamp", Instant.now().toString());
                metadata.put("LocalTime", OffsetDateTime.now(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            
            // Game version info
            metadata.put("MinecraftVersion", client.getGameVersion());
            metadata.put("ModVersion", ScreenshotMetadataMod.MOD_VERSION);
            metadata.put("ModId", ScreenshotMetadataMod.MOD_ID);

            // Player Status - Game Mode
            if (config.includePlayerStatus && client.player != null && client.world != null) {
                try {
                    // Try to get difficulty as proxy for game mode info
                    metadata.put("Difficulty", client.world.getDifficulty().getName());
                } catch (Exception e) {
                    ScreenshotMetadataMod.LOGGER.debug("Could not get difficulty", e);
                }
            }

            // Player Health and Hunger
            if (config.includePlayerStatus && client.player != null) {
                metadata.put("Health", String.format("%.1f", client.player.getHealth()));
                metadata.put("MaxHealth", String.format("%.1f", client.player.getMaxHealth()));
                metadata.put("HungerLevel", String.valueOf(client.player.getHungerManager().getFoodLevel()));
                metadata.put("Saturation", String.format("%.1f", client.player.getHungerManager().getSaturationLevel()));
            }

            // Performance Metrics
            if (config.includePerformanceMetrics) {
                // Record current time for FPS calculation (approximate)
                long currentTime = System.currentTimeMillis();
                metadata.put("CaptureTimeMs", String.valueOf(currentTime));
                
                if (client.options != null) {
                    metadata.put("RenderDistance", String.valueOf(client.options.getViewDistance().getValue()));
                    if (client.options.getSimulationDistance() != null) {
                        metadata.put("SimulationDistance", String.valueOf(client.options.getSimulationDistance().getValue()));
                    }
                }
            }

            // Equipped Items
            if (config.includeEquipment && client.player != null) {
                net.minecraft.item.ItemStack mainHand = client.player.getMainHandStack();
                if (mainHand != null && !mainHand.isEmpty()) {
                    metadata.put("MainHandItem", mainHand.getItem().getName().getString());
                    metadata.put("MainHandCount", String.valueOf(mainHand.getCount()));
                }

                net.minecraft.item.ItemStack offHand = client.player.getOffHandStack();
                if (offHand != null && !offHand.isEmpty()) {
                    metadata.put("OffHandItem", offHand.getItem().getName().getString());
                    metadata.put("OffHandCount", String.valueOf(offHand.getCount()));
                }
            }

            // Armor and Equipment
            if (config.includeEquipment && client.player != null) {
                addArmorMetadata(client.player, metadata);
            }

            // Active Potion Effects
            if (config.includePotionEffects && client.player != null) {
                addPotionEffectsMetadata(client.player, metadata);
            }
            
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.error("Error collecting metadata", e);
        }
        
        return metadata;
    }

    /**
     * Adds armor and equipment details to metadata
     */
    private static void addArmorMetadata(net.minecraft.entity.player.PlayerEntity player, Map<String, String> metadata) {
        try {
            net.minecraft.entity.EquipmentSlot[] armorSlots = {
                net.minecraft.entity.EquipmentSlot.HEAD,
                net.minecraft.entity.EquipmentSlot.CHEST,
                net.minecraft.entity.EquipmentSlot.LEGS,
                net.minecraft.entity.EquipmentSlot.FEET
            };

            String[] armorNames = {"Head", "Chest", "Legs", "Feet"};

            for (int i = 0; i < armorSlots.length; i++) {
                net.minecraft.item.ItemStack armor = player.getEquippedStack(armorSlots[i]);
                if (armor != null && !armor.isEmpty()) {
                    metadata.put("Armor" + armorNames[i], armor.getItem().getName().getString());
                }
            }
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.debug("Could not collect armor metadata", e);
        }
    }

    /**
     * Adds active potion effects to metadata
     */
    private static void addPotionEffectsMetadata(net.minecraft.entity.player.PlayerEntity player, Map<String, String> metadata) {
        try {
            java.util.Collection<net.minecraft.entity.effect.StatusEffectInstance> effects = player.getStatusEffects();
            
            if (effects.isEmpty()) {
                metadata.put("PotionEffects", "None");
                return;
            }

            StringBuilder effectsBuilder = new StringBuilder();
            int effectCount = 0;

            for (net.minecraft.entity.effect.StatusEffectInstance effect : effects) {
                if (effectCount > 0) {
                    effectsBuilder.append(", ");
                }

                String effectName = effect.getEffectType().value().getName().getString();
                int amplifier = effect.getAmplifier();
                int duration = effect.getDuration();

                effectsBuilder.append(effectName);
                if (amplifier > 0) {
                    effectsBuilder.append(" ").append(amplifier + 1);
                }
                effectsBuilder.append(" (").append(duration).append("t)");
                effectCount++;
            }

            if (effectCount > 0) {
                metadata.put("PotionEffects", effectsBuilder.toString());
                metadata.put("PotionEffectCount", String.valueOf(effectCount));
            }
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.debug("Could not collect potion effects metadata", e);
        }
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
            if (!writePngMetadataWithRetry(screenshotFile, metadata)) {
                ScreenshotMetadataMod.LOGGER.error("Failed to write PNG metadata to {}", screenshotFile.getName());
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

    private static boolean writePngMetadataWithRetry(File screenshotFile, Map<String, String> metadata) {
        final int maxAttempts = 3;
        final long sleepMillis = 200L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                PngMetadataWriter.writeMetadata(screenshotFile, metadata);
                return true;
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.debug("PNG metadata write attempt {} failed for {}: {}",
                    attempt, screenshotFile.getName(), e.getMessage());
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }
}
