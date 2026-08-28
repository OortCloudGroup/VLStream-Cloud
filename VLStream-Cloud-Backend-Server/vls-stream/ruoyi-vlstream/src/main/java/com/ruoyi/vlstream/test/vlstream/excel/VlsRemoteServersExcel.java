/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.excel;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;


/**
 * service configuration Excel
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsRemoteServersExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * service
	 */
	@ColumnWidth(20)
	@ExcelProperty("服务器名称")
	private String serverName;
	/**
	 * service IP
	 */
	@ColumnWidth(20)
	@ExcelProperty("服务器IP地址")
	private String serverIp;
	/**
	 * SSH
	 */
	@ColumnWidth(20)
	@ExcelProperty("SSH端口")
	private Integer serverPort;
	/**
	 * user
	 */
	@ColumnWidth(20)
	@ExcelProperty("用户名")
	private String username;
	/**
	 * ( )
	 */
	@ColumnWidth(20)
	@ExcelProperty("密码(加密)")
	private String password;
	/**
	 * Conda
	 */
	@ColumnWidth(20)
	@ExcelProperty("Conda环境名称")
	private String condaEnv;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("工作目录")
	private String workDir;

}
