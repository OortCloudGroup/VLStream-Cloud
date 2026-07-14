/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.common.constant;

/**
 * 流程常量信息
 *
 * @author Xuan xuan
 * @date 2021/4/17 22:46
 */
public class ProcessConstants {

    public static final String SUFFIX = ".bpmn";

    /**
     * 动态数据
     */
    public static final String DATA_TYPE = "dynamic";

    /**
     * 单个审批人
     */
    public static final String USER_TYPE_ASSIGNEE = "assignee";


    /**
     * 候选人
     */
    public static final String USER_TYPE_USERS = "candidateUsers";


    /**
     * 审批组
     */
    public static final String USER_TYPE_ROUPS = "candidateGroups";

    /**
     * 单个审批人
     */
    public static final String PROCESS_APPROVAL = "approval";

    /**
     * 会签人员
     */
    public static final String PROCESS_MULTI_INSTANCE_USER = "userList";

    /**
     * nameapace
     */
    public static final String NAMASPASE = "http://flowable.org/bpmn";

    /**
     * 会签节点
     */
    public static final String PROCESS_MULTI_INSTANCE = "multiInstance";

    /**
     * 自定义属性 dataType
     */
    public static final String PROCESS_CUSTOM_DATA_TYPE = "dataType";

    /**
     * 自定义属性 userType
     */
    public static final String PROCESS_CUSTOM_USER_TYPE = "userType";

    /**
     * 自定义属性 localScope
     */
    public static final String PROCESS_FORM_LOCAL_SCOPE = "localScope";

    /**
     * 自定义属性 流程状态
     */
    public static final String PROCESS_STATUS_KEY = "processStatus";


    /**
     * 流程跳过
     */
    public static final String FLOWABLE_SKIP_EXPRESSION_ENABLED = "_FLOWABLE_SKIP_EXPRESSION_ENABLED";

//    /**
//     * 应用工单类型
//     */
//    public static final String WORK_ORDER_APP = "workOrderApp";
//
//    /**
//     * 综合工单类型
//     */
//    public static final String WORK_ORDER_SYNTHESIS = "workOrderSynthesis";
//    /**
//     * 应用通用类型
//     */
//    public static final String WF_APP = "wfApp";
//    /**
//     * 综合通用类型
//     */
//    public static final String WF_SYNTHESIS = "wfSynthesis";


}
