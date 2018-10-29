package com.epam.dlab.util.mongo;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class JavaPrimitiveModule extends SimpleModule {

	public JavaPrimitiveModule(){
		addDeserializer(Long.class, new LongDeSerializer());
	}
}
