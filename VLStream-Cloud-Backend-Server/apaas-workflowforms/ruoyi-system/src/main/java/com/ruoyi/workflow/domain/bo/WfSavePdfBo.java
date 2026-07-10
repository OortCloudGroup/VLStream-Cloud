package com.ruoyi.workflow.domain.bo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程pdf上传
 */
@Data
public class WfSavePdfBo {

    /**
     * ID
     */
    private Long id;

    /**
     * 任务Id
     */
    private String taskId;

    /**
     * 流程实例Id
     */
    private String procInsId;

    /**
     * 附件链接
     */
    private String attachmentLink;

    /**
     * 是否签名
     */
    private String isSignature;
}
