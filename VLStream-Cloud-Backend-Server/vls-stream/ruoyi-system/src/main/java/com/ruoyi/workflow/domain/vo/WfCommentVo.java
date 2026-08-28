/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * workflow object
 *
 * @author konbai
 * @createTime 2022/4/4 02:03
 */
@Data
public class WfCommentVo {

    /**
     * approval
     */
    private String type;

    /**
     *
     */
    private String message;

    /**
     *
     */
    private Date time;


}
