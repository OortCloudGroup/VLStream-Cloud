package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class SampleMediaInspectorTest {
    private final SampleMediaInspector inspector = new SampleMediaInspector();

    @Test void decodesImagesAndDetectsFlatLowQualitySamples() throws Exception {
        byte[] png = image(false, 1);
        SampleMediaInspector.Inspection result = inspector.inspect("sample.png", png);
        assertEquals(100, result.getWidth()); assertEquals("image", result.getMediaType());
        assertEquals(64, result.getSha256().length()); assertTrue(result.getIssues().contains("模糊"));
        assertTrue(inspector.inspect("textured.png", image(true, 1)).getFocusScore() > result.getFocusScore());
    }

    @Test void rejectsEmptyForgedAndTruncatedFiles() throws Exception {
        assertThrows(ServiceException.class, () -> inspector.inspect("bad.png", new byte[0]));
        byte[] png = image(true, 2);
        assertThrows(ServiceException.class, () -> inspector.inspect("fake.jpg", png));
        assertThrows(ServiceException.class, () -> inspector.inspect("bad.png", Arrays.copyOf(png, png.length - 5)));
        assertThrows(ServiceException.class, () -> inspector.inspect("bad.mp4", new byte[]{0, 0, 1, 0, 'm', 'd', 'a', 't'}));
    }

    @Test void rejectsAudioOnlyAndTruncatedVideoContainers() throws Exception {
        byte[] hdlr = new byte[16]; System.arraycopy("vide".getBytes(StandardCharsets.US_ASCII), 0, hdlr, 8, 4);
        byte[] video = concat(box("ftyp", new byte[8]), box("moov", box("trak", box("mdia", box("hdlr", hdlr)))), box("mdat", new byte[]{1, 2, 3}));
        assertEquals("video", inspector.inspect("clip.mp4", video).getMediaType());
        assertThrows(ServiceException.class, () -> inspector.inspect("clip.mp4", Arrays.copyOf(video, video.length - 1)));
        System.arraycopy("soun".getBytes(StandardCharsets.US_ASCII), 0, hdlr, 8, 4);
        byte[] audio = concat(box("ftyp", new byte[8]), box("moov", box("trak", box("mdia", box("hdlr", hdlr)))), box("mdat", new byte[]{1}));
        assertThrows(ServiceException.class, () -> inspector.inspect("audio.mp4", audio));
    }

    static byte[] image(boolean textured, int seed) throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB); Random random = new Random(seed);
        if (textured) for (int y = 0; y < 100; y++) for (int x = 0; x < 100; x++) image.setRGB(x, y, random.nextInt(0xffffff));
        ByteArrayOutputStream output = new ByteArrayOutputStream(); ImageIO.write(image, "png", output); return output.toByteArray();
    }

    private byte[] box(String type, byte[] body) { return ByteBuffer.allocate(body.length + 8).putInt(body.length + 8).put(type.getBytes(StandardCharsets.US_ASCII)).put(body).array(); }
    private byte[] concat(byte[]... parts) throws IOException { ByteArrayOutputStream out = new ByteArrayOutputStream(); for (byte[] part : parts) out.write(part); return out.toByteArray(); }
}
