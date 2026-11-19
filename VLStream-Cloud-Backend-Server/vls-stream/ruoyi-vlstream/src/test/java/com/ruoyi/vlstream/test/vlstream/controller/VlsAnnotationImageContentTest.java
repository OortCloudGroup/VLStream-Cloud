package com.ruoyi.vlstream.test.vlstream.controller;

import com.ruoyi.vlstream.test.vlstream.data.DataManagementService;
import com.ruoyi.vlstream.test.vlstream.data.DataMediaStorage;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.Tag("dev")
class VlsAnnotationImageContentTest {
    private final DataManagementService data = mock(DataManagementService.class);
    private final DataMediaStorage storage = mock(DataMediaStorage.class);
    private final VlsAnnotationImageController controller = new VlsAnnotationImageController(null);

    VlsAnnotationImageContentTest() {
        ReflectionTestUtils.setField(controller, "dataManagementService", data);
        ReflectionTestUtils.setField(controller, "mediaStorage", storage);
    }

    @Test
    void streamsOriginalBytesUsingAuthorizedSampleKey() throws Exception {
        AnnotationImage image = new AnnotationImage();
        image.setMediaType("image");
        image.setLocalPath("2026/11/19/example.jpg");
        byte[] bytes = {1, 2, 3};
        when(data.sample(1L, 2L)).thenReturn(image);
        when(storage.read(image.getLocalPath())).thenReturn(new ByteArrayInputStream(bytes));
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.readAnnotationImage(1L, 2L, response);
        assertArrayEquals(bytes, response.getContentAsByteArray());
        assertEquals("private, no-store", response.getHeader("Cache-Control"));
    }

    @Test
    void deniedSampleNeverReadsStorage() {
        when(data.sample(1L, 2L)).thenThrow(new IllegalStateException("denied"));
        assertThrows(IllegalStateException.class,
            () -> controller.readAnnotationImage(1L, 2L, new MockHttpServletResponse()));
        verifyNoInteractions(storage);
    }
}
