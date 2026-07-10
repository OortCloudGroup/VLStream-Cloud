package com.ruoyi.workflow.domain.vo;


import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;



/**
 * 流程图JSON视图对象 re_mode_json
 *
 * @author 雷超群
 * @date 2024-11-02
 */
@Data
@ExcelIgnoreUnannotated
public class ReModelJsonVo {

    private static final long serialVersionUID = 1L;

    /**
     * 与act_re_model 表的关联ID
     */
    @ExcelProperty(value = "与act_re_model 表的关联ID")
    private String modelId;

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
     * 流程图JSON
     */
    @ExcelProperty(value = "流程图JSON")
    private String jsonContent;


}
