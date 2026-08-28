/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TreeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * department sys_dept
 *
 * @author Lion Li
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDeptView extends TreeEntity<SysDeptView> {
    private static final long serialVersionUID = 1L;

    /**
     * department ID
     */
    @TableId(value = "dept_id")
    private String  deptId;

    /**
     * tenant ID
     */
    private String tenantId;

    /**
     * departmentcode
     */
    @TableField("oort_dcode")
    private String deptCode;

    /**
     * department name
     */
    @NotBlank(message = "部门名称不能为空")
    @Size(min = 0, max = 30, message = "部门名称长度不能超过{max}个字符")
    private String deptName;

    /**
     *
     */
    @NotNull(message = "显示顺序不能为空")
    private Integer orderNum;

    /**
     *
     */
    private String leader;

    /**
     * department
     */
    private String parentId;

    /**
     *
     */
    @Size(min = 0, max = 11, message = "联系电话长度不能超过{max}个字符")
    private String phone;

    /**
     *
     */
    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过{max}个字符")
    private String email;

    /**
     * department :0 ,1
     */
    private String status;

    /**
     * Delete (0represents in 2represents Delete )
     */
    @TableLogic
    private String delFlag;

    /**
     *
     */
    private String ancestors;

    /**
     * creator
     */
 //   private String createBy;

    /**
     * create time
     */
   // private Date createTime;

    /**
     * updater
     */
    //private String updateBy;

    /**
     * update time
     */
   // private Date updateTime;
}
