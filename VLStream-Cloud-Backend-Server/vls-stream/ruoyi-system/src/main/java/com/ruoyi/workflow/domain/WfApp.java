package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用通用流程对象 wf_app
 *
 * @author 雷超群
 * @date 2025-01-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_app")
public class WfApp extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @TableId(value = "app_id")
    private String  appId;
    /**
     * 应用名称
     */
    private String applicationName;
    /**
     * 应用ID
     */
    private String applicationId;
    /**
     * 应用密钥
     */
    private String applicationSecret;
    /**
     * 租户id
     */
    private String tenantId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 删除标记，0表示未删除，1表示删除
     */
    @TableLogic
    private String delFlag;
    /**
     * 0选择应用，1添加应用
     */
    private String appFlag;

    /**
     * 图标地址
     */
    private String images;
    /**
     * 应用包名
     */
    private String appPackage;
}
