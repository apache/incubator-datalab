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

package com.epam.datalab.automation.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.gson.JsonParseException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class JsonMapperDto {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private JsonMapperDto() {
	}

    @SuppressWarnings("unchecked")
	public static <T> T readNode(String pathToJson, Class<T> clasz) throws IOException {
        try (FileInputStream in = new FileInputStream(pathToJson)){
			return OBJECT_MAPPER.readerFor(clasz).readValue(in);
        }
    }

    public static <T> List<T> readListOf(String pathToJson, Class<T> clasz) {
        try (FileInputStream in = new FileInputStream(pathToJson)){
            CollectionType typeReference = TypeFactory.defaultInstance().constructCollectionType(List.class, clasz);
            return OBJECT_MAPPER.readValue(in, typeReference);
        } catch (IOException e) {
			throw new JsonParseException("Cannot read json file", e);
        }
    }

    public static <T> T readObject(String pathToJson, Class<T> clasz) {
        try (FileInputStream in = new FileInputStream(pathToJson)){
            return OBJECT_MAPPER.readValue(in, clasz);
        } catch (IOException e) {
			throw new JsonParseException("Cannot read json file", e);
        }
    }
}
