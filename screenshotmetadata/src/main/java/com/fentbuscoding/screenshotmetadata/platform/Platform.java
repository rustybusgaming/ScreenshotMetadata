package com.fentbuscoding.screenshotmetadata.platform;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The few things the shared code needs from the mod loader. Fabric (and Quilt,
 * Babric and BTA, which all expose the Fabric Loader API) and NeoForge each ship
 * one implementation, registered in {@code META-INF/services}.
 */
public interface Platform {
    /** Directory that holds {@code screenshotmetadata.json}. */
    Path getConfigDir();

    /** Version of an installed mod, or {@code null} if it is not installed. */
    String getModVersion(String modId);

    boolean isModLoaded(String modId);

    /** Every loaded mod, sorted by id. */
    List<ModEntry> getMods();

    /** Loader name recorded in the metadata, e.g. "Fabric" or "NeoForge". */
    String getLoaderName();

    record ModEntry(String id, String version) {
    }

    static Platform get() {
        return Holder.INSTANCE;
    }

    final class Holder {
        private static final Platform INSTANCE = load();

        private Holder() {
        }

        private static Platform load() {
            Iterator<Platform> found = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).iterator();
            if (!found.hasNext()) {
                throw new IllegalStateException("No " + Platform.class.getName() + " implementation on the classpath");
            }
            return found.next();
        }
    }
}
