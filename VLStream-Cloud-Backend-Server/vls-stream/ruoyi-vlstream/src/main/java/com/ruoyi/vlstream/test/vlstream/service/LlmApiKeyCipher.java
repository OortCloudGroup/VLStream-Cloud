/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.EncryptUtils;
import com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/** Encrypts provider credentials before persistence. */
@Service
@RequiredArgsConstructor
public class LlmApiKeyCipher {

	private final VlsLlmReviewProperties properties;

	public String encrypt(String value) {
		validateKey();
		return EncryptUtils.encryptByAes(StringUtils.defaultString(value), properties.getEncryptionKey());
	}

	public String decrypt(String value) {
		validateKey();
		return EncryptUtils.decryptByAes(value, properties.getEncryptionKey());
	}

	private void validateKey() {
		int length = StringUtils.length(properties.getEncryptionKey());
		if (length != 16 && length != 24 && length != 32) {
			throw new ServiceException("大模型 API Key 加密密钥必须为 16、24 或 32 位");
		}
	}
}
