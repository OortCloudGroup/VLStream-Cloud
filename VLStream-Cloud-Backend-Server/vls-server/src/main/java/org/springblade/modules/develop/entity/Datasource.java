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
package org.springblade.modules.develop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.BaseEntity;

import java.io.Serial;

/**
 * Data source configuration table entity class
 *
 * @author Chill
 */
@Data
@TableName("blade_datasource")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Datasource object")
public class Datasource extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Primary key
	 */
	@Schema(description = "Primary key")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/**
	 * Name
	 */
	@Schema(description = "Name")
	private String name;
	/**
	 * Driver class
	 */
	@Schema(description = "Driver class")
	private String driverClass;
	/**
	 * Connection address
	 */
	@Schema(description = "Connection address")
	private String url;
	/**
	 * Username
	 */
	@Schema(description = "Username")
	private String username;
	/**
	 * Password
	 */
	@Schema(description = "Password")
	private String password;
	/**
	 * Remarks
	 */
	@Schema(description = "Remarks")
	private String remark;


}
