package com.ruoyi.workflow.domain.vo;

import lombok.Data;

@Data
public class WfDefAndDepVo {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义ID
     */
    private String definitionId;


    /**
     * 部署ID
     */
    private String deploymentId;
}
