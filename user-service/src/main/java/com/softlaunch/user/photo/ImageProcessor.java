package com.softlaunch.user.photo;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Every uploaded photo is decoded and re-encoded here before anyone can see it:
 *  - proves it is a real JPEG/PNG (a renamed .exe or .txt fails to decode)
 *  - refuses huge images BEFORE decoding (a 50 KB file can claim 100,000 x 100,000 px = a "decompression bomb")
 *  - applies the EXIF rotation, then DROPS ALL METADATA: phone photos often carry the GPS position
 *    where they were taken (usually home). The new JPEG we write contains pixels only.
 *  - shrinks to maxDimension, so we never serve 12-megapixel originals
 */
final class ImageProcessor {

    record Processed(byte[] jpeg, int width, int height) {
    }

    static final class InvalidImageException extends RuntimeException {
        InvalidImageException(String message) {
            super(message);
        }
    }

    private static final Set<String> ALLOWED_FORMATS = Set.of("jpeg", "png");
    private static final float JPEG_QUALITY = 0.85f;

    private ImageProcessor() {
    }

    static Processed process(byte[] original, long maxPixels, int maxDimension) {
        int orientation = ExifOrientation.read(original);   // read BEFORE we throw the metadata away
        BufferedImage decoded = decode(original, maxPixels);
        BufferedImage upright = applyOrientation(decoded, orientation);
        BufferedImage resized = resizeToRgb(upright, maxDimension);
        return new Processed(encodeJpeg(resized), resized.getWidth(), resized.getHeight());
    }

    private static BufferedImage decode(byte[] bytes, long maxPixels) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (in == null || !readers.hasNext()) {
                throw new InvalidImageException("Not an image");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!ALLOWED_FORMATS.contains(format)) {
                    throw new InvalidImageException("Only JPEG and PNG photos are supported");
                }
                reader.setInput(in, true, true);   // ignoreMetadata = true: we never even parse it
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);   // header only, nothing decoded yet
                if (pixels > maxPixels) {
                    throw new InvalidImageException("Photo is too large (max " + maxPixels / 1_000_000 + " megapixels)");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof InvalidImageException invalid) throw invalid;
            throw new InvalidImageException("Not a valid image");
        }
    }

    /** EXIF orientation 1-8 (see the EXIF spec): rotate/flip so the pixels themselves are upright. */
    private static BufferedImage applyOrientation(BufferedImage img, int orientation) {
        if (orientation <= 1 || orientation > 8) return img;
        int w = img.getWidth(), h = img.getHeight();
        boolean swap = orientation >= 5;   // 5-8 rotate by 90/270 degrees: width and height swap
        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 2 -> { t.translate(w, 0); t.scale(-1, 1); }                       // mirror
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); }                     // 180
            case 4 -> { t.translate(0, h); t.scale(1, -1); }                        // flip vertical
            case 5 -> { t.rotate(Math.PI / 2); t.scale(1, -1); }                    // transpose
            case 6 -> { t.translate(h, 0); t.rotate(Math.PI / 2); }                 // 90 clockwise (most phone portraits)
            case 7 -> { t.translate(h, w); t.scale(-1, -1); t.rotate(Math.PI / 2); t.scale(1, -1); } // transverse
            case 8 -> { t.translate(0, w); t.rotate(3 * Math.PI / 2); }             // 90 counter-clockwise
            default -> { }
        }
        BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.drawImage(img, t, null);
        g.dispose();
        return out;
    }

    /** Fit inside maxDimension x maxDimension, on a white background (PNG transparency -> white). */
    private static BufferedImage resizeToRgb(BufferedImage img, int maxDimension) {
        double scale = Math.min(1.0, (double) maxDimension / Math.max(img.getWidth(), img.getHeight()));
        int w = Math.max(1, (int) Math.round(img.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(img.getHeight() * scale));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /** A brand-new JPEG: ImageIO writes pixels + a minimal JFIF header, no EXIF, no GPS. */
    private static byte[] encodeJpeg(BufferedImage img) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(out);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(img, null, null), param);   // null metadata = nothing copied over
        } catch (IOException e) {
            throw new IllegalStateException("JPEG encoding failed", e);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    /** Minimal EXIF reader: finds ONLY the orientation tag (0x0112). Any parsing problem = "upright" (1). */
    static final class ExifOrientation {

        private ExifOrientation() {
        }

        static int read(byte[] b) {
            try {
                if (b.length < 4 || (b[0] & 0xFF) != 0xFF || (b[1] & 0xFF) != 0xD8) return 1;   // not a JPEG
                int pos = 2;
                while (pos + 4 <= b.length) {
                    if ((b[pos] & 0xFF) != 0xFF) return 1;
                    int marker = b[pos + 1] & 0xFF;
                    int length = ((b[pos + 2] & 0xFF) << 8) | (b[pos + 3] & 0xFF);
                    if (marker == 0xDA) return 1;                       // start of image data: no EXIF before it
                    if (marker == 0xE1 && pos + 10 <= b.length
                            && b[pos + 4] == 'E' && b[pos + 5] == 'x' && b[pos + 6] == 'i' && b[pos + 7] == 'f') {
                        return fromTiff(b, pos + 10, Math.min(b.length, pos + 2 + length));
                    }
                    pos += 2 + length;
                }
            } catch (RuntimeException ignored) {
                // broken EXIF is common; never fail an upload over it
            }
            return 1;
        }

        private static int fromTiff(byte[] b, int tiff, int end) {
            boolean little = b[tiff] == 'I';
            int ifd = tiff + u32(b, tiff + 4, little);
            int entries = u16(b, ifd, little);
            for (int i = 0; i < entries; i++) {
                int entry = ifd + 2 + i * 12;
                if (entry + 12 > end) break;
                if (u16(b, entry, little) == 0x0112) {
                    int value = u16(b, entry + 8, little);
                    return value >= 1 && value <= 8 ? value : 1;
                }
            }
            return 1;
        }

        private static int u16(byte[] b, int i, boolean little) {
            return little ? (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8) : ((b[i] & 0xFF) << 8) | (b[i + 1] & 0xFF);
        }

        private static int u32(byte[] b, int i, boolean little) {
            return little
                    ? (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8) | ((b[i + 2] & 0xFF) << 16) | ((b[i + 3] & 0xFF) << 24)
                    : ((b[i] & 0xFF) << 24) | ((b[i + 1] & 0xFF) << 16) | ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
        }
    }
}
