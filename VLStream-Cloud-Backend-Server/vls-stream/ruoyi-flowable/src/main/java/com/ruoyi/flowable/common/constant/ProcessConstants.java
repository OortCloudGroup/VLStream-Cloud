/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.flowable.common.constant;

/**
 * workflow info
 *
 * @author Xuan xuan
 * @date 2021/4/17 22:46
 */
public class ProcessConstants {

    public static final String SUFFIX = ".bpmn";

    /**
     * data
     */
    public static final String DATA_TYPE = "dynamic";

    /**
     * approver
     */
    public static final String USER_TYPE_ASSIGNEE = "assignee";


    /**
     * candidate user
     */
    public static final String USER_TYPE_USERS = "candidateUsers";


    /**
     * approval
     */
    public static final String USER_TYPE_ROUPS = "candidateGroups";

    /**
     * approver
     */
    public static final String PROCESS_APPROVAL = "approval";

    /**
     * will
     */
    public static final String PROCESS_MULTI_INSTANCE_USER = "userList";

    /**
     * nameapace
     */
    public static final String NAMASPASE = "http://flowable.org/bpmn";

    /**
     * will node
     */
    public static final String PROCESS_MULTI_INSTANCE = "multiInstance";

    /**
     * Customproperty dataType
     */
    public static final String PROCESS_CUSTOM_DATA_TYPE = "dataType";

    /**
     * Customproperty userType
     */
    public static final String PROCESS_CUSTOM_USER_TYPE = "userType";

    /**
     * Customproperty localScope
     */
    public static final String PROCESS_FORM_LOCAL_SCOPE = "localScope";

    /**
     * Customproperty workflow
     */
    public static final String PROCESS_STATUS_KEY = "processStatus";


    /**
     * workflow
     */
    public static final String FLOWABLE_SKIP_EXPRESSION_ENABLED = "_FLOWABLE_SKIP_EXPRESSION_ENABLED";

//    /**
// * work order
//     */
//    public static final String WORK_ORDER_APP = "workOrderApp";
//
//    /**
// * work order
//     */
//    public static final String WORK_ORDER_SYNTHESIS = "workOrderSynthesis";
//    /**
// *
//     */
//    public static final String WF_APP = "wfApp";
//    /**
// *
//     */
//    public static final String WF_SYNTHESIS = "wfSynthesis";


}
