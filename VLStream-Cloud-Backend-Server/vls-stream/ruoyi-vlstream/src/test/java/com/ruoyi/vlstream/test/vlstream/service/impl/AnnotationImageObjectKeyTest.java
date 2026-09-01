package com.ruoyi.vlstream.test.vlstream.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnotationImageObjectKeyTest {

    @Test
    void shouldExtractObjectKeyFromLegacyPresignedUrl() {
        String storedUrl = "http://frontend/vlstream/2026/08/31/example.jpg?X-Amz-Signature=abc";

        assertEquals("2026/08/31/example.jpg", AnnotationImageObjectKey.normalize(storedUrl, "vlstream"));
    }

    @Test
    void shouldKeepDurableObjectKey() {
        assertEquals("2026/08/31/example.jpg", AnnotationImageObjectKey.normalize("2026/08/31/example.jpg", "vlstream"));
    }
}
