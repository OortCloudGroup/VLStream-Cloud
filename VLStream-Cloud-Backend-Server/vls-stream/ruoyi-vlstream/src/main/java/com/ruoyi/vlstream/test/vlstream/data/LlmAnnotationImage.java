package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import lombok.Value;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

final class LlmAnnotationImage {
    private LlmAnnotationImage() { }

    @Value
    static class Dimensions {
        int width;
        int height;
    }

    static Dimensions inspect(byte[] bytes) throws IOException {
        try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ServiceException("大模型标注只支持可解码的 JPEG 或 PNG 图片");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if ("JPEG".equalsIgnoreCase(format)) checkJpegOrientation(bytes);
                else if ("PNG".equalsIgnoreCase(format)) checkPngOrientation(bytes);
                else throw new ServiceException("大模型标注只支持 JPEG 或 PNG 图片");
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                AnnotationMask.checkDimensions(width, height);
                return new Dimensions(width, height);
            } finally {
                reader.dispose();
            }
        }
    }

    private static void checkJpegOrientation(byte[] bytes) {
        int offset = 2;
        while (offset + 4 <= bytes.length && (bytes[offset] & 255) == 255) {
            int marker = bytes[offset + 1] & 255;
            if (marker == 0xda || marker == 0xd9) return;
            if (marker == 255) { offset++; continue; }
            int length = (bytes[offset + 2] & 255) * 256 + (bytes[offset + 3] & 255);
            if (length < 2 || offset + 2 + length > bytes.length) throw invalidMetadata();
            int payload = offset + 4;
            if (marker == 0xe1 && length >= 8 && bytes[payload] == 'E' && bytes[payload + 1] == 'x'
                && bytes[payload + 2] == 'i' && bytes[payload + 3] == 'f'
                && bytes[payload + 4] == 0 && bytes[payload + 5] == 0) {
                checkTiffOrientation(bytes, payload + 6, length - 8);
            }
            offset += 2 + length;
        }
    }

    private static void checkPngOrientation(byte[] bytes) {
        ByteBuffer png = ByteBuffer.wrap(bytes);
        for (int offset = 8; offset <= bytes.length - 12;) {
            int length = png.getInt(offset);
            if (length < 0 || length > bytes.length - offset - 12) throw invalidMetadata();
            int type = png.getInt(offset + 4);
            if (type == 0x65584966) checkTiffOrientation(bytes, offset + 8, length); // eXIf
            if (type == 0x49454e44) return; // IEND
            offset += 12 + length;
        }
    }

    private static void checkTiffOrientation(byte[] bytes, int offset, int length) {
        if (length < 8) throw invalidMetadata();
        ByteBuffer tiff = ByteBuffer.wrap(bytes, offset, length).slice();
        int byteOrder = tiff.getShort(0) & 0xffff;
        if (byteOrder == 0x4949) tiff.order(ByteOrder.LITTLE_ENDIAN);
        else if (byteOrder != 0x4d4d) throw invalidMetadata();
        if (tiff.getShort(2) != 42) throw invalidMetadata();
        int directory = tiff.getInt(4);
        if (directory < 8 || directory > length - 2) throw invalidMetadata();
        int entries = tiff.getShort(directory) & 0xffff;
        if (entries > (length - directory - 2) / 12) throw invalidMetadata();
        for (int index = 0; index < entries; index++) {
            int entry = directory + 2 + index * 12;
            if ((tiff.getShort(entry) & 0xffff) != 0x0112) continue;
            if (tiff.getShort(entry + 2) != 3 || tiff.getInt(entry + 4) != 1) throw invalidMetadata();
            if (tiff.getShort(entry + 8) != 1)
                throw new ServiceException("图片带有 EXIF 旋转或镜像，请转为已校正方向的图片后重新标注");
        }
    }

    private static ServiceException invalidMetadata() {
        return new ServiceException("无法校验图片方向或文件结构");
    }
}
