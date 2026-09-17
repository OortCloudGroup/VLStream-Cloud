package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.ruoyi.vlstream.test.vlstream.data.DatasetStorageProvider;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAnnotationImageMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.oss.core.OssClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Arrays;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class AnnotationImageLoadingTest {
    @Test void unavailableStorageFailsOnceInsteadOfRetryingForEveryImage() {
        VlsAnnotationImageServiceImpl service = new VlsAnnotationImageServiceImpl();
        DatasetStorageProvider storage = mock(DatasetStorageProvider.class);
        VlsAnnotationImageMapper mapper = mock(VlsAnnotationImageMapper.class);
        ReflectionTestUtils.setField(service, "storageProvider", storage);
        ReflectionTestUtils.setField(service, "annotationImageMapper", mapper);
        when(mapper.selectByDatasetId(1L)).thenReturn(Arrays.asList(new AnnotationImage(), new AnnotationImage(), new AnnotationImage()));
        when(storage.current()).thenThrow(new IllegalStateException("storage unavailable"));
        assertThrows(IllegalStateException.class, () -> service.getImagesByDataset(1L));
        verify(storage, times(1)).current();
    }

    @Test void signsAllImagesWithOneClientAndNormalizesLegacyUrls() {
        VlsAnnotationImageServiceImpl service = new VlsAnnotationImageServiceImpl();
        DatasetStorageProvider storage = mock(DatasetStorageProvider.class);
        VlsAnnotationImageMapper mapper = mock(VlsAnnotationImageMapper.class);
        OssClient client = mock(OssClient.class);
        ReflectionTestUtils.setField(service, "storageProvider", storage);
        ReflectionTestUtils.setField(service, "annotationImageMapper", mapper);
        ReflectionTestUtils.setField(service, "annotationMediaSignedUrlTtlSeconds", 600);
        ReflectionTestUtils.setField(service, "annotationMediaPublicEndpoint", "");
        when(storage.current()).thenReturn(client); when(client.getBucketName()).thenReturn("images");
        AnnotationImage a = new AnnotationImage(); a.setLocalPath("http://legacy/images/a.png?expired=yes");
        AnnotationImage b = new AnnotationImage(); b.setLocalPath("b.png");
        when(mapper.selectByDatasetId(1L)).thenReturn(Arrays.asList(a,b));
        when(client.getPrivateUrl("a.png",600,"")).thenReturn("fresh-a");
        when(client.getPrivateUrl("b.png",600,"")).thenReturn("fresh-b");
        assertEquals("fresh-a", service.getImagesByDataset(1L).get(0).getLocalPath());
        assertEquals("fresh-b", b.getLocalPath()); verify(storage,times(1)).current();
    }
}
