package com.fentbuscoding.screenshotmetadata.neoforge;

import com.fentbuscoding.screenshotmetadata.platform.Platform;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NeoForgePlatform implements Platform {
    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String getModVersion(String modId) {
        ModList mods = ModList.get();
        if (mods == null) {
            return null;
        }
        return mods.getModContainerById(modId)
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse(null);
    }

    @Override
    public boolean isModLoaded(String modId) {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded(modId);
    }

    @Override
    public List<ModEntry> getMods() {
        List<ModEntry> entries = new ArrayList<>();
        ModList mods = ModList.get();
        if (mods == null) {
            return entries;
        }
        for (IModInfo mod : mods.getMods()) {
            entries.add(new ModEntry(mod.getModId(), mod.getVersion().toString()));
        }
        entries.sort(Comparator.comparing(ModEntry::id));
        return entries;
    }

    @Override
    public String getLoaderName() {
        return "NeoForge";
    }
}
