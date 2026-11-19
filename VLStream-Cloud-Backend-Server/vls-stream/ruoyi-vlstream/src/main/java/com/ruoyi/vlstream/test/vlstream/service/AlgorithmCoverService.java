package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.data.DataMediaStorage;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Persist object references separately from expiring browser preview URLs. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlgorithmCoverService {
    private static final String PREFIX = "minio://algorithm-covers/";
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private final VlsAlgorithmMapper algorithms;
    private final DataMediaStorage storage;

    public Algorithm upload(Long algorithmId, MultipartFile file) throws IOException {
        Algorithm algorithm = algorithms.selectOne(new QueryWrapper<Algorithm>()
            .eq("id", algorithmId).eq("tenant_id", tenant()).eq("is_deleted", 0));
        if (algorithm == null) throw new ServiceException("算法不存在或无权访问");
        if (file == null || file.isEmpty()) throw new ServiceException("请选择图片");
        if (file.getSize() > MAX_SIZE) throw new ServiceException("图片大小不能超过 5MB");
        byte[] bytes = file.getBytes();
        String suffix = imageSuffix(bytes, file.getContentType());
        String key = "algorithm-covers/" + tenant() + "/" + algorithmId + "/" + UUID.randomUUID() + suffix;
        storage.put(key, bytes, file.getContentType());
        Algorithm result = new Algorithm();
        result.setId(algorithmId);
        result.setImageUrl("minio://" + key);
        try {
            result.setImagePreviewUrl(storage.preview(key));
        } catch (RuntimeException error) {
            removeUploadedObject(key);
            throw error;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) removeUploadedObject(key);
                }
            });
        }
        return result;
    }

    private void removeUploadedObject(String key) {
        try {
            storage.remove(key);
        } catch (RuntimeException error) {
            log.warn("Failed to clean up algorithm cover {}", key, error);
        }
    }

    public void validate(Algorithm algorithm) {
        String reference = algorithm.getImageUrl();
        if (reference == null || !reference.startsWith("minio://")) return;
        String expected = PREFIX + tenant() + "/" + algorithm.getId() + "/";
        if (!reference.startsWith(expected)
            || !reference.substring(expected.length()).matches("[0-9a-f-]{36}\\.(jpg|png|webp)")) {
            throw new ServiceException("算法图片引用无效或无权访问");
        }
    }

    public void prepare(Algorithm algorithm) {
        if (algorithm == null) return;
        algorithm.setImagePreviewUrl(null);
        String reference = algorithm.getImageUrl();
        if (reference == null || !reference.startsWith("minio://")) return;
        try {
            validate(algorithm);
            algorithm.setImagePreviewUrl(storage.preview(reference.substring("minio://".length())));
        } catch (RuntimeException error) {
            // Storage availability must not hide the algorithm or its edit form.
            log.warn("Algorithm cover preview unavailable for algorithm {}", algorithm.getId());
        }
    }

    private String tenant() {
        String tenant = TenantContextHolder.getTenantId();
        if (tenant == null || !tenant.matches("[A-Za-z0-9_-]+")) throw new ServiceException("缺少有效租户上下文");
        return tenant;
    }

    private String imageSuffix(byte[] bytes, String contentType) {
        if ("image/jpeg".equals(contentType) && bytes.length >= 3
            && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return ".jpg";
        if ("image/png".equals(contentType) && bytes.length >= 8
            && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71
            && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) return ".png";
        if ("image/webp".equals(contentType) && bytes.length >= 12
            && "RIFF".equals(new String(bytes, 0, 4, StandardCharsets.US_ASCII))
            && "WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII))) return ".webp";
        throw new ServiceException("图片格式仅支持 JPG、PNG 或 WEBP，请检查文件内容");
    }
}
