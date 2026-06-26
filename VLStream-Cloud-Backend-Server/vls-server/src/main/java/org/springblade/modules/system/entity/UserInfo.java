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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * User information
 *
 * @author Chill
 */
@Data
@Schema(description = "User information")
public class UserInfo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * User basic information
	 */
	@Schema(description = "User")
	private User user;

	/**
	 * Permission Identifier Set
	 */
	@Schema(description = "Permission Set")
	private List<String> permissions;

	/**
	 * Role Set
	 */
	@Schema(description = "Role Set")
	private List<String> roles;

	/**
	 * Third-party authorization ID
	 */
	@Schema(description = "Third-party authorization ID")
	private String oauthId;

}
