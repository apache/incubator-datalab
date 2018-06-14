package com.epam.dlab.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;

public class IsoLocalDateTimeSerDeTest {

	private static ObjectMapper objectMapper;

	@BeforeClass
	public static void setup() {
		objectMapper = new ObjectMapper();
	}

	@Test
	public void shoudProperlySerializeLocalDateTimeToJson() throws JsonProcessingException {
		String actual = objectMapper.writeValueAsString(new SampleClass());
		assertEquals("{\"localDateTime\":{\"$date\":\"2018-04-10T15:30:45\"}}", actual);
	}

	@Test
	public void shoudProperlyDeserializeLocalDateTimeFromJson() throws IOException {
		LocalDateTime now = LocalDateTime.now();
		long l = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		SampleClass actual = objectMapper
				.readValue("{\"localDateTime\":{\"$date\":" + l + "}}", SampleClass.class);

		assertEquals(now, actual.getLocalDateTime());
	}

	private static class SampleClass {

		@JsonSerialize(using = IsoLocalDateTimeSerializer.class)
		@JsonDeserialize(using = IsoLocalDateTimeDeSerializer.class)
		private final LocalDateTime localDateTime = LocalDateTime.parse("2018-04-10T15:30:45");

		LocalDateTime getLocalDateTime() {
			return localDateTime;
		}
	}
}
