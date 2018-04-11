package com.epam.dlab.mongo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.epam.dlab.mongo.IsoDateDeSerializer.DATE_NODE;

public class IsoLocalDateTimeDeSerializer extends JsonDeserializer<LocalDateTime> {

	@Override
	public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p);
		if (node.get(DATE_NODE) != null) {
			String dateValue = node.get(DATE_NODE).asText();
			return Instant.ofEpochMilli(Long.valueOf(dateValue)).atZone(ZoneOffset.systemDefault()).toLocalDateTime();
		} else {
			return Instant.ofEpochMilli(node.asLong()).atZone(ZoneOffset.systemDefault()).toLocalDateTime();
		}
	}
}
