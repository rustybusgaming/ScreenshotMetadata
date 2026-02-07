package com.fentbuscoding.screenshotmetadata.metadata;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Creates XMP sidecar files for screenshots to provide File Explorer-visible metadata.
 * XMP (Extensible Metadata Platform) files are industry standard and recognized by Windows.
 */
public class XmpSidecarWriter {
    
    private static final String XMP_TEMPLATE_HEADER = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">\n" +
        " <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
        "          xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
        "          xmlns:xmp=\"http://ns.adobe.com/xap/1.0/\"\n" +
        "          xmlns:minecraft=\"http://fentbuscoding.com/minecraft/ns/\">\n" +
        "  <rdf:Description rdf:about=\"\">\n";
    
    private static final String XMP_TEMPLATE_FOOTER = 
        "  </rdf:Description>\n" +
        " </rdf:RDF>\n" +
        "</x:xmpmeta>";
    
    /**
     * Creates an XMP sidecar file for the given image file.
     * The XMP file will have the same name as the image but with .xmp extension.
     * 
     * @param imageFile The image file to create a sidecar for
     * @param metadata The metadata to include in the XMP file
     */
    public static void writeSidecarFile(File imageFile, Map<String, String> metadata) {
        if (imageFile == null || !imageFile.exists()) {
            ScreenshotMetadataMod.LOGGER.warn("Cannot create XMP sidecar for non-existent file: {}", 
                imageFile != null ? imageFile.getName() : "null");
            return;
        }
        
        try {
            File xmpFile = getXmpFile(imageFile);
            String xmpContent = generateXmpContent(imageFile, metadata);
            
            try (FileWriter writer = new FileWriter(xmpFile)) {
                writer.write(xmpContent);
            }
            
            ScreenshotMetadataMod.LOGGER.debug("Created XMP sidecar file: {}", xmpFile.getName());
            
        } catch (IOException e) {
            ScreenshotMetadataMod.LOGGER.error("Failed to create XMP sidecar file for {}: {}", 
                imageFile.getName(), e.getMessage());
        }
    }
    
    /**
     * Gets the XMP file path for a given image file
     */
    private static File getXmpFile(File imageFile) {
        String baseName = imageFile.getName();
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        return new File(imageFile.getParent(), baseName + ".xmp");
    }
    
    /**
     * Generates the complete XMP content
     */
    private static String generateXmpContent(File imageFile, Map<String, String> metadata) {
        StringBuilder xmp = new StringBuilder();
        xmp.append(XMP_TEMPLATE_HEADER);
        
        // Dublin Core metadata (recognized by File Explorer)
        addDublinCoreMetadata(xmp, metadata);
        
        // XMP basic metadata
        addXmpBasicMetadata(xmp, metadata);
        
        // Custom Minecraft namespace metadata
        addMinecraftMetadata(xmp, metadata);
        
        xmp.append(XMP_TEMPLATE_FOOTER);
        return xmp.toString();
    }
    
    /**
     * Adds Dublin Core metadata elements
     */
    private static void addDublinCoreMetadata(StringBuilder xmp, Map<String, String> metadata) {
        // Title
        String title = "Minecraft - " + metadata.getOrDefault("Username", "Unknown Player");
        xmp.append("   <dc:title>").append(escapeXml(title)).append("</dc:title>\n");
        
        // Description
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
        
        xmp.append("   <dc:description>").append(escapeXml(description.toString())).append("</dc:description>\n");
        
        // Creator
        String creator = metadata.getOrDefault("Username", "Unknown Player");
        xmp.append("   <dc:creator>").append(escapeXml(creator)).append("</dc:creator>\n");
        
        // Subject/Keywords
        String subject = "Minecraft Screenshot";
        if (metadata.containsKey("Tags")) {
            String tags = metadata.get("Tags");
            if (tags != null && !tags.isBlank()) {
                subject = subject + ", " + tags;
            }
        }
        xmp.append("   <dc:subject>").append(escapeXml(subject)).append("</dc:subject>\n");
        
        // Type
        xmp.append("   <dc:type>Image</dc:type>\n");
    }
    
    /**
     * Adds XMP basic metadata
     */
    private static void addXmpBasicMetadata(StringBuilder xmp, Map<String, String> metadata) {
        // Software
          xmp.append("   <xmp:CreatorTool>Screenshot Metadata Mod v")
              .append(escapeXml(ScreenshotMetadataMod.MOD_VERSION))
              .append("</xmp:CreatorTool>\n");
        
        // Creation date
        if (metadata.containsKey("Timestamp")) {
            try {
                Instant timestamp = Instant.parse(metadata.get("Timestamp"));
                String formattedDate = DateTimeFormatter.ISO_INSTANT.format(timestamp);
                xmp.append("   <xmp:CreateDate>").append(formattedDate).append("</xmp:CreateDate>\n");
                xmp.append("   <xmp:ModifyDate>").append(formattedDate).append("</xmp:ModifyDate>\n");
            } catch (Exception e) {
                ScreenshotMetadataMod.LOGGER.debug("Could not parse timestamp: {}", metadata.get("Timestamp"));
            }
        }
    }
    
    /**
     * Adds custom Minecraft-specific metadata
     */
    private static void addMinecraftMetadata(StringBuilder xmp, Map<String, String> metadata) {
        if (metadata.containsKey("World")) {
            xmp.append("   <minecraft:world>").append(escapeXml(metadata.get("World"))).append("</minecraft:world>\n");
        }
        
        if (metadata.containsKey("Biome")) {
            xmp.append("   <minecraft:biome>").append(escapeXml(metadata.get("Biome"))).append("</minecraft:biome>\n");
        }
        
        if (metadata.containsKey("X") && metadata.containsKey("Y") && metadata.containsKey("Z")) {
            String coords = metadata.get("X") + "," + metadata.get("Y") + "," + metadata.get("Z");
            xmp.append("   <minecraft:coordinates>").append(escapeXml(coords)).append("</minecraft:coordinates>\n");
            
            // Individual coordinate fields for better searchability
            xmp.append("   <minecraft:x>").append(escapeXml(metadata.get("X"))).append("</minecraft:x>\n");
            xmp.append("   <minecraft:y>").append(escapeXml(metadata.get("Y"))).append("</minecraft:y>\n");
            xmp.append("   <minecraft:z>").append(escapeXml(metadata.get("Z"))).append("</minecraft:z>\n");
        }
        
        if (metadata.containsKey("Username")) {
            xmp.append("   <minecraft:player>").append(escapeXml(metadata.get("Username"))).append("</minecraft:player>\n");
        }

        if (metadata.containsKey("Tags")) {
            String tags = metadata.get("Tags");
            if (tags != null && !tags.isBlank()) {
                xmp.append("   <minecraft:tags>").append(escapeXml(tags)).append("</minecraft:tags>\n");
            }
        }

        if (metadata.containsKey("Weather")) {
            xmp.append("   <minecraft:weather>").append(escapeXml(metadata.get("Weather"))).append("</minecraft:weather>\n");
        }
    }
    
    /**
     * Escapes XML special characters
     */
    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
