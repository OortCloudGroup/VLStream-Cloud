package com.ruoyi.workflow.convert.node;

import lombok.Data;

@Data
public class HeaderOrParams {
    private String key;
    private String keyType; // 1 表单， 2 固定
    private String value;
}
