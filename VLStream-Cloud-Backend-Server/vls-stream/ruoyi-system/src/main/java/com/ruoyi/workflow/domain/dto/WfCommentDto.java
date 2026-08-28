/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author KonBAI
 * @createTime 2022/3/10 00:12
 */
@Data
@Builder
public class WfCommentDto implements Serializable {

    /**
     * 0 1 2
     */
    private String type;

    /**
     *
     */
    private String comment;
}
