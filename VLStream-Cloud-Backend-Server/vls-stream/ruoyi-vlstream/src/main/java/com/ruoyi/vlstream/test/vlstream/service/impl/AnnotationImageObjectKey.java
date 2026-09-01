/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Converts the legacy persisted OSS URL into the durable object key.
 */
final class AnnotationImageObjectKey {

    private AnnotationImageObjectKey() {
    }

    static String normalize(String storedPath, String bucketName) {
        if (storedPath == null || storedPath.trim().isEmpty()) {
            return null;
        }
        String value = storedPath.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return trimLeadingSlashes(value);
        }
        try {
            String path = URI.create(value).getRawPath();
            if (path == null || path.trim().isEmpty()) {
                return null;
            }
            String bucketPrefix = "/" + bucketName + "/";
            if (path.startsWith(bucketPrefix)) {
                path = path.substring(bucketPrefix.length());
            } else {
                path = trimLeadingSlashes(path);
            }
            return URLDecoder.decode(path, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            return value;
        }
    }

    private static String trimLeadingSlashes(String value) {
        int index = 0;
        while (index < value.length() && value.charAt(index) == '/') {
            index++;
        }
        return value.substring(index);
    }
}
