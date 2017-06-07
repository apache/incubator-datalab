/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.automation.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.IOException;

public class JsonMapperDto {

    @SuppressWarnings("unchecked")
	public static <T> T readNode(String pathToJson, Class<T> clasz) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        try (FileInputStream in = new FileInputStream(pathToJson);){
          return (T) mapper
        		  .readerFor(clasz)
        		  .readValue(in);
        }
    }
}
