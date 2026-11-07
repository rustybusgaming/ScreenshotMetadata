package com.fentbuscoding.screenshotmetadata.metadata;

import com.fentbuscoding.screenshotmetadata.ScreenshotMetadataMod;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import org.w3c.dom.Node;

/**
 * Shared read-modify-write plumbing for the PNG writers.
 *
 * <p>The file is decoded with an {@link ImageReader} so that the image metadata
 * it already carries (text chunks written by an earlier pass, an embedded XMP
 * packet, colour information, unknown chunks) is kept and handed back to the
 * writer, instead of being replaced by default metadata. Several writers run one
 * after another on the same screenshot, and each must leave the others' chunks
 * in place.
 */
final class PngFileRewriter {

    /** The iTXt keyword that holds an embedded XMP packet. */
    static final String XMP_ITXT_KEYWORD = "XML:com.adobe.xmp";

    private static final String[] TEXT_CHUNK_NODES = {"tEXt", "iTXt", "zTXt"};

    private PngFileRewriter() {
    }

    /** Edits a copy of the file's native metadata tree; return false to report failure. */
    @FunctionalInterface
    interface TreeEditor {
        boolean edit(IIOMetadataNode root) throws Exception;
    }

    /**
     * Rewrites {@code file} with the same pixels and the existing metadata, after
     * applying {@code editors} to the metadata tree. Each editor is tried in turn
     * on a fresh copy of the existing tree until one succeeds.
     */
    static void rewrite(File file, String purpose, List<TreeEditor> editors) throws IOException {
        Path tempPath = Files.createTempFile(file.getParentFile().toPath(), file.getName(), ".tmp");
        boolean moved = false;

        ImageWriter writer = null;
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
            if (!writers.hasNext()) {
                throw new IOException("No PNG writer available");
            }
            writer = writers.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();

            Loaded loaded = load(file);
            BufferedImage image = loaded.image;
            IIOMetadata meta = loaded.metadata;
            if (meta == null || meta.isReadOnly() || !isNativePng(meta)) {
                ScreenshotMetadataMod.LOGGER.debug(
                    "Existing PNG metadata of {} is not editable; starting from defaults", file.getName());
                meta = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), writeParam);
            }
            String nativeFormat = meta.getNativeMetadataFormatName();
            if (nativeFormat == null || nativeFormat.isBlank()) {
                throw new IOException("PNG metadata format is unavailable");
            }

            // One copy of the existing tree per attempt, taken before any attempt
            // touches the metadata (IIOMetadataNode.cloneNode drops attributes).
            List<IIOMetadataNode> trees = new ArrayList<>();
            for (int i = 0; i < editors.size(); i++) {
                trees.add((IIOMetadataNode) meta.getAsTree(nativeFormat));
            }
            boolean applied = false;
            for (int i = 0; i < editors.size(); i++) {
                try {
                    IIOMetadataNode root = trees.get(i);
                    if (editors.get(i).edit(root)) {
                        // setFromTree resets the metadata first, so a failed attempt
                        // never leaves stale state behind for the next one.
                        meta.setFromTree(nativeFormat, root);
                        applied = true;
                        break;
                    }
                } catch (Exception e) {
                    ScreenshotMetadataMod.LOGGER.debug("{} merge attempt failed for {}: {}",
                        purpose, file.getName(), e.getMessage());
                }
            }
            if (!applied) {
                throw new IOException("Could not merge " + purpose + " into PNG metadata");
            }

            try (ImageOutputStream output = ImageIO.createImageOutputStream(tempPath.toFile())) {
                writer.setOutput(output);
                writer.write(null, new IIOImage(image, null, meta), writeParam);
            }

            try {
                Files.move(tempPath, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicFailure) {
                Files.move(tempPath, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
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
    }

    /**
     * Removes every tEXt, iTXt and zTXt entry whose keyword matches, and drops
     * text chunk containers left empty.
     */
    static void removeTextEntries(IIOMetadataNode root, Predicate<String> keywordMatches) {
        List<Node> emptied = new ArrayList<>();
        for (Node chunk = root.getFirstChild(); chunk != null; chunk = chunk.getNextSibling()) {
            if (!isTextChunkNode(chunk.getNodeName())) {
                continue;
            }
            Node entry = chunk.getFirstChild();
            while (entry != null) {
                Node next = entry.getNextSibling();
                if (entry instanceof IIOMetadataNode node && keywordMatches.test(node.getAttribute("keyword"))) {
                    chunk.removeChild(entry);
                }
                entry = next;
            }
            if (!chunk.hasChildNodes()) {
                emptied.add(chunk);
            }
        }
        for (Node chunk : emptied) {
            root.removeChild(chunk);
        }
    }

    /** Returns the root's child with the given name, creating and appending it if absent. */
    static IIOMetadataNode childNode(IIOMetadataNode root, String name) {
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (name.equals(child.getNodeName())) {
                return (IIOMetadataNode) child;
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }

    private static boolean isTextChunkNode(String name) {
        for (String textChunk : TEXT_CHUNK_NODES) {
            if (textChunk.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNativePng(IIOMetadata meta) {
        String nativeFormat = meta.getNativeMetadataFormatName();
        return nativeFormat != null && nativeFormat.startsWith("javax_imageio_png");
    }

    private record Loaded(BufferedImage image, IIOMetadata metadata) {
    }

    private static Loaded load(File file) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            if (input == null) {
                throw new IOException("Could not open image: " + file.getName());
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("No reader for image: " + file.getName());
            }
            ImageReader reader = readers.next();
            try {
                // ignoreMetadata = false: the existing chunks are what must survive.
                reader.setInput(input, true, false);
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IOException("Could not read image data from: " + file.getName());
                }
                return new Loaded(image, reader.getImageMetadata(0));
            } finally {
                reader.dispose();
            }
        }
    }
}
