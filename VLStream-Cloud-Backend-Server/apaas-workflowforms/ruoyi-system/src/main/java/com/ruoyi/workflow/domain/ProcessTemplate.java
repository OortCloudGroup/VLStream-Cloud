package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程初始化模版对象 process_template
 *
 * @author lcq
 * @date 2025-01-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("process_template")
public class ProcessTemplate extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 模板ID
     */
    @TableId(value = "id")
    private String id;
    /**
     * 部署id
     */
    private String deploymentId;
    /**
     * 模型id
     */
    private String modelId;
    /**
     * 模型Key
     */
    private String modelKey;
    /**
     * 模型名称
     */
    private String modelName;
    /**
     * 手机端是否显示 0（显示） 1（不显示）
     */
    private String showMobile;
    /**
     * 租户id
     */
    private String tenantId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 描述
     */
    private String description;
    /**
     * 删除标记，0表示未删除，1表示删除
     */
    @TableLogic
    private String delFlag;

}
