package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Annotation Image Entity Class
 */
@Data
@TableName("annotation_image")
public class AnnotationImage {
    
    private Long id;
    private Long annotationId;
    private String imageName;
    private String originalName;
    private String localPath;
    private Long fileSize;
    private Boolean isImported;
    private LocalDateTime importTime;
    private Boolean deleted;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("created_time")
    private LocalDateTime createTime;   // Corresponding to the created_time field in the database

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("updated_time")
    private LocalDateTime updateTime;   // Corresponding to the updated_time field in the database

    private LocalDateTime lastModified; // Last modified time

    // Constructor
    public AnnotationImage() {}

    public AnnotationImage(Long datasetId, String fileName, String originalName, String filePath) {
        this.annotationId = datasetId;
        this.imageName = fileName;
        this.originalName = originalName;
        this.localPath = filePath;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.isImported = true;
        this.importTime = LocalDateTime.now();
        this.deleted = false;
    }

}