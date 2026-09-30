package com.ruoyi.vlstream.test.vlstream.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class PortableDatasetZipWriterTest {
    @Test
    void removesPublishingHostPathAndPreservesTrainingFiles() throws Exception {
        byte[] image = {1, 2, 3, 4};
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(original, StandardCharsets.UTF_8)) {
            add(zip, "version_3/dataset.yaml", "path: \"/data/work/private/version_3\"\ntrain: images/train\nval: images/val\nnc: 1\n".getBytes(StandardCharsets.UTF_8));
            add(zip, "version_3/images/train/image.jpg", image);
            add(zip, "version_3/labels/train/image.txt", "0 0.5 0.5 1 1\n".getBytes(StandardCharsets.UTF_8));
        }

        ByteArrayOutputStream portable = new ByteArrayOutputStream();
        PortableDatasetZipWriter.write(new ByteArrayInputStream(original.toByteArray()), portable);
        Map<String, byte[]> files = read(portable.toByteArray());
        String yaml = new String(files.get("version_3/dataset.yaml"), StandardCharsets.UTF_8);
        assertFalse(yaml.contains("/data/work/private"));
        assertFalse(yaml.contains("path:"));
        assertTrue(yaml.contains("train: images/train"));
        assertTrue(yaml.contains("val: images/val"));
        assertArrayEquals(image, files.get("version_3/images/train/image.jpg"));
        assertTrue(files.containsKey("version_3/labels/train/image.txt"));
    }

    @Test
    void rejectsArchiveWithoutDatasetYaml() throws Exception {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(original)) {
            add(zip, "version_3/images/train/image.jpg", new byte[]{1});
        }
        assertThrows(IOException.class, () -> PortableDatasetZipWriter.write(
            new ByteArrayInputStream(original.toByteArray()), new ByteArrayOutputStream()));
    }

    private static void add(ZipOutputStream zip, String path, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(bytes);
        zip.closeEntry();
    }

    private static Map<String, byte[]> read(byte[] archive) throws IOException {
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            while ((entry = zip.getNextEntry()) != null) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                }
                files.put(entry.getName(), bytes.toByteArray());
            }
        }
        return files;
    }
}
