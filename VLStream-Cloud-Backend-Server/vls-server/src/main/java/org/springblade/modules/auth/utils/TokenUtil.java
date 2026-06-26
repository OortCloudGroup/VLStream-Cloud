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
package org.springblade.modules.auth.utils;

import org.springblade.common.cache.CacheNames;
import org.springblade.core.launch.constant.TokenConstant;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.secure.AuthInfo;
import org.springblade.core.secure.TokenInfo;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.*;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.entity.UserInfo;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Utility Class
 *
 * @author Chill
 */
public class TokenUtil {

	public final static String CAPTCHA_HEADER_KEY = "Captcha-Key";
	public final static String CAPTCHA_HEADER_CODE = "Captcha-Code";
	public final static String CAPTCHA_NOT_CORRECT = "Verification code is incorrect";
	public final static String TENANT_HEADER_KEY = "Tenant-Id";
	public final static String DEFAULT_TENANT_ID = "000000";
	public final static String USER_TYPE_HEADER_KEY = "User-Type";
	public final static String DEFAULT_USER_TYPE = "web";
	public final static String USER_NOT_FOUND = "Incorrect username or password";
	public final static String HEADER_KEY = "Authorization";
	public final static String HEADER_PREFIX = "Basic ";
	public final static String ENCRYPT_PREFIX = "04";
	public final static String USER_HAS_TOO_MANY_FAILS = "Too many failed user login attempts";
	public final static String IP_HAS_TOO_MANY_FAILS = "Too many failed user login attempts, please try again later";
	public final static String DEFAULT_AVATAR = "https://bladex.cn/images/logo.png";

	/**
	 * Create authentication token
	 *
	 * @param userInfo user information
	 * @return token
	 */
	public static AuthInfo createAuthInfo(UserInfo userInfo) {
		User user = userInfo.getUser();

		//Set jwt parameters
		Map<String, String> param = new HashMap<>(16);
		param.put(TokenConstant.TOKEN_TYPE, TokenConstant.ACCESS_TOKEN);
		param.put(TokenConstant.TENANT_ID, user.getTenantId());
		param.put(TokenConstant.OAUTH_ID, userInfo.getOauthId());
		param.put(TokenConstant.USER_ID, Func.toStr(user.getId()));
		param.put(TokenConstant.ROLE_ID, user.getRoleId());
		param.put(TokenConstant.DEPT_ID, user.getDeptId());
		param.put(TokenConstant.ACCOUNT, user.getAccount());
		param.put(TokenConstant.USER_NAME, user.getAccount());
		param.put(TokenConstant.ROLE_NAME, Func.join(userInfo.getRoles()));

		TokenInfo accessToken = SecureUtil.createJWT(param, "audience", "issuser", TokenConstant.ACCESS_TOKEN);
		AuthInfo authInfo = new AuthInfo();
		authInfo.setUserId(user.getId());
		authInfo.setTenantId(user.getTenantId());
		authInfo.setOauthId(userInfo.getOauthId());
		authInfo.setAccount(user.getAccount());
		authInfo.setUserName(user.getRealName());
		authInfo.setAuthority(Func.join(userInfo.getRoles()));
		authInfo.setAccessToken(accessToken.getToken());
		authInfo.setExpiresIn(accessToken.getExpire());
		authInfo.setRefreshToken(createRefreshToken(userInfo).getToken());
		authInfo.setTokenType(TokenConstant.BEARER);
		authInfo.setLicense(TokenConstant.LICENSE_NAME);

		return authInfo;
	}

	/**
	 * Create refreshToken
	 *
	 * @param userInfo user information
	 * @return refreshToken
	 */
	private static TokenInfo createRefreshToken(UserInfo userInfo) {
		User user = userInfo.getUser();
		Map<String, String> param = new HashMap<>(16);
		param.put(TokenConstant.TOKEN_TYPE, TokenConstant.REFRESH_TOKEN);
		param.put(TokenConstant.USER_ID, Func.toStr(user.getId()));
		return SecureUtil.createJWT(param, "audience", "issuser", TokenConstant.REFRESH_TOKEN);
	}

