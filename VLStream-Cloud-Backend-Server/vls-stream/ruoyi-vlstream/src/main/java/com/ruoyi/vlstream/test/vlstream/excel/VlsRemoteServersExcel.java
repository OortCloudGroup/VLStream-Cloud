/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.excel;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;


/**
 * 远程服务器配置表 Excel实体类
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
	 * 服务器名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("服务器名称")
	private String serverName;
	/**
	 * 服务器IP地址
	 */
	@ColumnWidth(20)
	@ExcelProperty("服务器IP地址")
	private String serverIp;
	/**
	 * SSH端口
	 */
	@ColumnWidth(20)
	@ExcelProperty("SSH端口")
	private Integer serverPort;
	/**
	 * 用户名
	 */
	@ColumnWidth(20)
	@ExcelProperty("用户名")
	private String username;
	/**
	 * 密码(加密)
	 */
	@ColumnWidth(20)
	@ExcelProperty("密码(加密)")
	private String password;
	/**
	 * Conda环境名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("Conda环境名称")
	private String condaEnv;
	/**
	 * 工作目录
	 */
	@ColumnWidth(20)
	@ExcelProperty("工作目录")
	private String workDir;

}
