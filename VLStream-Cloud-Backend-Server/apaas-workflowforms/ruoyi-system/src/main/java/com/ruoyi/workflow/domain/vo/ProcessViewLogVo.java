package com.ruoyi.workflow.domain.vo;


import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 流程访问日志视图对象 process_view_log
 *
 * @author lcq
 * @date 2025-08-15
 */
@Data
@ExcelIgnoreUnannotated
public class ProcessViewLogVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private String id;

    /**
     * 流程实例id（processInstanceId）
     */
    @ExcelProperty(value = "流程实例id", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "p=rocessInstanceId")
    private String processInstanceId;

    /**
     * 流程定义 key（processKey）
     */
    @ExcelProperty(value = "流程定义 key", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "p=rocessKey")
    private String processKey;

    /**
     * 访问者用户id
     */
    @ExcelProperty(value = "访问者用户id")
    private String viewerUserId;

    /**
     * 访问者用户名/显示名
     */
    @ExcelProperty(value = "访问者用户名/显示名")
    private String viewerUsername;

    /**
     * 访问者部门id
     */
    @ExcelProperty(value = "访问者部门id")
    private String viewerDeptId;

    /**
     * 访问者部门名称
     */
    @ExcelProperty(value = "访问者部门名称")
    private String viewerDeptName;

    /**
     * 操作类型
     */
    @ExcelProperty(value = "操作类型")
    private String operationType;

    /**
     * 流程状态
     */
    @ExcelProperty(value = "流程状态")
    private String processStatus;

    /**
     * 访问时间
     */
    @ExcelProperty(value = "访问时间")
    private Date viewTime;

    /**
     * 附件名称
     */
    @ExcelProperty(value = "附件名称")
    private String attachmentName;

    private List<ProcessViewLogVo> ProcessViewList;
}
