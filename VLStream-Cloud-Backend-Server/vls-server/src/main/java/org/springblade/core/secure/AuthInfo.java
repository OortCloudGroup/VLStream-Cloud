/**
 * Copyright (c) 2018-2099, Chill Zhuang (bladejava@qq.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.core.secure;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AuthInfo
 *
 * @author Chill
 */
@Data
@Schema(description = "Authentication Information")
public class AuthInfo {
	@Schema(description = "Token")
	private String accessToken;
	@Schema(description = "Token type")
	private String tokenType;
	@Schema(description = "Refresh token")
	private String refreshToken;
	@Schema(description = "User ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long userId;
	@Schema(description = "Tenant ID")
	private String tenantId;
	@Schema(description = "Third-party system ID")
	private String oauthId;
	@Schema(description = "Avatar")
	private String avatar = "https://bladex.cn/images/logo.png";
	@Schema(description = "Role Name")
	private String authority;
	@Schema(description = "Username")
	private String userName;
	@Schema(description = "Account Name")
	private String account;
	@Schema(description = "Expiration Time")
	private long expiresIn;
	@Schema(description = "License")
	private String license = "powered by blade";
}
