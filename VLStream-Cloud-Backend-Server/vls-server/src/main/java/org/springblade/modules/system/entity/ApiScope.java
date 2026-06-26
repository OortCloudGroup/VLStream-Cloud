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

import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("blade_scope_api")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "ApiScope object")
public class ApiScope extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

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
	 * Interface permission name
	 */
	@Schema(description = "Interface permission name")
	private String scopeName;
	/**
	 * Interface permission field
	 */
	@Schema(description = "Interface permission field")
	private String scopePath;
	/**
	 * Interface permission type
	 */
	@Schema(description = "Interface permission type")
	private Integer scopeType;
	/**
	 * Interface permission remarks
	 */
	@Schema(description = "Interface permission remarks")
	private String remark;


}
