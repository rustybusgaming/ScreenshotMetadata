package com.fentbuscoding.screenshotmetadata.metadata;

import java.util.Collections;
import java.util.List;

public class JsonSidecarContext {
    private final List<String> resourcePacks;
    private final String shaderPack;
    private final List<String> mods;
    private final int modCount;
    private final boolean modListTruncated;

    public JsonSidecarContext(List<String> resourcePacks,
                              String shaderPack,
                              List<String> mods,
                              int modCount,
                              boolean modListTruncated) {
        this.resourcePacks = resourcePacks == null ? Collections.emptyList() : resourcePacks;
        this.shaderPack = shaderPack;
        this.mods = mods == null ? Collections.emptyList() : mods;
        this.modCount = modCount;
        this.modListTruncated = modListTruncated;
    }

    public List<String> getResourcePacks() {
        return resourcePacks;
    }

    public String getShaderPack() {
        return shaderPack;
    }

    public List<String> getMods() {
        return mods;
    }

    public int getModCount() {
        return modCount;
    }

    public boolean isModListTruncated() {
        return modListTruncated;
    }
}
