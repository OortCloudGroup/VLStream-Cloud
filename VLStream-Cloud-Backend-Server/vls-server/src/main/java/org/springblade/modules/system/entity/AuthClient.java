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
 * @since 2019-03-24
 */
@Data
@TableName("blade_client")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Client object")
public class AuthClient extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Primary key ID
	 */
	@Schema(description = "Primary key")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/**
	 * Client ID
	 */
	@Schema(description = "Client ID")
	private String clientId;
	/**
	 * Client secret
	 */
	@Schema(description = "Client secret")
	private String clientSecret;
	/**
	 * Resource Set
	 */
	@Schema(description = "Resource Set")
	private String resourceIds;
	/**
	 * Authorization scope
	 */
	@Schema(description = "Authorization scope")
	private String scope;
	/**
	 * Authorization type
	 */
	@Schema(description = "Authorization type")
	private String authorizedGrantTypes;
	/**
	 * Callback address
	 */
	@Schema(description = "Callback address")
	private String webServerRedirectUri;
	/**
	 * Permission
	 */
	@Schema(description = "Permission")
	private String authorities;
	/**
	 * Token expiration seconds
	 */
	@Schema(description = "Token expiration seconds")
	private Integer accessTokenValidity;
	/**
	 * Refresh token expiration seconds
	 */
	@Schema(description = "Refresh token expiration seconds")
	private Integer refreshTokenValidity;
	/**
	 * Additional remarks
	 */
	@Schema(description = "Additional remarks")
	private String additionalInformation;
	/**
	 * Auto authorization
	 */
	@Schema(description = "Auto authorization")
	private String autoapprove;


}
