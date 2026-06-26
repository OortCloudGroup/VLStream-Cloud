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
package org.springblade.modules.auth.controller;

import com.wf.captcha.SpecCaptcha;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.CacheNames;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.secure.AuthInfo;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.WebUtil;
import org.springblade.modules.auth.granter.ITokenGranter;
import org.springblade.modules.auth.granter.TokenGranterBuilder;
import org.springblade.modules.auth.granter.TokenParameter;
import org.springblade.modules.auth.utils.TokenUtil;
import org.springblade.modules.system.entity.UserInfo;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Authentication Module
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_AUTH_NAME)
@Tag(name = "User authorization authentication", description = "Authorization interface")
public class AuthController {

	private BladeRedis bladeRedis;

	@PostMapping("token")
	@Operation(summary = "Get authentication token", description = "Pass in tenant ID: tenantId, account: account, password: password")
	public R<AuthInfo> token(@Parameter(description = "Authorization type", required = true) @RequestParam(defaultValue = "password", required = false) String grantType,
							 @Parameter(description = "Refresh token") @RequestParam(required = false) String refreshToken,
							 @Parameter(description = "Tenant ID", required = true) @RequestParam(defaultValue = "000000", required = false) String tenantId,
							 @Parameter(description = "Account") @RequestParam(required = false) String account,
							 @Parameter(description = "Password") @RequestParam(required = false) String password) {

		String userType = Func.toStr(WebUtil.getRequest().getHeader(TokenUtil.USER_TYPE_HEADER_KEY), TokenUtil.DEFAULT_USER_TYPE);

		TokenParameter tokenParameter = new TokenParameter();
		tokenParameter.getArgs().set("tenantId", tenantId)
			.set("account", account)
			.set("password", password)
			.set("grantType", grantType)
			.set("refreshToken", refreshToken)
			.set("userType", userType);

		ITokenGranter granter = TokenGranterBuilder.getGranter(grantType);
		UserInfo userInfo = granter.grant(tokenParameter);

		if (userInfo == null || userInfo.getUser() == null) {
			return R.fail(TokenUtil.USER_NOT_FOUND);
		}

		return R.data(TokenUtil.createAuthInfo(userInfo));
	}

	@GetMapping("/captcha")
	@Operation(summary = "Get verification code")
	public R<Kv> captcha() {
		SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 5);
		String verCode = specCaptcha.text().toLowerCase();
		String key = UUID.randomUUID().toString();
		// Save to Redis and set expiration time to 30 minutes
		bladeRedis.setEx(CacheNames.CAPTCHA_KEY + key, verCode, 30L, TimeUnit.MINUTES);
		// Return key and base64 to frontend
		return R.data(Kv.init().set("key", key).set("image", specCaptcha.toBase64()));
	}

	@PostMapping("/logout")
	@Operation(summary = "Logout")
	public R<Kv> logout() {
		// Logout reserved logic
		return R.data(Kv.init().set("code", "200").set("msg", "Operation successful"));
	}

}
