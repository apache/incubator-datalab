package com.epam.dlab.mongo;

import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IsoLocalDateTimeSerializerTest {

	@Test
	public void serialize() throws IOException {
		com.fasterxml.jackson.core.JsonGenerator jsonGenerator = mock(com.fasterxml.jackson.core.JsonGenerator.class);
		SerializerProvider serializerProvider = mock(SerializerProvider.class);

		LocalDateTime localDateTime = LocalDateTime.now();

		new IsoLocalDateTimeSerializer().serialize(localDateTime, jsonGenerator, serializerProvider);

		verify(jsonGenerator).writeStartObject();
		verify(jsonGenerator).writeFieldName("$date");
		verify(jsonGenerator).writeString(localDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
		verify(jsonGenerator).writeEndObject();
		verifyNoMoreInteractions(jsonGenerator);
		verifyZeroInteractions(serializerProvider);
	}


}