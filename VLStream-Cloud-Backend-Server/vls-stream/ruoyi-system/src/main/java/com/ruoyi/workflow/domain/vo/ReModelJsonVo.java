/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;


import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;



/**
 * workflow JSON object re_mode_json
 *
 * @author
 * @date 2024-11-02
 */
@Data
@ExcelIgnoreUnannotated
public class ReModelJsonVo {

    private static final long serialVersionUID = 1L;

    /**
     * and act_re_model ID
     */
    @ExcelProperty(value = "与act_re_model 表的关联ID")
    private String modelId;

    /**
     * id
     */
    @ExcelProperty(value = "租户id")
    private String tenantId;

    /**
     * user ID
     */
    @ExcelProperty(value = "用户id")
    private String userId;

    /**
     * workflow JSON
     */
    @ExcelProperty(value = "流程图JSON")
    private String jsonContent;


}
