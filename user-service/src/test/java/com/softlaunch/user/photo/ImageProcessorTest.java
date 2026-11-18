package com.softlaunch.user.photo;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageProcessorTest {

    private static final long MAX_PIXELS = 40_000_000;

    @Test
    void stripsAllMetadataIncludingGps() throws Exception {
        byte[] withExif = withExif(jpeg(400, 200), 1);
        assertThat(contains(withExif, "HOME-GPS-12.97N-77.64E")).isTrue();   // the original leaks it

        byte[] out = ImageProcessor.process(withExif, MAX_PIXELS, 1600).jpeg();

        assertThat(contains(out, "HOME-GPS-12.97N-77.64E")).isFalse();
        assertThat(contains(out, "Exif")).isFalse();
    }

    @Test
    void appliesPhoneRotationBeforeDroppingIt() throws Exception {
        // 400x200 landscape pixels with a red block top-left; EXIF 6 = "display rotated 90 degrees clockwise"
        ImageProcessor.Processed p = ImageProcessor.process(withExif(jpeg(400, 200), 6), MAX_PIXELS, 1600);

        assertThat(p.width()).isEqualTo(200);
        assertThat(p.height()).isEqualTo(400);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(p.jpeg()));
        assertThat(isRed(img.getRGB(185, 15))).as("red block moved to the top-RIGHT").isTrue();
        assertThat(isRed(img.getRGB(15, 15))).isFalse();
    }

    @Test
    void shrinksBigPhotos() throws Exception {
        ImageProcessor.Processed p = ImageProcessor.process(jpeg(3200, 2400), MAX_PIXELS, 1600);
        assertThat(p.width()).isEqualTo(1600);
        assertThat(p.height()).isEqualTo(1200);
    }

    @Test
    void rejectsFilesThatAreNotImages() {
        byte[] text = "this is not a photo, just text renamed to .jpg".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> ImageProcessor.process(text, MAX_PIXELS, 1600))
                .isInstanceOf(ImageProcessor.InvalidImageException.class);
    }

    @Test
    void rejectsDecompressionBombsWithoutDecodingThem() throws Exception {
        // a tiny PNG whose header claims 50,000 x 50,000 pixels (would need ~10 GB of memory to decode)
        byte[] bomb = pngClaiming(50_000, 50_000);
        assertThat(bomb.length).isLessThan(200);
        assertThatThrownBy(() -> ImageProcessor.process(bomb, MAX_PIXELS, 1600))
                .isInstanceOf(ImageProcessor.InvalidImageException.class)
                .hasMessageContaining("too large");
    }

    // ---------- helpers ----------

    @Test
    void veilKeepsOnlyColourNotDetail() throws Exception {
        // a sharp black/white checkerboard: lots of detail
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(800, 1000, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 1000; y++) for (int x = 0; x < 800; x++) img.setRGB(x, y, ((x / 20 + y / 20) % 2 == 0) ? 0xFFFFFF : 0x000000);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpeg", out);

        byte[] veil = ImageProcessor.veil(out.toByteArray());
        java.awt.image.BufferedImage v = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(veil));

        assertThat(v.getWidth()).isEqualTo(120);
        assertThat(v.getHeight()).isEqualTo(150);                 // same shape as the photo
        assertThat(veil.length).isLessThan(8_000);               // tiny: nothing to zoom into
        int min = 255, max = 0;                                  // the checkerboard is gone: everything is mid-grey
        for (int y = 0; y < v.getHeight(); y++) for (int x = 0; x < v.getWidth(); x++) {
            int g = v.getRGB(x, y) & 0xFF;
            min = Math.min(min, g);
            max = Math.max(max, g);
        }
        assertThat(max - min).isLessThan(60);
    }

    private static byte[] jpeg(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.RED);
        g.fillRect(0, 0, 40, 40);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", out);
        return out.toByteArray();
    }

    /** Inserts an EXIF block (orientation + a fake GPS marker in ImageDescription) right after the JPEG start. */
    private static byte[] withExif(byte[] jpeg, int orientation) {
        byte[] marker = "HOME-GPS-12.97N-77.64E\0".getBytes(StandardCharsets.US_ASCII);
        ByteBuffer tiff = ByteBuffer.allocate(8 + 2 + 2 * 12 + 4 + marker.length).order(ByteOrder.LITTLE_ENDIAN);
        tiff.put((byte) 'I').put((byte) 'I').putShort((short) 42).putInt(8);
        tiff.putShort((short) 2);
        tiff.putShort((short) 0x010E).putShort((short) 2).putInt(marker.length).putInt(8 + 2 + 24 + 4);  // ImageDescription
        tiff.putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientation).putShort((short) 0);
        tiff.putInt(0);
        tiff.put(marker);
        byte[] t = tiff.array();
        ByteArrayOutputStream app1 = new ByteArrayOutputStream();
        int length = 2 + 6 + t.length;
        app1.write(0xFF); app1.write(0xE1); app1.write(length >> 8); app1.write(length & 0xFF);
        app1.writeBytes("Exif\0\0".getBytes(StandardCharsets.US_ASCII));
        app1.writeBytes(t);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2);
        out.writeBytes(app1.toByteArray());
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }

    private static byte[] pngClaiming(int w, int h) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", out);
        byte[] png = out.toByteArray();
        ByteBuffer.wrap(png, 16, 8).putInt(w).putInt(h);   // IHDR width/height
        CRC32 crc = new CRC32();
        crc.update(png, 12, 17);                            // "IHDR" + 13 data bytes
        ByteBuffer.wrap(png, 29, 4).putInt((int) crc.getValue());
        return png;
    }

    private static boolean contains(byte[] haystack, String needle) {
        return new String(haystack, StandardCharsets.ISO_8859_1).contains(needle);
    }

    private static boolean isRed(int rgb) {
        Color c = new Color(rgb);
        return c.getRed() > 200 && c.getGreen() < 60 && c.getBlue() < 60;
    }
}
