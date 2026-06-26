/**
 * Copyright (c) 2018-2099, Chill Zhuang (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.modules.system.excel;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * UserDTO
 *
 * @author Chill
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class UserExcel implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	@ColumnWidth(15)
	@ExcelProperty("Tenant number")
	private String tenantId;

	@ColumnWidth(15)
	@ExcelProperty("Account")
	private String account;

	@ColumnWidth(10)
	@ExcelProperty("Nickname")
	private String name;

	@ColumnWidth(10)
	@ExcelProperty("Name")
	private String realName;

	@ExcelProperty("Email")
	private String email;

	@ColumnWidth(15)
	@ExcelProperty("Mobile phone")
	private String phone;

	@ExcelIgnore
	@ExcelProperty("Role ID")
	private String roleId;

	@ExcelIgnore
	@ExcelProperty("Department ID")
	private String deptId;

	@ExcelIgnore
	@ExcelProperty("Job position ID")
	private String postId;

	@ExcelProperty("Role Name")
	private String roleName;

	@ExcelProperty("Department name")
	private String deptName;

	@ExcelProperty("Job position name")
	private String postName;

	@ColumnWidth(20)
	@ExcelProperty("Birthday")
	private Date birthday;

}
