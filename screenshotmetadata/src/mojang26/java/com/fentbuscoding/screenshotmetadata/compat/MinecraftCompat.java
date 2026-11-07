package com.fentbuscoding.screenshotmetadata.compat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;

import java.util.concurrent.Executor;

/**
 * Minecraft 26 names for the calls the shared Mojang-names mixin makes; the 1.21.x
 * equivalents live in src/mojang121.
 */
public final class MinecraftCompat {
    private MinecraftCompat() {
    }

    public static Executor ioExecutor() {
        return Util.ioPool();
    }

    public static String id(ResourceKey<?> key) {
        return key.identifier().toString();
    }

    public static String path(ResourceKey<?> key) {
        return key.identifier().getPath();
    }

    public static long dayTime(ClientLevel level) {
        return level.getDefaultClockTime();
    }
}
