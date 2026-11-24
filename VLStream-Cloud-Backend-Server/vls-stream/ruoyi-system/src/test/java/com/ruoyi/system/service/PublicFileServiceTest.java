package com.ruoyi.system.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.system.domain.VlsPublicFile;
import com.ruoyi.system.mapper.VlsPublicFileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class PublicFileServiceTest {

    private final VlsPublicFileMapper mapper = mock(VlsPublicFileMapper.class);
    private final OssClient uploadStorage = mock(OssClient.class);
    private final OssClient savedStorage = mock(OssClient.class);
    private final PublicFileService service = new PublicFileService(mapper) {
        @Override
        protected OssClient storage(String configKey) {
            if (configKey == null) {
                return uploadStorage;
            }
            assertEquals("saved-minio", configKey);
            return savedStorage;
        }
    };
    private final byte[] payload = {1, 2, 3};
    private final MultipartFile file = mock(MultipartFile.class);

    @BeforeEach
    void configureStorage() throws Exception {
        when(uploadStorage.getConfigKey()).thenReturn("saved-minio");
        when(file.getOriginalFilename()).thenReturn("C:\\fakepath\\说明.txt");
        when(file.getSize()).thenReturn((long) payload.length);
        doAnswer(call -> {
            Files.write(call.<Path>getArgument(0), payload);
            return null;
        }).when(file).transferTo(any(Path.class));
    }

    @Test
    void uploadUsesDiskAndPersistsOwnershipAfterMinioAcceptsTheBytes() throws Exception {
        doAnswer(call -> {
            File temporaryFile = call.getArgument(0);
            assertArrayEquals(payload, Files.readAllBytes(temporaryFile.toPath()));
            return null;
        }).when(uploadStorage).uploadFile(any(File.class), anyString(), eq("application/octet-stream"));
        when(mapper.insert(any(VlsPublicFile.class))).thenReturn(1);

        VlsPublicFile published = service.upload(file, "tenant-a", "uploader-a");
        assertEquals("tenant-a", published.getTenantId());
        assertEquals("uploader-a", published.getUploaderId());
        assertEquals("说明.txt", published.getOriginalName());
        assertEquals(payload.length, published.getFileSize());
        assertEquals("saved-minio", published.getStorageConfigKey());

        ArgumentCaptor<File> temporaryFile = ArgumentCaptor.forClass(File.class);
        InOrder order = inOrder(uploadStorage, mapper);
        order.verify(uploadStorage).uploadFile(temporaryFile.capture(), eq("public-files/" + published.getFileId()),
            eq("application/octet-stream"));
        order.verify(mapper).insert(published);
        assertFalse(temporaryFile.getValue().exists());
    }

    @Test
    void removesTheObjectWhenTheDatabaseInsertFails() {
        when(mapper.insert(any(VlsPublicFile.class))).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(ServiceException.class, () -> service.upload(file, "tenant-a", "uploader-a"));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<File> temporaryFile = ArgumentCaptor.forClass(File.class);
        verify(uploadStorage).uploadFile(temporaryFile.capture(), key.capture(), eq("application/octet-stream"));
        verify(uploadStorage).delete(key.getValue());
        assertFalse(temporaryFile.getValue().exists());
    }

    @Test
    void storageFailureDoesNotPublishAFile() {
        when(uploadStorage.uploadFile(any(File.class), anyString(), anyString()))
            .thenThrow(new IllegalStateException("MinIO unavailable"));
        assertThrows(ServiceException.class, () -> service.upload(file, "tenant-a", "uploader-a"));
        verifyNoInteractions(mapper);
    }

    @Test
    void readsUseTheOriginallyRecordedStorage() {
        VlsPublicFile published = new VlsPublicFile();
        published.setFileId("record-id");
        published.setStorageConfigKey("saved-minio");
        when(mapper.findPublishedFile("record-id")).thenReturn(published);
        ByteArrayInputStream input = new ByteArrayInputStream(payload);
        when(savedStorage.getObjectContent("public-files/record-id")).thenReturn(input);

        assertSame(published, service.find("record-id"));
        assertSame(input, service.open(published));
        service.checkExists(published);
        verify(savedStorage).getObjectMetadata("public-files/record-id");
        verifyNoInteractions(uploadStorage);
    }
}
