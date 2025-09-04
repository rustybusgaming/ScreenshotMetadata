package com.example.screenshotmetadata;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.IIOImage;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PngMetadataWriter {
    public static void writeMetadata(File file, Map<String, String> metadata) throws IOException {
        // Load original image
        BufferedImage image = ImageIO.read(file);
        // Prepare writer
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        IIOMetadata meta = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), writeParam);
        String nativeFormat = meta.getNativeMetadataFormatName();
        // Build metadata tree
        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(nativeFormat);
        IIOMetadataNode textNode = new IIOMetadataNode("tEXt");
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
            textEntry.setAttribute("keyword", entry.getKey());
            textEntry.setAttribute("value", entry.getValue());
            textNode.appendChild(textEntry);
        }
        root.appendChild(textNode);
        try {
            meta.mergeTree(nativeFormat, root);
        } catch (Exception e) {
            throw new IOException("Failed to merge PNG metadata", e);
        }
        // Write to temp file
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileImageOutputStream output = new FileImageOutputStream(tmp)) {
            writer.setOutput(output);
            writer.write(meta, new IIOImage(image, null, meta), writeParam);
        }
        writer.dispose();
        // Replace original file
        if (!file.delete() || !tmp.renameTo(file)) {
            throw new IOException("Could not replace original PNG");
        }
    }
}
