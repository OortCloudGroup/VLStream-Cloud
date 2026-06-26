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
package org.springblade.modules.system.entity;

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
 * Entity class
 *
 * @author BladeX
 */
@Data
@TableName("blade_scope_data")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DataScope object")
public class DataScope extends BaseEntity {

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
	 * Menu primary key
	 */
	@Schema(description = "Menu primary key")
	private Long menuId;
	/**
	 * Resource Code
	 */
	@Schema(description = "Resource Code")
	private String resourceCode;
	/**
	 * Data permission name
	 */
	@Schema(description = "Data permission name")
	private String scopeName;
	/**
	 * Data permission visible fields
	 */
	@Schema(description = "Data permission visible fields")
	private String scopeField;
	/**
	 * Data permission class name
	 */
	@Schema(description = "Data permission class name")
	private String scopeClass;
	/**
	 * Data permission field
	 */
	@Schema(description = "Data permission field")
	private String scopeColumn;
	/**
	 * Data permission type
	 */
	@Schema(description = "Data permission type")
	private Integer scopeType;
	/**
	 * Data permission value range
	 */
	@Schema(description = "Data permission value range")
	private String scopeValue;
	/**
	 * Data permission remarks
	 */
	@Schema(description = "Data permission remarks")
	private String remark;


}
