/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.util.mongo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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