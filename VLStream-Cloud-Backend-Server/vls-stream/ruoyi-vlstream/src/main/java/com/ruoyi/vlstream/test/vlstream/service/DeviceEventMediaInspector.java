package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.vlstream.test.vlstream.data.SampleMediaInspector;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/** Bounded streamed verification. MP4 uses container checks, never image decoding. */
final class DeviceEventMediaInspector {
    private DeviceEventMediaInspector() { }

    static void validate(OssClient client, DeviceMediaUpload upload, long maxBytes) {
        Path temporary = null;
        try {
            if (upload.getFileSize() == null || upload.getFileSize() <= 0 || upload.getFileSize() > maxBytes) {
                throw new ServiceException("媒体超过配置大小上限");
            }
            temporary = Files.createTempFile("vls-event-media-", ".tmp");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long total = 0;
            try (InputStream input = client.getObjectContent(upload.getObjectKey());
                 OutputStream output = Files.newOutputStream(temporary)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    if (length == 0) continue;
                    total += length;
                    if (total > maxBytes || total > upload.getFileSize()) {
                        throw new ServiceException("媒体实际字节数超过声明或配置上限");
                    }
                    digest.update(buffer, 0, length);
                    output.write(buffer, 0, length);
                }
            }
            if (total != upload.getFileSize()) throw new ServiceException("媒体实际字节数不匹配");
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest.digest()) hex.append(String.format("%02x", value & 255));
            if (!hex.toString().equalsIgnoreCase(upload.getSha256())) {
                throw new ServiceException("MinIO 媒体 SHA-256 校验失败");
            }
            byte[] header = new byte[32];
            int count;
            try (InputStream input = Files.newInputStream(temporary)) { count = input.read(header); }
            String type = upload.getContentType();
            boolean matches;
            if (DeviceMediaUploadService.isVideoType(type)) {
                // Exclude QuickTime, HEIF and arbitrary ISO-BMFF files, even with an .mp4 name.
                String brand = count >= 12 ? ascii(header, 8, 4) : "";
                matches = count >= 16 && "ftyp".equals(ascii(header, 4, 4))
                    && Arrays.asList("isom", "iso2", "iso3", "iso4", "iso5", "iso6", "mp41", "mp42", "avc1", "M4V ", "MSNV").contains(brand);
                if (matches) new SampleMediaInspector().inspectFile("event.mp4", temporary);
            } else if ("image/jpeg".equalsIgnoreCase(type)) {
                matches = count >= 3 && header[0] == (byte) 0xff && header[1] == (byte) 0xd8 && header[2] == (byte) 0xff;
            } else if ("image/png".equalsIgnoreCase(type)) {
                matches = count >= 8 && Arrays.equals(Arrays.copyOf(header, 8), new byte[] {(byte) 137,80,78,71,13,10,26,10});
            } else {
                matches = "image/webp".equalsIgnoreCase(type) && count >= 12
                    && "RIFF".equals(ascii(header, 0, 4)) && "WEBP".equals(ascii(header, 8, 4));
            }
            if (!matches) throw new ServiceException("媒体实际格式与声明 Content-Type 不一致");
            if (DeviceMediaUploadService.isVideoType(type)) {
                // The PUT grant may still be valid after binding. Publish only the verified
                // bytes under a server-only key, so later device PUTs cannot replace evidence.
                client.uploadFile(temporary.toFile(), DeviceMediaUploadService.verifiedVideoKey(upload), type);
            }
        } catch (ServiceException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new ServiceException("读取校验事件媒体失败：" + failure.getMessage());
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (java.io.IOException failure) { temporary.toFile().deleteOnExit(); }
            }
        }
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }
}
