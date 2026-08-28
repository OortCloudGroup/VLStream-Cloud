/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.demo.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * object test_demo
 *
 * @author Lion Li
 * @date 2021-07-26
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class TestDemoBo extends BaseEntity {

    /**
     * primary key
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * department ID
     */
    @NotNull(message = "部门id不能为空", groups = {AddGroup.class, EditGroup.class})
    private String deptId;

    /**
     * user ID
     */
    @NotNull(message = "用户id不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long userId;

    /**
     *
     */
    @NotNull(message = "排序号不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer orderNum;

    /**
     * key
     */
    @NotBlank(message = "key键不能为空", groups = {AddGroup.class, EditGroup.class})
    private String testKey;

    /**
     * value
     */
    @NotBlank(message = "值不能为空", groups = {AddGroup.class, EditGroup.class})
    private String value;

}
