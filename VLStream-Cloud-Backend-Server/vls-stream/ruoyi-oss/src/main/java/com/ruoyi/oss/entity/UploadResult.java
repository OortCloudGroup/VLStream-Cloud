/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.oss.entity;

import lombok.Builder;
import lombok.Data;

/**
 *
 *
 * @author Lion Li
 */
@Data
@Builder
public class UploadResult {

    /**
     *
     */
    private String url;

    /**
     *
     */
    private String filename;
}
