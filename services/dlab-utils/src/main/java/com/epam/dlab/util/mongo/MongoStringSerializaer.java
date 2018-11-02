package com.epam.dlab.util.mongo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class MongoStringSerializaer extends JsonSerializer<String> {
	public static final String DOT_UNICODE = "U+FF0E";

	@Override
	public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value.contains(".")) {
			gen.writeFieldName(value.replace(".", DOT_UNICODE));
		} else {
			gen.writeFieldName(value);
		}
	}
}
