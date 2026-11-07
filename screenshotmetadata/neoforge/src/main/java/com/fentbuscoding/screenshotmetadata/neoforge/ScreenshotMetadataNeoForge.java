package com.fentbuscoding.screenshotmetadata.neoforge;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import com.fentbuscoding.screenshotmetadata.compat.ScreenshotMetadataConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * NeoForge entrypoint. The mod is client-only: the screenshot hook is a client
 * mixin, and nothing is registered on the server.
 */
@Mod(value = ScreenshotMetadataMod.MOD_ID, dist = Dist.CLIENT)
public class ScreenshotMetadataNeoForge {
    public ScreenshotMetadataNeoForge(IEventBus modBus, ModContainer container) {
        ScreenshotMetadataMod.init();
        // The "Config" button in NeoForge's mod list opens the same screen ModMenu does on Fabric.
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (IConfigScreenFactory) (mod, parent) -> new ScreenshotMetadataConfigScreen(parent));
    }
}
