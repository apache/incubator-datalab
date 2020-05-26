package org.apache.dlab.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JacksonMapper {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static <T> String marshall(T obj) {
		try {
			return MAPPER.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
