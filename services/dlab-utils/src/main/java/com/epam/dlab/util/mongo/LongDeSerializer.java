package com.epam.dlab.util.mongo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class LongDeSerializer extends JsonDeserializer<Long> {
	private static final String NUMBER_NODE = "$numberLong";

	@Override
	public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p);

		final JsonNode numberNode = node.get(NUMBER_NODE);
		if (numberNode != null) {
			return numberNode.asLong();
		} else {
			return node.asLong();
		}
	}
}
