package com.vlstream.service.impl;

import com.vlstream.dto.FileResponseDto;
import com.vlstream.entity.AnnotationImage;
import com.vlstream.entity.AlgorithmAnnotation;
import com.vlstream.mapper.AlgorithmAnnotationMapper;
import com.vlstream.mapper.AnnotationImageMapper;
import com.vlstream.service.AnnotationImageService;
import com.vlstream.service.AlgorithmAnnotationService;
import com.vlstream.service.IFileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AnnotationImageServiceImpl implements AnnotationImageService {

    @Autowired
    private AnnotationImageMapper annotationImageMapper;

    @Autowired
    private IFileUploadService fileUploadService;

    @Autowired
    private AlgorithmAnnotationMapper algorithmAnnotationMapper;

    @Override
    public List<AnnotationImage> uploadImages(MultipartFile[] files, Long annotationId) {
        List<AnnotationImage> uploadedImages = new ArrayList<>();
        int addedCount = 0;

        for (MultipartFile file : files) {
            try {
                FileResponseDto fileResponse = fileUploadService.uploadFile("818301f0e77f4cd8a117414cbeb32d9e", "5f0de11687d744bc95e84e207d319493", fileUploadService.multipartFileToFile(file));

                String originalName = file.getOriginalFilename();
                if (originalName == null || originalName.trim().isEmpty()) {
                    throw new RuntimeException("File name cannot be empty");
                }
                originalName = Paths.get(originalName).getFileName().toString();
                originalName = originalName.replace("\\", "_")
                        .replace("/", "_")
                        .replaceAll("[<>:\"|?*]", "_");

                String fileName = System.currentTimeMillis() + "_" + originalName;

                AnnotationImage annotationImage = new AnnotationImage();
                annotationImage.setAnnotationId(annotationId);
                annotationImage.setImageName(fileName);
                annotationImage.setOriginalName(originalName);
                annotationImage.setLocalPath(fileResponse.getUrl());
                annotationImage.setFileSize(file.getSize());
                annotationImage.setIsImported(true);
                annotationImage.setImportTime(LocalDateTime.now());
                annotationImage.setDeleted(false);
                annotationImage.setCreateTime(LocalDateTime.now());
                annotationImage.setUpdateTime(LocalDateTime.now());

                annotationImageMapper.insert(annotationImage);
                uploadedImages.add(annotationImage);
                addedCount++;

            } catch (Exception e) {
                e.printStackTrace();
                log.error("Failed to save annotation image: annotationId={}, file={}", annotationId, file != null ? file.getOriginalFilename() : "null", e);
                throw new RuntimeException("File upload failed: " + e.getMessage(), e);
            }
        }

        // Update annotation totalCount
        try {
            if (addedCount > 0) {
                AlgorithmAnnotation annotation = algorithmAnnotationMapper.selectById(annotationId);
                if (annotation != null) {
                    int currentTotal = annotation.getTotalCount() == null ? 0 : annotation.getTotalCount();
                    annotation.setTotalCount(currentTotal + addedCount);
                    algorithmAnnotationMapper.updateById(annotation);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update annotation total count after uploading images: annotationId={}, error={}", annotationId, e.getMessage());
        }

        return uploadedImages;
    }

    @Override
    public List<AnnotationImage> getImagesByDataset(Long annotationId) {
        return annotationImageMapper.selectByDatasetId(annotationId);
    }

    @Override
    public AnnotationImage getImageById(Long id) {
        return annotationImageMapper.selectById(id);
    }

    @Override
    public AnnotationImage updateImage(AnnotationImage image) {
        image.setUpdateTime(LocalDateTime.now());
        annotationImageMapper.updateById(image);
        return annotationImageMapper.selectById(image.getId());
    }

    @Override
    public void deleteImage(Long id) {
        AnnotationImage image = annotationImageMapper.selectById(id);
        if (image != null) {
            File file = new File(image.getLocalPath());
            if (file.exists()) {
                file.delete();
            }
            annotationImageMapper.deleteById(id);
        }
    }

    @Override
    public void batchDeleteImages(List<Long> ids) {
        for (Long id : ids) {
            deleteImage(id);
        }
    }

    @Override
    public Map<String, Object> getDatasetStats(Long datasetId) {
        List<AnnotationImage> images = getImagesByDataset(datasetId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalImages", images.size());

        Map<String, Long> importStats = new HashMap<>();
        importStats.put("IMPORTED", images.stream().filter(img -> Boolean.TRUE.equals(img.getIsImported())).count());
        importStats.put("NOT_IMPORTED", images.stream().filter(img -> !Boolean.TRUE.equals(img.getIsImported())).count());
        stats.put("importStats", importStats);

        long totalSize = images.stream().mapToLong(img -> img.getFileSize() != null ? img.getFileSize() : 0).sum();
        stats.put("totalSize", totalSize);

        return stats;
    }

    @Override
    public boolean saveImage(AnnotationImage annotationImage) {
        try {
            if (annotationImage.getImageName() == null || annotationImage.getImageName().trim().isEmpty()) {
                throw new IllegalArgumentException("File name cannot be empty");
            }
            if (annotationImage.getAnnotationId() == null) {
                throw new IllegalArgumentException("Dataset ID cannot be empty");
            }

            if (annotationImage.getCreateTime() == null) {
                annotationImage.setCreateTime(LocalDateTime.now());
            }
            if (annotationImage.getUpdateTime() == null) {
                annotationImage.setUpdateTime(LocalDateTime.now());
            }
            if (annotationImage.getIsImported() == null) {
                annotationImage.setIsImported(true);
            }
            if (annotationImage.getDeleted() == null) {
                annotationImage.setDeleted(false);
            }

            if (annotationImage.getOriginalName() == null || annotationImage.getOriginalName().trim().isEmpty()) {
                annotationImage.setOriginalName(annotationImage.getImageName());
            }

            log.info("Saving annotation image: datasetId={}, fileName={}",
                    annotationImage.getAnnotationId(), annotationImage.getImageName());

            annotationImageMapper.insert(annotationImage);
            return true;
        } catch (Exception e) {
            log.error("Failed to save annotation image: datasetId={}, fileName={}",
                    annotationImage.getAnnotationId(), annotationImage.getImageName(), e);
            throw new RuntimeException("Failed to save annotation image: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean batchSaveImages(List<AnnotationImage> annotationImages) {
        try {
            LocalDateTime now = LocalDateTime.now();

            for (AnnotationImage image : annotationImages) {
                if (image.getCreateTime() == null) {
                    image.setCreateTime(now);
                }
                if (image.getUpdateTime() == null) {
                    image.setUpdateTime(now);
                }
                if (image.getIsImported() == null) {
                    image.setIsImported(true);
                }
                if (image.getDeleted() == null) {
                    image.setDeleted(false);
                }

                annotationImageMapper.insert(image);
            }

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to batch save annotation images: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AnnotationImage> getImagesByAnnotationId(Long annotationId) {
        return annotationImageMapper.selectByAnnotationId(annotationId);
    }
}
