/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;


import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * workflow log object process_view_log
 *
 * @author lcq
 * @date 2025-08-15
 */
@Data
@ExcelIgnoreUnannotated
public class ProcessViewLogVo {

    private static final long serialVersionUID = 1L;

    /**
     * primary key
     */
    @ExcelProperty(value = "主键")
    private String id;

    /**
     * workflow instanceid (processInstanceId)
     */
    @ExcelProperty(value = "流程实例id", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "p=rocessInstanceId")
    private String processInstanceId;

    /**
     * workflow definition key (processKey)
     */
    @ExcelProperty(value = "流程定义 key", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "p=rocessKey")
    private String processKey;

    /**
     * user ID
     */
    @ExcelProperty(value = "访问者用户id")
    private String viewerUserId;

    /**
     * user /
     */
    @ExcelProperty(value = "访问者用户名/显示名")
    private String viewerUsername;

    /**
     * department ID
     */
    @ExcelProperty(value = "访问者部门id")
    private String viewerDeptId;

    /**
     * department name
     */
    @ExcelProperty(value = "访问者部门名称")
    private String viewerDeptName;

    /**
     * operation
     */
    @ExcelProperty(value = "操作类型")
    private String operationType;

    /**
     * workflow
     */
    @ExcelProperty(value = "流程状态")
    private String processStatus;

    /**
     *
     */
    @ExcelProperty(value = "访问时间")
    private Date viewTime;

    /**
     *
     */
    @ExcelProperty(value = "附件名称")
    private String attachmentName;

    private List<ProcessViewLogVo> ProcessViewList;
}
