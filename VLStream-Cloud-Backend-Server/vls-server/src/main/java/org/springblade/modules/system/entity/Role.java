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
@TableName("blade_role")
@Schema(description = "Role object")
public class Role implements Serializable {

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
	 * Tenant ID
	 */
	@Schema(description = "Tenant ID")
	private String tenantId;

	/**
	 * Parent primary key
	 */
	@Schema(description = "Parent primary key")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long parentId;

	/**
	 * Role Name
	 */
	@Schema(description = "Role Name")
	private String roleName;

	/**
	 * Sort
	 */
	@Schema(description = "Sort")
	private Integer sort;

	/**
	 * Role Alias
	 */
	@Schema(description = "Role Alias")
	private String roleAlias;

	/**
	 * Whether deleted
	 */
	@TableLogic
	@Schema(description = "Whether deleted")
	private Integer isDeleted;


}
