package com.ruoyi.vlstream.test.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AudioAnomalyDetectionSetting;


/**
 * 音频异常侦测设置表 视图实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AudioAnomalyDetectionSettingVO extends AudioAnomalyDetectionSetting {
	private static final long serialVersionUID = 1L;
}