	/**
	 * Parse SM2 encrypted password
	 *
	 * @param rawPassword original password submitted during request
	 * @param publicKey   public key
	 * @param privateKey  private key
	 * @return decrypted password
	 */
	public static String decryptPassword(String rawPassword, String publicKey, String privateKey) {
		// Match fails if any of them is empty
		if (StringUtil.isAnyBlank(publicKey, privateKey)) {
			return StringPool.EMPTY;
		}
		// Handle case where encryption in some utility classes does not have the 04 prefix
		if (!StringUtil.startsWithIgnoreCase(rawPassword, ENCRYPT_PREFIX)) {
			rawPassword = ENCRYPT_PREFIX + rawPassword;
		}
		// Decrypt password
		String decryptPassword = SM2Util.decrypt(rawPassword, privateKey);
		// Signature verification
		boolean isVerified = SM2Util.verify(decryptPassword, SM2Util.sign(decryptPassword, privateKey), publicKey);
		if (!isVerified) {
			return StringPool.EMPTY;
		}
		return decryptPassword;
	}

	/**
	 * Max failure limit
	 */
	public final static Integer FAIL_COUNT = 5;

	/**
	 * Check if account and IP are locked
	 *
	 * @param bladeRedis Redis cache
	 * @param tenantId   tenant ID
	 * @param account    account
	 */
	public static void checkAccountAndIpLock(BladeRedis bladeRedis, String tenantId, String account) {
		String ip = WebUtil.getIP();

		// Check account lock
		int userFailCount = Func.toInt(bladeRedis.get(CacheNames.tenantKey(tenantId, CacheNames.USER_FAIL_KEY, account)), 0);
		if (userFailCount >= FAIL_COUNT) {
			throw new ServiceException(USER_HAS_TOO_MANY_FAILS);
		}

		// Check IP lock
		int ipFailCount = Func.toInt(bladeRedis.get(CacheNames.IP_FAIL_KEY + ip), 0);
		if (ipFailCount >= FAIL_COUNT) {
			throw new ServiceException(IP_HAS_TOO_MANY_FAILS);
		}
	}

	/**
	 * Handle login failure, increase failure count
	 *
	 * @param bladeRedis Redis cache
	 * @param tenantId   tenant ID
	 * @param account    account
	 */
	public static void handleLoginFailure(BladeRedis bladeRedis, String tenantId, String account) {
		String ip = WebUtil.getIP();

		// Increase account error lock count
		int userFailCount = Func.toInt(bladeRedis.get(CacheNames.tenantKey(tenantId, CacheNames.USER_FAIL_KEY, account)), 0);
		bladeRedis.setEx(CacheNames.tenantKey(tenantId, CacheNames.USER_FAIL_KEY, account), userFailCount + 1, Duration.ofMinutes(30));

		// Increase IP error lock count
		int ipFailCount = Func.toInt(bladeRedis.get(CacheNames.IP_FAIL_KEY + ip), 0);
		bladeRedis.setEx(CacheNames.IP_FAIL_KEY + ip, ipFailCount + 1, Duration.ofMinutes(30));
	}

	/**
	 * Handle login success, clear failure cache
	 *
	 * @param bladeRedis Redis cache
	 * @param tenantId   tenant ID
	 * @param account    account
	 */
	public static void handleLoginSuccess(BladeRedis bladeRedis, String tenantId, String account) {
		String ip = WebUtil.getIP();

		// Clear account login failure cache
		bladeRedis.del(CacheNames.tenantKey(tenantId, CacheNames.USER_FAIL_KEY, account));

		// Clear IP login failure cache
		bladeRedis.del(CacheNames.IP_FAIL_KEY + ip);
	}

}
