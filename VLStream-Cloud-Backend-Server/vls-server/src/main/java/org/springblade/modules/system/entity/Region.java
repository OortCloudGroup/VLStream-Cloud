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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Administrative division table entity class
 *
 * @author Chill
 */
@Data
@TableName("blade_region")
@Schema(description = "Region object")
public class Region implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Division code
	 */
	@TableId(value = "code", type = IdType.INPUT)
	@Schema(description = "Division code")
	private String code;
	/**
	 * Parent division code
	 */
	@Schema(description = "Parent division code")
	private String parentCode;
	/**
	 * Ancestor division code
	 */
	@Schema(description = "Ancestor division code")
	private String ancestors;
	/**
	 * Division name
	 */
	@Schema(description = "Division name")
	private String name;
	/**
	 * Provincial division code
	 */
	@Schema(description = "Provincial division code")
	private String provinceCode;
	/**
	 * Provincial name
	 */
	@Schema(description = "Provincial name")
	private String provinceName;
	/**
	 * Municipal division code
	 */
	@Schema(description = "Municipal division code")
	private String cityCode;
	/**
	 * Municipal name
	 */
	@Schema(description = "Municipal name")
	private String cityName;
	/**
	 * District-level division code
	 */
	@Schema(description = "District-level division code")
	private String districtCode;
	/**
	 * District-level name
	 */
	@Schema(description = "District-level name")
	private String districtName;
	/**
	 * Town-level division code
	 */
	@Schema(description = "Town-level division code")
	private String townCode;
	/**
	 * Town-level name
	 */
	@Schema(description = "Town-level name")
	private String townName;
	/**
	 * Village Division Code
	 */
	@Schema(description = "Village Division Code")
	private String villageCode;
	/**
	 * Village Name
	 */
	@Schema(description = "Village Name")
	private String villageName;
	/**
	 * Level
	 */
	@Schema(description = "Level")
	private Integer level;
	/**
	 * Sort
	 */
	@Schema(description = "Sort")
	private Integer sort;
	/**
	 * Remarks
	 */
	@Schema(description = "Remarks")
	private String remark;


}
