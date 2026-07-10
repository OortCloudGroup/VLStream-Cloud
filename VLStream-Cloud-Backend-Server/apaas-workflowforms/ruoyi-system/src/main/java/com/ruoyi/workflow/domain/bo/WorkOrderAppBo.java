package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用工单分类业务对象 workorder_app
 *
 * @author 雷超群
 * @date 2025-01-04
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderAppBo extends BaseEntity {

    /**
     * 主键ID
     */
    private String appId;

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
     * 用户id
     */
    private String userId;

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
