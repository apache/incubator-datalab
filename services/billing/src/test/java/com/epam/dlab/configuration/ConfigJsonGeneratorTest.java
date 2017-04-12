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

package com.epam.dlab.configuration;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class ConfigJsonGeneratorTest {
	
	private void checkProperty(JsonNode conf, String module, String name, String expectedValue) {
		JsonNode m = conf.get(module);
		assertNotNull("Module \"" + module + "\" not found in JSON configuration", m);
		
		JsonNode item = m.get(0);
		assertNotNull("Property \"" + module + "." + name + "\" not found in JSON configuration", item);
		
		JsonNode p = item.get(name);
		assertNotNull("Property \"" + module + "." + name + "\" not found in JSON configuration", p);
		
		assertEquals(expectedValue, p.asText());
	}

	@Test
	public void build() throws IOException {
		JsonNode conf = new ConfigJsonGenerator()
				.withAdapterIn("adapterInProperty", "adapterInValue")
				.withAdapterOut("adapterOutProperty", "adapterOutValue")
				.withParser("parserProperty", "parserValue")
				.withFilter("filterProperty", "filterValue")
				.build();
		
		checkProperty(conf, "adapterIn", "adapterInProperty", "adapterInValue");
		checkProperty(conf, "adapterOut", "adapterOutProperty", "adapterOutValue");
		checkProperty(conf, "parser", "parserProperty", "parserValue");
		checkProperty(conf, "filter", "filterProperty", "filterValue");
	}
}
