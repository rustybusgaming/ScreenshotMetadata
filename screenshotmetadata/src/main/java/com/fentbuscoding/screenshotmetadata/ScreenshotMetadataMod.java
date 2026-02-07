package com.fentbuscoding.screenshotmetadata;

import com.fentbuscoding.screenshotmetadata.ui.TagInputScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

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
    private static final AtomicReference<String> PENDING_TAGS = new AtomicReference<>(null);
    private static KeyBinding TAGS_KEY;
    
    @Override
    public void onInitializeClient() {
        TAGS_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.screenshotmetadata.add_tags",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TAGS_KEY.wasPressed()) {
                if (client != null) {
                    client.setScreen(new TagInputScreen(client.currentScreen));
                }
            }
        });

        LOGGER.info("{} v{} initialized.", MOD_NAME, MOD_VERSION);
    }

    public static void setPendingTags(String tags) {
        if (tags == null || tags.isBlank()) {
            PENDING_TAGS.set(null);
            return;
        }
        PENDING_TAGS.set(tags.trim());
    }

    public static String getPendingTags() {
        return PENDING_TAGS.get();
    }

    public static String consumePendingTags() {
        return PENDING_TAGS.getAndSet(null);
    }
}
