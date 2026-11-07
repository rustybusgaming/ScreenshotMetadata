package com.fentbuscoding.screenshotmetadata.compat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Minecraft 1.21.x names for the calls the shared Mojang-names mixin makes (used by
 * the NeoForge 1.21.x builds); the Minecraft 26 equivalents live in src/mojang26.
 *
 * <p>Two renames happen inside 1.21.x itself: 1.21.11 renamed ResourceLocation to
 * Identifier (so {@code ResourceKey#location()} became {@code identifier()}) and moved
 * {@code Util}. Those two are looked up at runtime so one class covers 1.21.1-1.21.11.
 */
public final class MinecraftCompat {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ScreenshotMetadata-IO");
        thread.setDaemon(true);
        return thread;
    });

    private MinecraftCompat() {
    }

    public static Executor ioExecutor() {
        return IO;
    }

    public static String id(ResourceKey<?> key) {
        Object id = location(key);
        return id != null ? id.toString() : "unknown";
    }

    public static String path(ResourceKey<?> key) {
        String id = id(key);
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    public static long dayTime(ClientLevel level) {
        return level.getDayTime();
    }

    private static Object location(ResourceKey<?> key) {
        for (String name : new String[]{"location", "identifier"}) {
            try {
                Method method = key.getClass().getMethod(name);
                return method.invoke(key);
            } catch (ReflectiveOperationException ignored) {
                // Try the other name.
            }
        }
        return null;
    }
}
