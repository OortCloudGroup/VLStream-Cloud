package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.function.LongConsumer;

public final class DatasetFileIO {
    public static final long FOUR_GIB = 4L * 1024 * 1024 * 1024;
    private DatasetFileIO() { }
    public static String sha256(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) { return digest(input, null, Long.MAX_VALUE, ignored -> { }); }
    }
    public static String copy(InputStream input, Path destination, long limit, LongConsumer progress) throws IOException {
        try (OutputStream output = Files.newOutputStream(destination)) { return digest(input, output, limit, progress); }
    }
    private static String digest(InputStream input, OutputStream output, long limit, LongConsumer progress) throws IOException {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[128 * 1024]; int count; long length = 0, notified = 0;
            while ((count = input.read(buffer)) != -1) {
                length += count;
                if (length > limit) throw new ServiceException("文件超过本次导入的大小限额");
                if (output != null) output.write(buffer, 0, count);
                hash.update(buffer, 0, count);
                if (length - notified >= 8L * 1024 * 1024) { progress.accept(length); notified = length; }
            }
            progress.accept(length);
            StringBuilder result = new StringBuilder(); for (byte b : hash.digest()) result.append(String.format("%02x", b & 255));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
