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
package org.springblade.common.constant;

import org.springblade.core.launch.constant.AppConstant;

/**
 * Common constants
 *
 * @author Chill
 */
public interface LauncherConstant {

	/**
	 * app name
	 */
	String APPLICATION_NAME = AppConstant.APPLICATION_NAME_PREFIX + "api";

	/**
	 * sentinel dev address
	 */
	String SENTINEL_DEV_ADDR = "127.0.0.1:8858";

	/**
	 * sentinel prod address
	 */
	String SENTINEL_PROD_ADDR = "192.168.186.129:8858";

	/**
	 * sentinel test address
	 */
	String SENTINEL_TEST_ADDR = "192.168.186.129:8858";

	/**
	 * Dynamically get Sentinel address
	 *
	 * @param profile environment variable
	 * @return addr
	 */
	static String sentinelAddr(String profile) {
		switch (profile) {
			case (AppConstant.PROD_CODE):
				return SENTINEL_PROD_ADDR;
			case (AppConstant.TEST_CODE):
				return SENTINEL_TEST_ADDR;
			default:
				return SENTINEL_DEV_ADDR;
		}
	}
}
