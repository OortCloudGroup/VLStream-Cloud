package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * Remote Server Configuration Table Excel Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsRemoteServersExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Server Name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Server Name")
	private String serverName;
	/**
	 * Server IP Address
	 */
	@ColumnWidth(20)
	@ExcelProperty("Server IP Address")
	private String serverIp;
	/**
	 * SSH port
	 */
	@ColumnWidth(20)
	@ExcelProperty("SSH port")
	private Integer serverPort;
	/**
	 * Username
	 */
	@ColumnWidth(20)
	@ExcelProperty("Username")
	private String username;
	/**
	 * Password (encrypted)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Password (encrypted)")
	private String password;
	/**
	 * Conda environment name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Conda environment name")
	private String condaEnv;
	/**
	 * Working directory
	 */
	@ColumnWidth(20)
	@ExcelProperty("Working directory")
	private String workDir;

}
