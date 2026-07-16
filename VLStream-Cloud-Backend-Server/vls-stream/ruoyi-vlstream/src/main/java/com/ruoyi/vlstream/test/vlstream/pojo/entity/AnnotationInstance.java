package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;

import java.math.BigDecimal;

/**
 * 标注实例实体类 实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_annotation_instance")
@Schema(description = "VlsAnnotationInstanceEntity对象")
@EqualsAndHashCode(callSuper = true)
public class AnnotationInstance extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 关联的标注项目ID
	 */
	@Schema(description = "关联的标注项目ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long annotationId;
	/**
	 * 标签ID
	 */
	@Schema(description = "标签ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long labelId;
	/**
	 * 图片id
	 */
	@Schema(description = "图片id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long imageId;
	/**
	 * 标注类型
	 */
	@Schema(description = "标注类型")
	private AlgorithmAnnotationTypeEnum annotationType;
	/**
	 * 标注坐标数据(JSON格式)
	 */
	@Schema(description = "标注坐标数据(JSON格式)")
	private String annotationData;
	/**
	 * 置信度
	 */
	@Schema(description = "置信度")
	private BigDecimal confidence;
	/**
	 * 是否已验证
	 */
	@Schema(description = "是否已验证")
	private Integer verified;

}
