package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TimeStrategy;


/**
 * 时间策略表 数据传输对象实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TimeStrategyDTO extends TimeStrategy {
	private static final long serialVersionUID = 1L;

}
