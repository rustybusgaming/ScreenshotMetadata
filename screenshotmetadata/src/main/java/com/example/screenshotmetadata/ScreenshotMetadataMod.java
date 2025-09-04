package com.example.screenshotmetadata;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screenshots.v1.ScreenshotCallback;
import java.nio.file.Path;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ScreenshotMetadataMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register screenshot event to embed metadata whenever a screenshot is saved
    ScreenshotCallback.EVENT.register((MinecraftClient client, Path path, NativeImage image) -> {
            Map<String, String> metadata = buildMetadata(client);
            try {
                PngMetadataWriter.writeMetadata(path.toFile(), metadata);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // Helper: build metadata map from client state
    private Map<String, String> buildMetadata(MinecraftClient client) {
        Map<String, String> metadata = new HashMap<>();
        if (client.player != null) {
            metadata.put("Username", client.getSession().getUsername());
            metadata.put("X", String.valueOf(client.player.getX()));
            metadata.put("Y", String.valueOf(client.player.getY()));
            metadata.put("Z", String.valueOf(client.player.getZ()));
        }
        if (client.world != null && client.player != null) {
            metadata.put("World", client.world.getRegistryKey().getValue().toString());
            metadata.put("Biome", client.world.getBiome(client.player.getBlockPos()).toString());
        }
        metadata.put("Timestamp", Instant.now().toString());
        return metadata;
    }
}
