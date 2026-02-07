package com.fentbuscoding.screenshotmetadata.metadata;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Map;

/**
 * Handles writing metadata to PNG files using ImageIO's text chunks.
 * This metadata is embedded directly in the PNG file and can be read by technical tools.
 */
public class PngMetadataWriter {
    
    /**
     * Writes metadata to a PNG file as text chunks.
     * Creates a temporary file and replaces the original to ensure data integrity.
     * 
     * @param file The PNG file to add metadata to
     * @param metadata Map of key-value pairs to embed
     * @throws IOException if file operations fail
     */
    public static void writeMetadata(File file, Map<String, String> metadata) throws IOException {
        if (file == null || metadata == null) {
            throw new IllegalArgumentException("File and metadata must not be null");
        }
        if (!file.exists() || !file.getName().toLowerCase().endsWith(".png")) {
            throw new IllegalArgumentException("File must be an existing PNG file: " + file.getPath());
        }
        if (metadata.isEmpty()) {
            ScreenshotMetadataMod.LOGGER.debug("No metadata provided for {} - skipping write", file.getName());
            return;
        }

        ScreenshotMetadataMod.LOGGER.debug("Writing PNG metadata to: {} ({} entries)", file.getName(), metadata.size());

        Path tempPath = Files.createTempFile(file.getParentFile().toPath(), file.getName(), ".tmp");
        boolean moved = false;

        ImageWriter writer = null;
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IOException("Could not read image data from: " + file.getName());
            }

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
            if (!writers.hasNext()) {
                throw new IOException("No PNG writer available");
            }
            writer = writers.next();

            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            IIOMetadata meta = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), writeParam);
            String nativeFormat = meta.getNativeMetadataFormatName();

            if (nativeFormat == null || nativeFormat.isBlank()) {
                throw new IOException("PNG metadata format is unavailable");
            }

            boolean merged = mergeTextMetadata(meta, nativeFormat, metadata, true);
            if (!merged) {
                // Recreate metadata before fallback; failed merge calls can leave metadata in a bad state.
                meta = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), writeParam);
                nativeFormat = meta.getNativeMetadataFormatName();
                if (nativeFormat == null || nativeFormat.isBlank() ||
                    !mergeTextMetadata(meta, nativeFormat, metadata, false)) {
                    throw new IOException("Could not merge PNG metadata");
                }
            }

            try (ImageOutputStream output = ImageIO.createImageOutputStream(tempPath.toFile())) {
                writer.setOutput(output);
                writer.write(null, new IIOImage(image, null, meta), writeParam);
            }

            // Replace original file with the updated one, prefer atomic move when supported
            try {
                Files.move(tempPath, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicFailure) {
                Files.move(tempPath, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;

        } catch (Exception e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            ScreenshotMetadataMod.LOGGER.debug("Failed to write PNG metadata to {}: {}", file.getName(), reason, e);
            throw new IOException("Failed to write PNG metadata: " + reason, e);
        } finally {
            if (writer != null) {
                writer.dispose();
            }
            if (!moved) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException cleanupFailure) {
                    ScreenshotMetadataMod.LOGGER.warn("Could not delete temp file {}: {}", tempPath, cleanupFailure.getMessage());
                }
            }
        }

        ScreenshotMetadataMod.LOGGER.debug("Successfully wrote PNG metadata to: {}", file.getName());
    }
    
    /**
     * Adds standard text entries that various tools might recognize
     */
    private static void addStandardTextEntries(IIOMetadataNode textNode, Map<String, String> metadata, boolean useITXt) {
        // Build comprehensive description
        StringBuilder description = new StringBuilder();
        description.append("Minecraft Screenshot");
        
        if (metadata.containsKey("Username")) {
            description.append(" - Player: ").append(metadata.get("Username"));
        }
        if (metadata.containsKey("World")) {
            description.append(" | World: ").append(metadata.get("World"));
        }
        if (metadata.containsKey("X") && metadata.containsKey("Y") && metadata.containsKey("Z")) {
            description.append(" | Coords: (")
                      .append(metadata.get("X")).append(", ")
                      .append(metadata.get("Y")).append(", ")
                      .append(metadata.get("Z")).append(")");
        }
        if (metadata.containsKey("Biome")) {
            description.append(" | Biome: ").append(metadata.get("Biome"));
        }
        
        // Add standard entries
        addTextEntry(textNode, "Comment", description.toString(), useITXt);
        addTextEntry(textNode, "Description", description.toString(), useITXt);
        addTextEntry(textNode, "Title", "Minecraft - " + metadata.getOrDefault("Username", "Unknown Player"), useITXt);
        addTextEntry(textNode, "Software", "Screenshot Metadata Mod v" + ScreenshotMetadataMod.MOD_VERSION, useITXt);
        addTextEntry(textNode, "Author", metadata.getOrDefault("Username", "Unknown Player"), useITXt);
    }
    
    /**
     * Helper method to add a text entry
     */
    private static void addTextEntry(IIOMetadataNode textNode, String keyword, String value, boolean useITXt) {
        if (keyword != null && value != null && !value.trim().isEmpty()) {
            IIOMetadataNode textEntry = new IIOMetadataNode(useITXt ? "iTXtEntry" : "tEXtEntry");
            textEntry.setAttribute("keyword", keyword);
            if (useITXt) {
                textEntry.setAttribute("text", value.trim());
                textEntry.setAttribute("languageTag", "");
                textEntry.setAttribute("translatedKeyword", "");
                textEntry.setAttribute("compressionFlag", "FALSE");
                textEntry.setAttribute("compressionMethod", "0");
            } else {
                textEntry.setAttribute("value", value.trim());
            }
            textNode.appendChild(textEntry);
        }
    }

    private static boolean mergeTextMetadata(IIOMetadata meta,
                                             String nativeFormat,
                                             Map<String, String> metadata,
                                             boolean useITXt) {
        try {
            IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(nativeFormat);
            IIOMetadataNode textNode = new IIOMetadataNode(useITXt ? "iTXt" : "tEXt");

            // Embed user-provided entries
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    addTextEntry(textNode, entry.getKey(), entry.getValue(), useITXt);
                }
            }

            addStandardTextEntries(textNode, metadata, useITXt);

            root.appendChild(textNode);
            meta.mergeTree(nativeFormat, root);
            return true;
        } catch (Exception e) {
            ScreenshotMetadataMod.LOGGER.debug("PNG metadata merge failed for {} chunks: {}",
                useITXt ? "iTXt" : "tEXt", e.getMessage());
            return false;
        }
    }
}
