package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 流程访问日志业务对象 process_view_log
 *
 * @author lcq
 * @date 2025-08-15
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessViewLogBo extends BaseEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 流程实例id（processInstanceId）
     */
    private String processInstanceId;

    /**
     * 流程定义 key（processKey）
     */
    private String processKey;

    /**
     * 访问者用户id
     */
    private String viewerUserId;

    /**
     * 访问者用户名/显示名
     */
    private String viewerUsername;

    /**
     * 访问者部门id
     */
    private String viewerDeptId;

    /**
     * 访问者部门名称
     */
    private String viewerDeptName;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 流程状态
     */
    private String processStatus;

    /**
     * 访问时间
     */
    private Date viewTime;

    /**
     * 附件名称
     */
    private String attachmentName;

    /**
     * 租户id
     */
    private String tenantId;

    /**
     * 删除标记，0表示未删除，1表示删除
     */
    private String delFlag;


}
