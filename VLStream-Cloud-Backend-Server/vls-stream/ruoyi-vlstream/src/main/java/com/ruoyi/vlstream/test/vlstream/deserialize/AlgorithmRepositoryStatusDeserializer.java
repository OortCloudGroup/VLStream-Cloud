/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Converts the repository UI status values into the integer values stored by the database.
 */
public class AlgorithmRepositoryStatusDeserializer extends StdDeserializer<Integer> {

	private static final long serialVersionUID = 1L;

	public AlgorithmRepositoryStatusDeserializer() {
		super(Integer.class);
	}

	/**
	 * Deserializes 1/0 and enabled/disabled into the database status convention.
	 */
	@Override
	public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		if (parser.currentToken() == JsonToken.VALUE_NULL) {
			return null;
		}
		if (parser.currentToken().isNumeric()) {
			return parser.getIntValue();
		}

		String value = parser.getValueAsString();
		if ("enabled".equalsIgnoreCase(value)) {
			return 1;
		}
		if ("disabled".equalsIgnoreCase(value)) {
			return 0;
		}
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException exception) {
			throw JsonMappingException.from(parser, "status must be 1, 0, enabled, or disabled", exception);
		}
	}
}
