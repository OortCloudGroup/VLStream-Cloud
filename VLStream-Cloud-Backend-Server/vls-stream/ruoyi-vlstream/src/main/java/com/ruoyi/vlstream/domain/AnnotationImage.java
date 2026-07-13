package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * VLS annotation image entity mapped to vls_annotation_image.
 */
@Data
@TableName("vls_annotation_image")
public class AnnotationImage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String tenantId;

    @TableField("annotation_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long annotationId;

    private String imageName;

    private String originalName;

    private String localPath;

    private Long fileSize;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastModified;

    private Integer isImported;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date importTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long createUser;

    private String createDept;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateUser;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    private Integer status;

    @TableLogic
    private Integer isDeleted;

    /**
     * Accept frontend status strings such as PENDING while storing the table integer status.
     */
    @JsonSetter("status")
    public void setStatus(Object status) {
        if (status instanceof Number) {
            this.status = ((Number) status).intValue();
            return;
        }
        if (status != null) {
            try {
                this.status = Integer.valueOf(String.valueOf(status));
                return;
            } catch (NumberFormatException ignored) {
                this.status = 1;
                return;
            }
        }
        this.status = null;
    }

    /**
     * Accept boolean or numeric import markers from frontend and source-compatible callers.
     */
    @JsonSetter("isImported")
    public void setIsImported(Object isImported) {
        if (isImported instanceof Boolean) {
            this.isImported = Boolean.TRUE.equals(isImported) ? 1 : 0;
            return;
        }
        if (isImported instanceof Number) {
            this.isImported = ((Number) isImported).intValue();
            return;
        }
        if (isImported != null) {
            String value = String.valueOf(isImported).trim();
            if ("true".equalsIgnoreCase(value)) {
                this.isImported = 1;
                return;
            }
            if ("false".equalsIgnoreCase(value)) {
                this.isImported = 0;
                return;
            }
            try {
                this.isImported = Integer.valueOf(value);
                return;
            } catch (NumberFormatException ignored) {
                this.isImported = 0;
                return;
            }
        }
        this.isImported = null;
    }
}
