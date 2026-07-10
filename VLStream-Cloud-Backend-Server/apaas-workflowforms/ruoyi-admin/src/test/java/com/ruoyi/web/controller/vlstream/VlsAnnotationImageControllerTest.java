package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.domain.AnnotationImage;
import com.ruoyi.vlstream.service.IVlsAnnotationImageService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsAnnotationImageControllerTest {

    @Test
    void exposesFrontendAnnotationImageRoutes() throws Exception {
        Method dataset = VlsAnnotationImageController.class.getDeclaredMethod("getImagesByDataset", Long.class);
        Method upload = VlsAnnotationImageController.class.getDeclaredMethod("uploadImages", MultipartFile[].class, Long.class);
        Method save = VlsAnnotationImageController.class.getDeclaredMethod("saveImage", AnnotationImage.class);
        Method batchSave = VlsAnnotationImageController.class.getDeclaredMethod("batchSaveImages", List.class);

        assertArrayEquals(new String[] {"/dataset/{annotationId}"}, dataset.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/upload"}, upload.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/images"}, save.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/images/batch"}, batchSave.getAnnotation(PostMapping.class).value());
    }

    @Test
    void datasetListReturnsSourceCompatibleSuccessMap() {
        IVlsAnnotationImageService service = mock(IVlsAnnotationImageService.class);
        VlsAnnotationImageController controller = new VlsAnnotationImageController(service);
        AnnotationImage image = image(7L, 19L, "fire.jpg", "http://example/fire.jpg");
        List<AnnotationImage> images = Collections.singletonList(image);

        when(service.getImagesByDataset(19L)).thenReturn(images);

        Map<String, Object> response = controller.getImagesByDataset(19L);

        assertEquals(Boolean.TRUE, response.get("success"));
        assertSame(images, response.get("data"));
        verify(service).getImagesByDataset(19L);
    }

    @Test
    void uploadImagesForwardsMultipartFilesAndAnnotationId() {
        IVlsAnnotationImageService service = mock(IVlsAnnotationImageService.class);
        VlsAnnotationImageController controller = new VlsAnnotationImageController(service);
        MockMultipartFile file = new MockMultipartFile("files", "fire.jpg", "image/jpeg", "jpg".getBytes());
        MultipartFile[] files = new MultipartFile[] {file};
        AnnotationImage image = image(8L, 19L, "fire.jpg", "/annotations/19/fire.jpg");
        List<AnnotationImage> images = Collections.singletonList(image);

        when(service.uploadImages(files, 19L)).thenReturn(images);

        Map<String, Object> response = controller.uploadImages(files, 19L);

        assertEquals(Boolean.TRUE, response.get("success"));
        assertEquals("Image uploaded successfully", response.get("message"));
        assertSame(images, response.get("data"));
        verify(service).uploadImages(files, 19L);
    }

    @Test
    void saveRoutesReturnSavedImagesForFrontendDbIdBackfill() {
        IVlsAnnotationImageService service = mock(IVlsAnnotationImageService.class);
        VlsAnnotationImageController controller = new VlsAnnotationImageController(service);
        AnnotationImage first = image(11L, 19L, "first.jpg", "http://example/first.jpg");
        AnnotationImage second = image(12L, 19L, "second.jpg", "http://example/second.jpg");
        List<AnnotationImage> request = Arrays.asList(first, second);

        when(service.saveImage(first)).thenReturn(first);
        when(service.batchSaveImages(request)).thenReturn(request);

        Map<String, Object> singleResponse = controller.saveImage(first);
        Map<String, Object> batchResponse = controller.batchSaveImages(request);

        assertEquals(Boolean.TRUE, singleResponse.get("success"));
        assertSame(first, singleResponse.get("data"));
        assertEquals(Boolean.TRUE, batchResponse.get("success"));
        assertSame(request, batchResponse.get("data"));
        assertTrue(((List<?>) batchResponse.get("data")).get(0) instanceof AnnotationImage);
        verify(service).saveImage(first);
        verify(service).batchSaveImages(request);
    }

    private AnnotationImage image(Long id, Long annotationId, String name, String path) {
        AnnotationImage image = new AnnotationImage();
        image.setId(id);
        image.setAnnotationId(annotationId);
        image.setImageName(name);
        image.setOriginalName(name);
        image.setLocalPath(path);
        image.setFileSize(3L);
        image.setIsImported(1);
        return image;
    }
}
