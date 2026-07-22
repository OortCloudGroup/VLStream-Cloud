/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlgorithmRepositoryTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Verifies that the API accepts the legacy string status values used by existing clients.
	 */
	@Test
	@Tag("dev")
	void deserializesLegacyStringStatus() throws Exception {
		AlgorithmRepository repository = objectMapper.readValue(
			"{\"name\":\"测试\",\"repositoryType\":\"extended\",\"status\":\"enabled\"}",
			AlgorithmRepository.class);

		assertEquals(Integer.valueOf(1), repository.getStatus());
	}

	/**
	 * Verifies that normal numeric database status values remain supported.
	 */
	@Test
	@Tag("dev")
	void deserializesNumericStatus() throws Exception {
		AlgorithmRepository repository = objectMapper.readValue(
			"{\"name\":\"测试\",\"repositoryType\":\"extended\",\"status\":0}",
			AlgorithmRepository.class);

		assertEquals(Integer.valueOf(0), repository.getStatus());
	}
}
