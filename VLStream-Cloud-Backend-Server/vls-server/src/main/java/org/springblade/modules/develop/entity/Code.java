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
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Entity class
 *
 * @author Chill
 */
@Data
@TableName("blade_code")
@Schema(description = "Code object")
public class Code implements Serializable {

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
	 * Data source primary key
	 */
	@Schema(description = "Data source primary key")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long datasourceId;

	/**
	 * Module name
	 */
	@Schema(description = "Service Name")
	private String serviceName;

	/**
	 * Module name
	 */
	@Schema(description = "Module name")
	private String codeName;

	/**
	 * Table name
	 */
	@Schema(description = "Table name")
	private String tableName;

	/**
	 * Entity name
	 */
	@Schema(description = "Table prefix")
	private String tablePrefix;

	/**
	 * Primary key name
	 */
	@Schema(description = "Primary key name")
	private String pkName;

	/**
	 * Basic business mode
	 */
	@Schema(description = "Basic business mode")
	private Integer baseMode;

	/**
	 * Wrapper pattern
	 */
	@Schema(description = "Wrapper pattern")
	private Integer wrapMode;

	/**
	 * Back-end package name
	 */
	@Schema(description = "Back-end package name")
	private String packageName;

	/**
	 * Back-end path
	 */
	@Schema(description = "Back-end path")
	private String apiPath;

	/**
	 * Front-end path
	 */
	@Schema(description = "Front-end path")
	private String webPath;

	/**
	 * Whether deleted
	 */
	@TableLogic
	@Schema(description = "Whether deleted")
	private Integer isDeleted;


}
