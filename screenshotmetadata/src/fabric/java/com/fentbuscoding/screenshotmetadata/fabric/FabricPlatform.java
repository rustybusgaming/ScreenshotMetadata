package com.fentbuscoding.screenshotmetadata.fabric;

import com.fentbuscoding.screenshotmetadata.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fabric Loader implementation. Quilt Loader, Babric and BTA all provide the same
 * API, so this one class serves every Fabric-style build.
 */
public class FabricPlatform implements Platform {
    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String getModVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse(null);
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public List<ModEntry> getMods() {
        List<ModEntry> mods = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            mods.add(new ModEntry(mod.getMetadata().getId(), mod.getMetadata().getVersion().getFriendlyString()));
        }
        mods.sort(Comparator.comparing(ModEntry::id));
        return mods;
    }

    @Override
    public String getLoaderName() {
        // Quilt Loader runs Fabric mods and exposes this API too; say which one it is.
        return FabricLoader.getInstance().isModLoaded("quilt_loader") ? "Quilt" : "Fabric";
    }
}
