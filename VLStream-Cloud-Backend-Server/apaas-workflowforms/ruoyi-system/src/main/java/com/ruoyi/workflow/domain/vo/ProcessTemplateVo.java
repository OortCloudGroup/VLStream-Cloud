package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;

/**
 * 流程初始化模版视图对象 process_template
 *
 * @author lcq
 * @date 2025-01-07
 */
@Data
@ExcelIgnoreUnannotated
public class ProcessTemplateVo {

    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    @ExcelProperty(value = "模板ID")
    private String id;

    /**
     * 部署id
     */
    @ExcelProperty(value = "部署id")
    private String deploymentId;

    /**
     * 模型id
     */
    @ExcelProperty(value = "模型id")
    private String modelId;

    /**
     * 模型Key
     */
    @ExcelProperty(value = "模型Key")
    private String modelKey;

    /**
     * 模型名称
     */
    @ExcelProperty(value = "模型名称")
    private String modelName;

    /**
     * 手机端是否显示 0（显示） 1（不显示）
     */
    @ExcelProperty(value = "手机端是否显示 0", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "显=示")
    private String showMobile;

    /**
     * 租户id
     */
    @ExcelProperty(value = "租户id")
    private String tenantId;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private String userId;

    /**
     * 描述
     */
    @ExcelProperty(value = "描述")
    private String description;


}
