package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import lombok.Data;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Basic image decoding and ISO BMFF container integrity checks; no GPU or external process required. */
@Component
public class SampleMediaInspector {
    public static final int MAX_FILE_BYTES = 100 * 1024 * 1024;

    @Data
    public static class Inspection {
        private String mediaType;
        private String contentType;
        private String sha256;
        private int width;
        private int height;
        private double focusScore;
        private String issues = "";
    }

    public Inspection inspect(String filename, byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_FILE_BYTES) throw new ServiceException("文件不能为空，单文件不能超过 100 MB");
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        Inspection result = new Inspection();
        result.setSha256(sha256(bytes));
        if (Arrays.asList("mp4", "mov").contains(extension)) {
            inspectVideo(bytes);
            result.setMediaType("video");
            result.setContentType("mov".equals(extension) ? "video/quicktime" : "video/mp4");
            result.setIssues("已通过容器结构检查；视频清晰度与内容完整性需人工播放抽检");
            return result;
        }
        if (!Arrays.asList("jpg", "jpeg", "png", "bmp").contains(extension))
            throw new ServiceException("仅支持 JPG、PNG、BMP 图片和 MP4、MOV 视频");
        if (bytes.length > 25 * 1024 * 1024) throw new ServiceException("图片不能超过 25 MB");
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ServiceException("图片无法解码或文件内容与扩展名不符");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String expected = "jpg".equals(extension) ? "jpeg" : extension;
                if (!expected.equals(format)) throw new ServiceException("文件内容与扩展名不符");
                if ("jpeg".equals(format) && (bytes.length < 4 || bytes[bytes.length - 2] != (byte) 0xff || bytes[bytes.length - 1] != (byte) 0xd9))
                    throw new ServiceException("JPEG 文件缺少结束标记，可能已截断");
                if ("png".equals(format) && (bytes.length < 12 || !"IEND".equals(ascii(bytes, bytes.length - 8, 4))))
                    throw new ServiceException("PNG 文件缺少完整结束块");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if ((long) width * height > 40000000L) throw new ServiceException("图片像素数不能超过 4000 万");
                List<String> warnings = new ArrayList<>();
                reader.addIIOReadWarningListener((source, warning) -> warnings.add(warning));
                BufferedImage image = reader.read(0);
                if (image == null || !warnings.isEmpty()) throw new ServiceException("图片数据不完整或解码出现异常");
                result.setMediaType("image");
                result.setContentType("image/" + format);
                result.setWidth(width);
                result.setHeight(height);
                result.setFocusScore(focus(image));
                List<String> issues = new ArrayList<>();
                if (width < 64 || height < 64) issues.add("分辨率过低（短边小于 64 像素）");
                if (result.getFocusScore() < 40) issues.add("疑似模糊或低纹理，需人工确认");
                result.setIssues(String.join("；", issues));
                return result;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new ServiceException("图片解码失败，文件可能损坏");
        }
    }

    private void inspectVideo(byte[] bytes) {
        boolean fileType = false, mediaData = false, videoTrack = false;
        long offset = 0;
        while (offset < bytes.length) {
            if (bytes.length - offset < 8) throw new ServiceException("视频块头不完整");
            int start = (int) offset;
            long size = Integer.toUnsignedLong(ByteBuffer.wrap(bytes, start, 4).getInt());
            String type = ascii(bytes, start + 4, 4);
            int header = 8;
            if (size == 1) {
                if (bytes.length - offset < 16) throw new ServiceException("视频扩展块头不完整");
                size = ByteBuffer.wrap(bytes, start + 8, 8).getLong();
                header = 16;
            } else if (size == 0) size = bytes.length - offset;
            if (size < header || size > bytes.length - offset) throw new ServiceException("视频数据被截断或块长度无效");
            if ("ftyp".equals(type)) fileType = size >= 16;
            if ("mdat".equals(type)) mediaData |= size > header;
            if ("moov".equals(type)) videoTrack |= containsVideoTrack(bytes, start + header, (int) (offset + size), 0);
            offset += size;
        }
        if (!fileType || !mediaData || !videoTrack) throw new ServiceException("视频缺少文件标识、视频轨道或媒体数据，仅支持完整 MP4/MOV 文件");
    }

    private boolean containsVideoTrack(byte[] bytes, int start, int end, int depth) {
        if (depth > 5) return false;
        boolean video = false;
        while (start < end) {
            if (end - start < 8) throw new ServiceException("视频索引块不完整");
            long length = Integer.toUnsignedLong(ByteBuffer.wrap(bytes, start, 4).getInt());
            if (length < 8 || length > end - start) throw new ServiceException("视频索引块长度无效");
            String type = ascii(bytes, start + 4, 4);
            if ("hdlr".equals(type) && length >= 20) video |= "vide".equals(ascii(bytes, start + 16, 4));
            if (Arrays.asList("trak", "mdia", "minf", "stbl").contains(type))
                video |= containsVideoTrack(bytes, start + 8, start + (int) length, depth + 1);
            start += (int) length;
        }
        return video;
    }

    private static String ascii(byte[] bytes, int start, int size) {
        return new String(bytes, start, size, StandardCharsets.US_ASCII);
    }

    private double focus(BufferedImage image) {
        int step = Math.max(1, Math.max(image.getWidth(), image.getHeight()) / 512);
        double sum = 0, square = 0;
        int count = 0;
        for (int y = step; y < image.getHeight() - step; y += step) {
            for (int x = step; x < image.getWidth() - step; x += step) {
                double value = gray(image.getRGB(x - step, y)) + gray(image.getRGB(x + step, y))
                    + gray(image.getRGB(x, y - step)) + gray(image.getRGB(x, y + step)) - 4 * gray(image.getRGB(x, y));
                sum += value; square += value * value; count++;
            }
        }
        return count == 0 ? 0 : Math.round(Math.max(0, square / count - Math.pow(sum / count, 2)) * 100.0) / 100.0;
    }

    private double gray(int rgb) {
        return ((rgb >> 16) & 255) * 0.299 + ((rgb >> 8) & 255) * 0.587 + (rgb & 255) * 0.114;
    }

    private String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder value = new StringBuilder();
            for (byte b : digest) value.append(String.format("%02x", b & 255));
            return value.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
