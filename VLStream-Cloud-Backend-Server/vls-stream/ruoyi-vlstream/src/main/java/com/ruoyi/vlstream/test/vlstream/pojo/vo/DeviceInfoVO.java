package com.ruoyi.vlstream.test.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;


/**
 * 设备信息表 视图实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceInfoVO extends DeviceInfo {
	private static final long serialVersionUID = 1L;

	private String algorithmName;

}
