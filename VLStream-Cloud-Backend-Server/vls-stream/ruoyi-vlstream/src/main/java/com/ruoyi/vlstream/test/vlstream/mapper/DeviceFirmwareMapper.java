/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceFirmware;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface DeviceFirmwareMapper extends BaseMapper<DeviceFirmware> {

	/** Physical removal is intentional because the immutable version key may be uploaded again after deletion. */
	@Delete("DELETE FROM vls_device_firmware WHERE id = #{id} AND tenant_id = #{tenantId}")
	int deletePermanently(@Param("id") Long id, @Param("tenantId") String tenantId);
}
