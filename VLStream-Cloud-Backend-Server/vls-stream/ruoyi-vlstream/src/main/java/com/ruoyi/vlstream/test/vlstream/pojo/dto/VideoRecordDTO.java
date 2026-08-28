/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.VideoRecord;


/**
 * record data object
 *
 * @author Oort
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VideoRecordDTO extends VideoRecord {
	private static final long serialVersionUID = 1L;

}
