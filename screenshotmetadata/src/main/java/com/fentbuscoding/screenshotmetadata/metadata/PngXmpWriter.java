package com.fentbuscoding.screenshotmetadata.metadata;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import javax.imageio.metadata.IIOMetadataNode;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Embeds XMP metadata directly inside PNG files as an iTXt chunk with the
 * standard keyword {@code XML:com.adobe.xmp}. This format is read by Adobe
 * tools, Windows File Explorer, and other mods such as screenshot-manager-enhanced.
 */
public class PngXmpWriter {

    private static final String XMP_ITXT_KEYWORD = PngFileRewriter.XMP_ITXT_KEYWORD;

    /**
     * Embeds XMP metadata into a PNG file.
     *
     * @param file     The PNG file to add embedded XMP to
     * @param metadata Map of key-value metadata pairs
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
            ScreenshotMetadataMod.LOGGER.debug("No metadata provided for {} - skipping embedded XMP", file.getName());
            return;
        }

        ScreenshotMetadataMod.LOGGER.debug("Writing embedded XMP to: {} ({} entries)", file.getName(), metadata.size());

        String xmpPacket = buildXmpPacket(metadata);
        try {
            // Keep the chunks already in the file (the text entries PngMetadataWriter
            // wrote, colour information, ...) and replace only the XMP packet.
            PngFileRewriter.rewrite(file, "XMP iTXt chunk", List.of(root -> mergeXmpChunk(root, xmpPacket)));
        } catch (Exception e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            ScreenshotMetadataMod.LOGGER.debug("Failed to write embedded XMP to {}: {}", file.getName(), reason, e);
            throw new IOException("Failed to write embedded XMP: " + reason, e);
        }

        ScreenshotMetadataMod.LOGGER.debug("Successfully wrote embedded XMP to: {}", file.getName());
    }

    /**
     * Builds a complete XMP packet from the metadata map.
     */
    private static String buildXmpPacket(Map<String, String> metadata) {
        StringBuilder xmp = new StringBuilder();
        xmp.append("<x:xmpmeta xmlns:x='adobe:ns:meta/' >\n");
        xmp.append(" <rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>\n");
        xmp.append("    <rdf:Description rdf:about=''\n");
        xmp.append("        xmlns:dc='http://purl.org/dc/elements/1.1/'\n");
        xmp.append("        xmlns:xmp='http://ns.adobe.com/xap/1.0/'\n");
        xmp.append("        xmlns:mc='http://milezerosoftware.com/mc/1.0/'>\n");

        addDublinCoreFields(xmp, metadata);
        addXmpBasicFields(xmp, metadata);
        addMinecraftFields(xmp, metadata);

        xmp.append("    </rdf:Description>\n");
        xmp.append(" </rdf:RDF>\n");
        xmp.append("</x:xmpmeta>");
        return xmp.toString();
    }

    private static void addDublinCoreFields(StringBuilder xmp, Map<String, String> metadata) {
        // Use rdf:Alt structure for dc:title and dc:description to match XMP spec
        // and screenshot-manager-enhanced format
        String title = "Minecraft - " + metadata.getOrDefault("Username", "Unknown Player");
        xmp.append("      <dc:title>\n");
        xmp.append("        <rdf:Alt>\n");
        xmp.append("          <rdf:li xml:lang='x-default'>").append(escapeXml(title)).append("</rdf:li>\n");
        xmp.append("        </rdf:Alt>\n");
        xmp.append("      </dc:title>\n");

        StringBuilder description = new StringBuilder();
        String worldName = metadata.getOrDefault("WorldName", metadata.getOrDefault("World", ""));
        String dimension = metadata.getOrDefault("Dimension", "");
        String coords = "";
        if (metadata.containsKey("X") && metadata.containsKey("Y") && metadata.containsKey("Z")) {
            coords = metadata.get("X") + ", " + metadata.get("Y") + ", " + metadata.get("Z");
        }
        description.append("World: ").append(worldName.isEmpty() ? "Unknown" : worldName)
                .append(" | Dim: ").append(dimension.isEmpty() ? "Unknown" : dimension)
                .append(" | Loc: ").append(coords.isEmpty() ? "Unknown" : coords);
        xmp.append("      <dc:description>\n");
        xmp.append("        <rdf:Alt>\n");
        xmp.append("          <rdf:li xml:lang='x-default'>").append(escapeXml(description.toString())).append("</rdf:li>\n");
        xmp.append("        </rdf:Alt>\n");
        xmp.append("      </dc:description>\n");
    }

