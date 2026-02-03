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
        writeSidecarFile(imageFile, metadata, null);
    }

    /**
     * Creates a JSON sidecar file with optional extended context fields.
     *
     * @param imageFile The image file to create a sidecar for
     * @param metadata The metadata to include in the JSON file
     * @param context Optional extra context (resource packs, shaders, mod list)
     */
    public static void writeSidecarFile(File imageFile, Map<String, String> metadata, JsonSidecarContext context) {
        if (imageFile == null || !imageFile.exists()) {
            ScreenshotMetadataMod.LOGGER.warn("Cannot create JSON sidecar for non-existent file: {}",
                imageFile != null ? imageFile.getName() : "null");
            return;
        }

        try {
            File jsonFile = getJsonFile(imageFile);
            String jsonContent = generateJsonContent(imageFile, metadata, context);

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

    private static String generateJsonContent(File imageFile, Map<String, String> metadata, JsonSidecarContext context) {
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
        json.append("}");

        if (context != null) {
            appendModpackContext(json, context);
        }

        json.append("\n");
        json.append("}\n");
        return json.toString();
    }

    private static void appendModpackContext(StringBuilder json, JsonSidecarContext context) {
        json.append(",\n");
        json.append("  \"modpack\": {\n");

        int fieldCount = 0;
        if (context.getShaderPack() != null) {
            appendStringField(json, "shaderPack", context.getShaderPack(), fieldCount > 0);
            fieldCount++;
        }

        if (context.getModCount() >= 0) {
            appendNumberField(json, "modCount", context.getModCount(), fieldCount > 0);
            fieldCount++;
        }

        if (!context.getResourcePacks().isEmpty()) {
            appendStringArrayField(json, "resourcePacks", context.getResourcePacks(), fieldCount > 0);
            fieldCount++;
        }

        if (!context.getMods().isEmpty()) {
            appendStringArrayField(json, "mods", context.getMods(), fieldCount > 0);
            fieldCount++;
        }

        appendBooleanField(json, "modListTruncated", context.isModListTruncated(), fieldCount > 0);
        json.append("\n  }");
    }

    private static void appendStringField(StringBuilder json, String key, String value, boolean withComma) {
        if (withComma) {
            json.append(",\n");
        }
        json.append("    \"")
            .append(escapeJson(key))
            .append("\": \"")
            .append(escapeJson(value))
            .append("\"");
    }

    private static void appendNumberField(StringBuilder json, String key, int value, boolean withComma) {
        if (withComma) {
            json.append(",\n");
        }
        json.append("    \"")
            .append(escapeJson(key))
            .append("\": ")
            .append(value);
    }

    private static void appendBooleanField(StringBuilder json, String key, boolean value, boolean withComma) {
        if (withComma) {
            json.append(",\n");
        }
        json.append("    \"")
            .append(escapeJson(key))
            .append("\": ")
            .append(value ? "true" : "false");
    }

    private static void appendStringArrayField(StringBuilder json, String key, Iterable<String> values, boolean withComma) {
        if (withComma) {
            json.append(",\n");
        }
        json.append("    \"")
            .append(escapeJson(key))
            .append("\": [");

        int index = 0;
        for (String value : values) {
            if (value == null) {
                continue;
            }
            if (index > 0) {
                json.append(", ");
            }
            json.append("\"").append(escapeJson(value)).append("\"");
            index++;
        }
        json.append("]");
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
