package com.fentbuscoding.screenshotmetadata.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Beta 1.7.3 keeps the client instance in a private static field with no getter.
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("INSTANCE")
    static Minecraft screenshotmetadata$getInstance() {
        throw new AssertionError();
    }
}
