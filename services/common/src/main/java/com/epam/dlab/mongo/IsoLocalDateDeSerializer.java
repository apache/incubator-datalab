package com.epam.dlab.mongo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static com.epam.dlab.mongo.IsoDateDeSerializer.DATE_NODE;

public class IsoLocalDateDeSerializer extends JsonDeserializer<LocalDate> {
	@Override
	public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p);
		if (node.get(DATE_NODE) != null) {
			String dateValue = node.get(DATE_NODE).asText();
			return Instant.ofEpochMilli(Long.valueOf(dateValue)).atZone(ZoneOffset.systemDefault()).toLocalDate();
		} else {
			return Instant.ofEpochMilli(node.asLong()).atZone(ZoneOffset.systemDefault()).toLocalDate();
		}
	}
}