    private static void addXmpBasicFields(StringBuilder xmp, Map<String, String> metadata) {
        xmp.append("      <xmp:CreatorTool>Screenshot Metadata Mod v")
                .append(escapeXml(ScreenshotMetadataMod.MOD_VERSION))
                .append("</xmp:CreatorTool>\n");

        if (metadata.containsKey("Timestamp")) {
            try {
                Instant timestamp = Instant.parse(metadata.get("Timestamp"));
                String formattedDate = DateTimeFormatter.ISO_INSTANT.format(timestamp);
                xmp.append("      <xmp:CreateDate>").append(formattedDate).append("</xmp:CreateDate>\n");
                xmp.append("      <xmp:ModifyDate>").append(formattedDate).append("</xmp:ModifyDate>\n");
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.debug("Could not parse timestamp: {}", metadata.get("Timestamp"));
            }
        }
    }

    private static void addMinecraftFields(StringBuilder xmp, Map<String, String> metadata) {
        // Use PascalCase property names matching screenshot-manager-enhanced's mc namespace
        appendIfPresent(xmp, "mc:WorldTitle", metadata, "WorldName", "World");
        appendIfPresent(xmp, "mc:Dimension", metadata, "Dimension");
        appendIfPresent(xmp, "mc:Biome", metadata, "Biome");

        if (metadata.containsKey("X") && metadata.containsKey("Y") && metadata.containsKey("Z")) {
            String coords = metadata.get("X") + ", " + metadata.get("Y") + ", " + metadata.get("Z");
            xmp.append("      <mc:Coordinates>").append(escapeXml(coords)).append("</mc:Coordinates>\n");
        }

        appendIfPresent(xmp, "mc:Difficulty", metadata, "Difficulty");
        appendIfPresent(xmp, "mc:Version", metadata, "MinecraftVersion");
        appendIfPresent(xmp, "mc:GameMode", metadata, "GameMode");
        appendIfPresent(xmp, "mc:Player", metadata, "Username");
        appendIfPresent(xmp, "mc:Weather", metadata, "Weather");
        appendIfPresent(xmp, "mc:ServerType", metadata, "ServerType");
    }

    private static void appendIfPresent(StringBuilder xmp, String element, Map<String, String> metadata, String... keys) {
        for (String key : keys) {
            if (metadata.containsKey(key)) {
                String value = metadata.get(key);
                if (value != null && !value.isBlank()) {
                    xmp.append("      <").append(element).append(">")
                            .append(escapeXml(value))
                            .append("</").append(element).append(">\n");
                    return;
                }
            }
        }
    }

    /**
     * Puts the XMP packet into the PNG metadata tree as an iTXt chunk, replacing
     * any XMP packet the file already carries so it is never duplicated.
     */
    private static boolean mergeXmpChunk(IIOMetadataNode root, String xmpPacket) {
        PngFileRewriter.removeTextEntries(root, XMP_ITXT_KEYWORD::equals);

        IIOMetadataNode entry = new IIOMetadataNode("iTXtEntry");
        entry.setAttribute("keyword", XMP_ITXT_KEYWORD);
        entry.setAttribute("compressionFlag", "FALSE");
        entry.setAttribute("compressionMethod", "0");
        entry.setAttribute("languageTag", "");
        entry.setAttribute("translatedKeyword", "");
        entry.setAttribute("text", xmpPacket);

        PngFileRewriter.childNode(root, "iTXt").appendChild(entry);
        return true;
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
