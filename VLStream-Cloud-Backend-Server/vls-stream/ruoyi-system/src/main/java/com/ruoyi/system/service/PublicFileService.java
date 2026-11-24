package com.ruoyi.system.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.system.domain.VlsPublicFile;
import com.ruoyi.system.mapper.VlsPublicFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicFileService {

    public static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final VlsPublicFileMapper mapper;

    public VlsPublicFile upload(MultipartFile file, String tenantId, String uploaderId) {
        VlsPublicFile record = new VlsPublicFile();
        record.setFileId(UUID.randomUUID().toString());
        record.setTenantId(tenantId);
        record.setUploaderId(uploaderId);
        record.setOriginalName(safeFileName(file.getOriginalFilename()));
        record.setFileSize(file.getSize());

        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile("vls-public-file-", ".upload");
            file.transferTo(temporaryFile);
            OssClient storage = storage(null);
            record.setStorageConfigKey(storage.getConfigKey());
            String key = objectKey(record);
            try {
                storage.uploadFile(temporaryFile.toFile(), key, MediaType.APPLICATION_OCTET_STREAM_VALUE);
                if (mapper.insert(record) != 1) {
                    throw new IllegalStateException("Public file record was not inserted");
                }
            } catch (RuntimeException failure) {
                try {
                    storage.delete(key);
                } catch (RuntimeException cleanupFailure) {
                    log.warn("Could not clean up MinIO object for public file {}", record.getFileId(), cleanupFailure);
                }
                throw failure;
            }
            return record;
        } catch (IOException | RuntimeException failure) {
            log.error("Could not publish file {}", record.getFileId(), failure);
            throw new ServiceException("文件上传失败，请稍后重试");
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException cleanupFailure) {
                    log.warn("Could not clean up temporary file for public file {}", record.getFileId(), cleanupFailure);
                }
            }
        }
    }

    public VlsPublicFile find(String fileId) {
        return mapper.findPublishedFile(fileId);
    }

    public InputStream open(VlsPublicFile file) {
        return storage(file.getStorageConfigKey()).getObjectContent(objectKey(file));
    }

    public void checkExists(VlsPublicFile file) {
        storage(file.getStorageConfigKey()).getObjectMetadata(objectKey(file));
    }

    protected OssClient storage(String configKey) {
        return configKey == null ? OssFactory.instance() : OssFactory.instance(configKey);
    }

    private static String objectKey(VlsPublicFile file) {
        return "public-files/" + file.getFileId();
    }

    private static String safeFileName(String originalName) {
        if (originalName == null) {
            return "download";
        }
        String name = originalName.substring(Math.max(originalName.lastIndexOf('/'), originalName.lastIndexOf('\\')) + 1)
            .replaceAll("[\\p{Cntrl}\\p{Cf}]", "").trim();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            return "download";
        }
        return name.substring(0, name.offsetByCodePoints(0, Math.min(name.codePointCount(0, name.length()), 255)));
    }
}
