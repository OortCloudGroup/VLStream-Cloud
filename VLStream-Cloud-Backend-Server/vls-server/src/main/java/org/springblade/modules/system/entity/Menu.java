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
import org.springblade.core.tool.utils.Func;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entity class
 *
 * @author Chill
 */
@Data
@TableName("blade_menu")
@Schema(description = "Menu object")
public class Menu implements Serializable {

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
	 * Menu parent primary key
	 */
	@Schema(description = "Parent primary key")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long parentId;

	/**
	 * Menu code
	 */
	@Schema(description = "Menu code")
	private String code;

	/**
	 * Menu name
	 */
	@Schema(description = "Menu name")
	private String name;

	/**
	 * Menu alias
	 */
	@Schema(description = "Menu alias")
	private String alias;

	/**
	 * Request Address
	 */
	@Schema(description = "Request Address")
	private String path;

	/**
	 * Menu resource
	 */
	@Schema(description = "Menu resource")
	private String source;

	/**
	 * Sort
	 */
	@Schema(description = "Sort")
	private Integer sort;

	/**
	 * Menu type
	 */
	@Schema(description = "Menu type")
	private Integer category;

	/**
	 * Operation button type
	 */
	@Schema(description = "Operation button type")
	private Integer action;

	/**
	 * Whether to open a new page
	 */
	@Schema(description = "Whether to open a new page")
	private Integer isOpen;

	/**
	 * Remarks
	 */
	@Schema(description = "Remarks")
	private String remark;

	/**
	 * Whether deleted
	 */
	@TableLogic
	@Schema(description = "Whether deleted")
	private Integer isDeleted;


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		Menu other = (Menu) obj;
		return Func.equals(this.getId(), other.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, parentId, code);
	}

}
