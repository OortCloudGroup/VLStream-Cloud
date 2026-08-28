/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
// * node (only property)
// */
//@EqualsAndHashCode(callSuper = true)
//@Data
//public abstract class AssigneeNode extends Node {
// // approvalobject
//    private AssigneeTypeEnum assigneeType;
// // form
//    private String formUser;
// // form role
//    private String formRole;
// // approver
//    private List<String> users;
// // approverrole
//    private List<String> roles;
// // main
//    private Integer leader;
// // main
//    private Integer orgLeader;
// // : true- , false-
//    private Boolean choice;
// //
//    private Boolean self;
//
//
//    public abstract List<FlowElement> convert();
//}
