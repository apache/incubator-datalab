package com.epam.dlab.util.mongo.modules;

import com.epam.dlab.util.mongo.MongoStringDeserializer;
import com.epam.dlab.util.mongo.MongoStringSerializaer;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class MongoModule extends SimpleModule {

	public MongoModule() {
		addKeySerializer(String.class, new MongoStringSerializaer());
		addKeyDeserializer(String.class, new MongoStringDeserializer());
	}
}
