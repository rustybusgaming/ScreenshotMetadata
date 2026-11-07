package com.fentbuscoding.screenshotmetadata.fabric;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint for Fabric, Quilt, Babric and BTA.
 */
public class ScreenshotMetadataFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenshotMetadataMod.init();
    }
}
