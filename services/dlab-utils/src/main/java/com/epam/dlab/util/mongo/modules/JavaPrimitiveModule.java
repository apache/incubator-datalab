package com.epam.dlab.util.mongo.modules;

import com.epam.dlab.util.mongo.LongDeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JavaPrimitiveModule extends SimpleModule {

	public JavaPrimitiveModule() {
		addDeserializer(Long.class, new LongDeSerializer());
	}

}
