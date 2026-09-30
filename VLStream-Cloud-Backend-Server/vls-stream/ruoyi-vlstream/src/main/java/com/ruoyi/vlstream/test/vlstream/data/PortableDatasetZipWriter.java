package com.ruoyi.vlstream.test.vlstream.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Makes an exported training archive independent of the publishing host's filesystem. */
public final class PortableDatasetZipWriter {
    private static final int MAX_YAML_BYTES = 1024 * 1024;

    private PortableDatasetZipWriter() {
    }

    public static void write(InputStream source, OutputStream target) throws IOException {
        boolean yamlFound = false;
        byte[] buffer = new byte[32768];
        try (ZipInputStream input = new ZipInputStream(source, StandardCharsets.UTF_8);
             ZipOutputStream output = new ZipOutputStream(target, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                String name = entry.getName();
                ZipEntry copy = new ZipEntry(name);
                copy.setTime(entry.getTime());
                output.putNextEntry(copy);
                if (!entry.isDirectory() && ("dataset.yaml".equals(name) || name.endsWith("/dataset.yaml"))) {
                    yamlFound = true;
                    ByteArrayOutputStream yaml = new ByteArrayOutputStream();
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (yaml.size() + count > MAX_YAML_BYTES) {
                            throw new IOException("Dataset YAML exceeds export limit");
                        }
                        yaml.write(buffer, 0, count);
                    }
                    String portable = new String(yaml.toByteArray(), StandardCharsets.UTF_8)
                        .replaceFirst("(?m)^path:[^\\r\\n]*(\\r?\\n|$)", "");
                    output.write(portable.getBytes(StandardCharsets.UTF_8));
                } else {
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }
                }
                output.closeEntry();
                input.closeEntry();
            }
            if (!yamlFound) {
                throw new IOException("Dataset YAML is missing from the training archive");
            }
        }
    }
}
