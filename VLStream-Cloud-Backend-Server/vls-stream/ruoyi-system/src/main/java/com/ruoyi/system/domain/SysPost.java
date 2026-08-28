/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.domain;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * sys_post
 *
 * @author Lion Li
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_post")
@ExcelIgnoreUnannotated
public class SysPost extends BaseEntity {

    /**
     *
     */
    @ExcelProperty(value = "岗位序号")
    @TableId(value = "post_id")
    private Long postId;

    /**
     *
     */
    @ExcelProperty(value = "岗位编码")
    @NotBlank(message = "岗位编码不能为空")
    @Size(min = 0, max = 64, message = "岗位编码长度不能超过{max}个字符")
    private String postCode;

    /**
     *
     */
    @ExcelProperty(value = "岗位名称")
    @NotBlank(message = "岗位名称不能为空")
    @Size(min = 0, max = 50, message = "岗位名称长度不能超过{max}个字符")
    private String postName;

    /**
     *
     */
    @ExcelProperty(value = "岗位排序")
    @NotNull(message = "显示顺序不能为空")
    private Integer postSort;

    /**
     * Status (0 normal 1 disabled)
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

    /**
     * remark
     */
    private String remark;

    /**
     * userwhether in in
     */
    @TableField(exist = false)
    private boolean flag = false;

}
