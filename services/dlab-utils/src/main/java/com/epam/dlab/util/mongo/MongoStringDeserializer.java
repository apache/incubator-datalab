package com.epam.dlab.util.mongo;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import static com.epam.dlab.util.mongo.MongoStringSerializaer.DOT_UNICODE;

public class MongoStringDeserializer extends KeyDeserializer {

	@Override
	public Object deserializeKey(String key, DeserializationContext ctxt) {
		return key.contains(DOT_UNICODE) ? key.replace(DOT_UNICODE, ".") : key;
	}
}
