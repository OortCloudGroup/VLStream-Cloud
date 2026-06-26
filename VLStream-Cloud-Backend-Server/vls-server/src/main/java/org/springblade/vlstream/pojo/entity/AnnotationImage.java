package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.util.Date;

/**
 * Annotation Image Info Table Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_annotation_image")
@Schema(description = "VlsAnnotationImageEntity object")
@EqualsAndHashCode(callSuper = true)
public class AnnotationImage extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Annotation Project ID
	 */
	@Schema(description = "Annotation Project ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long annotationId;
	/**
	 * Image name
	 */
	@Schema(description = "Image name")
	private String imageName;
	/**
	 * Original filename
	 */
	@Schema(description = "Original filename")
	private String originalName;
	/**
	 * Local Storage Path
	 */
	@Schema(description = "Local Storage Path")
	private String localPath;
	/**
	 * File size (bytes)
	 */
	@Schema(description = "File size (bytes)")
	private Long fileSize;
	/**
	 * Last Modified Time
	 */
	@Schema(description = "Last Modified Time")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date lastModified;
	/**
	 * Whether it is an imported image: 0-No, 1-Yes
	 */
	@Schema(description = "Whether it is an imported image: 0-No, 1-Yes")
	private Integer isImported;
	/**
	 * Import time
	 */
	@Schema(description = "Import time")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date importTime;

}
