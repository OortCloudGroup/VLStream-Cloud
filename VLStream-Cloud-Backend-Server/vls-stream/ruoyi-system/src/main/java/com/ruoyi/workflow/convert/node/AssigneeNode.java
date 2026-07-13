/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

//package com.ruoyi.workflow.convert.node;
//
//
//import com.ruoyi.workflow.convert.enums.AssigneeTypeEnum;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import org.flowable.bpmn.model.FlowElement;
//
//import java.util.List;
//
///**
// * 经办人节点（只有属性）
// */
//@EqualsAndHashCode(callSuper = true)
//@Data
//public abstract class AssigneeNode extends Node {
//    // 审批对象
//    private AssigneeTypeEnum assigneeType;
//    // 表单内人员
//    private String formUser;
//    // 表单内角色
//    private String formRole;
//    // 审批人
//    private List<String> users;
//    // 审批人角色
//    private List<String> roles;
//    // 主管
//    private Integer leader;
//    // 组织主管
//    private Integer orgLeader;
//    // 发起人自选：true-单选，false-多选
//    private Boolean choice;
//    // 发起人自己
//    private Boolean self;
//
//
//    public abstract List<FlowElement> convert();
//}
