package com.fentbuscoding.screenshotmetadata.metadata;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Creates JSON sidecar files for screenshots for easy parsing by tools.
 */
public class JsonSidecarWriter {

    /**
     * Creates a JSON sidecar file for the given image file.
     * The JSON file will have the same name as the image but with .json extension.
     *
     * @param imageFile The image file to create a sidecar for
     * @param metadata The metadata to include in the JSON file
     */
    public static void writeSidecarFile(File imageFile, Map<String, String> metadata) {
        if (imageFile == null || !imageFile.exists()) {
            ScreenshotMetadataMod.LOGGER.warn("Cannot create JSON sidecar for non-existent file: {}",
                imageFile != null ? imageFile.getName() : "null");
            return;
        }

        try {
            File jsonFile = getJsonFile(imageFile);
            String jsonContent = generateJsonContent(imageFile, metadata);

            try (FileWriter writer = new FileWriter(jsonFile)) {
                writer.write(jsonContent);
            }

            ScreenshotMetadataMod.LOGGER.debug("Created JSON sidecar file: {}", jsonFile.getName());

        } catch (IOException e) {
            ScreenshotMetadataMod.LOGGER.error("Failed to create JSON sidecar file for {}: {}",
                imageFile.getName(), e.getMessage());
        }
    }

    private static File getJsonFile(File imageFile) {
        String baseName = imageFile.getName();
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        return new File(imageFile.getParent(), baseName + ".json");
    }

    private static String generateJsonContent(File imageFile, Map<String, String> metadata) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"formatVersion\": \"1\",\n");
        json.append("  \"screenshotFile\": \"")
            .append(escapeJson(imageFile.getName()))
            .append("\",\n");

        json.append("  \"metadata\": {");
        if (metadata != null && !metadata.isEmpty()) {
            json.append("\n");
            int index = 0;
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                if (index > 0) {
                    json.append(",\n");
                }
                json.append("    \"")
                    .append(escapeJson(entry.getKey()))
                    .append("\": \"")
                    .append(escapeJson(entry.getValue()))
                    .append("\"");
                index++;
            }
            json.append("\n  ");
        }
        json.append("}\n");
        json.append("}\n");
        return json.toString();
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}