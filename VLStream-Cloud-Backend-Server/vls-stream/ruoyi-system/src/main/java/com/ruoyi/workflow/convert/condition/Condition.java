package com.ruoyi.workflow.convert.condition;

import lombok.Data;

/**
 * 筛选条件
 */
@Data
public class Condition {
    private String field;
    private String operator;
    private Object value;
}
