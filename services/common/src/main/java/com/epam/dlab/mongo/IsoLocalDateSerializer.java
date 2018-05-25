package com.epam.dlab.mongo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class IsoLocalDateSerializer extends JsonSerializer<LocalDate> {
	@Override
	public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeStartObject();
		gen.writeFieldName(IsoDateDeSerializer.DATE_NODE);
		gen.writeString(value.format(DateTimeFormatter.ISO_DATE));
		gen.writeEndObject();
	}
}
