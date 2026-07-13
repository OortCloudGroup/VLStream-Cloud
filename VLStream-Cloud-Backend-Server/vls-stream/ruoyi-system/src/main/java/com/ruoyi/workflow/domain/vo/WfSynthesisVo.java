package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 综合通用流程视图对象 wf_synthesis
 *
 * @author 雷超群
 * @date 2025-01-04
 */
@Data
@ExcelIgnoreUnannotated
public class WfSynthesisVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private String synthesisId;

    /**
     * 父节点ID
     */
    @ExcelProperty(value = "父节点ID")
    private String parentId;

    /**
     * 分类名称
     */
    @ExcelProperty(value = "分类名称")
    private String categoryName;


}
