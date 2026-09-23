package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.BitSet;
import java.util.Iterator;

/** Lossless binary PNG mask cropped to its region; holes and disconnected components are retained. */
public final class AnnotationMask {
    public static final int MAX_PIXELS = 40_000_000;
    public static final int MAX_ENCODED_BYTES = 8 * 1024 * 1024;

    private AnnotationMask() { }

    public static BitSet decode(String encoded, int width, int height) {
        checkDimensions(width, height);
        if (encoded == null || encoded.isEmpty() || encoded.length() > (MAX_ENCODED_BYTES * 4L / 3 + 8))
            throw new ServiceException("像素掩膜为空或超过8 MiB");
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(encoded); }
        catch (IllegalArgumentException ex) { throw new ServiceException("像素掩膜编码无效"); }
        byte[] signature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        if (bytes.length < signature.length || bytes.length > MAX_ENCODED_BYTES) throw new ServiceException("像素掩膜必须为PNG");
        for (int i = 0; i < signature.length; i++) if (bytes[i] != signature[i]) throw new ServiceException("像素掩膜必须为PNG");
        ImageReader reader = null;
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ServiceException("无法解析像素掩膜");
            reader = readers.next(); reader.setInput(input, true, true);
            if (reader.getWidth(0) != width || reader.getHeight(0) != height) throw new ServiceException("掩膜尺寸与标注区域不一致");
            BufferedImage bitmap = reader.read(0);
            BitSet foreground = new BitSet(width * height);
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                int pixel = bitmap.getRGB(x, y), alpha = pixel >>> 24, color = pixel & 0xffffff;
                if (alpha == 0) continue;
                if (alpha != 255 || (color != 0 && color != 0xffffff)) throw new ServiceException("像素掩膜只能包含黑白二值像素");
                if (color == 0xffffff) foreground.set(y * width + x);
            }
            return foreground;
        } catch (ServiceException ex) { throw ex; }
        catch (Exception ex) { throw new ServiceException("无法解析像素掩膜"); }
        finally { if (reader != null) reader.dispose(); }
    }

    public static String encode(BitSet foreground, int width, int height) {
        checkDimensions(width, height);
        if (foreground.length() > (long) width * height) throw new ServiceException("掩膜像素超出图片范围");
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int index = foreground.nextSetBit(0); index >= 0; index = foreground.nextSetBit(index + 1))
            image.setRGB(index % width, index / width, 0xffffff);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", bytes) || bytes.size() > MAX_ENCODED_BYTES) throw new ServiceException("像素掩膜超过处理限制");
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (java.io.IOException ex) { throw new ServiceException("无法编码像素掩膜"); }
    }

    public static void checkDimensions(int width, int height) {
        if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) throw new ServiceException("像素掩膜尺寸无效或超过4000万像素");
    }
}
