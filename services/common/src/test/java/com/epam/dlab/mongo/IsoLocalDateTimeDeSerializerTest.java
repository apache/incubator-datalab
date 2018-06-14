package com.epam.dlab.mongo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IsoLocalDateTimeDeSerializerTest {

	@Test
	public void deserialize() throws IOException {
		JsonParser jsonParser = mock(JsonParser.class);
		DeserializationContext ctxt = mock(DeserializationContext.class);

		ObjectCodec objectCodec = mock(ObjectCodec.class);
		when(jsonParser.getCodec()).thenReturn(objectCodec);

		JsonNode jsonNode = mock(JsonNode.class);
		when(objectCodec.readTree(jsonParser)).thenReturn(jsonNode);

		JsonNode jsonNode2 = mock(JsonNode.class);
		when(jsonNode.get(anyString())).thenReturn(jsonNode2);
		when(jsonNode2.asText()).thenReturn("1234567890");

		new IsoLocalDateTimeDeSerializer().deserialize(jsonParser, ctxt);

		verify(jsonParser).getCodec();
		verify(objectCodec).readTree(jsonParser);
		verify(jsonNode, times(2)).get("$date");
		verify(jsonNode2).asText();
		verify(jsonNode, never()).asLong();
		verifyNoMoreInteractions(jsonParser, objectCodec, jsonNode, jsonNode2);
	}

	@Test
	public void deserializeWhenMethodGetReturnsNull() throws IOException {
		JsonParser jsonParser = mock(JsonParser.class);
		DeserializationContext ctxt = mock(DeserializationContext.class);

		ObjectCodec objectCodec = mock(ObjectCodec.class);
		when(jsonParser.getCodec()).thenReturn(objectCodec);

		JsonNode jsonNode = mock(JsonNode.class);
		when(objectCodec.readTree(jsonParser)).thenReturn(jsonNode);

		when(jsonNode.get(anyString())).thenReturn(null);

		new IsoLocalDateTimeDeSerializer().deserialize(jsonParser, ctxt);

		verify(jsonParser).getCodec();
		verify(objectCodec).readTree(jsonParser);
		verify(jsonNode).get("$date");
		verify(jsonNode).asLong();
		verifyNoMoreInteractions(jsonParser, objectCodec, jsonNode);
	}
}