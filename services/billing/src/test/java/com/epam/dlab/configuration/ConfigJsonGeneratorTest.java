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

import java.io.IOException;

import org.junit.Test;

import com.epam.dlab.module.ModuleName;
import com.fasterxml.jackson.databind.JsonNode;

public class ConfigJsonGeneratorTest {

	@Test
	public void test() throws IOException {
		JsonNode config = new ConfigJsonGenerator()
				.withAdapterIn("type", ModuleName.ADAPTER_FILE,
						"adapterIn", "adapterInProperty")
				.withAdapterOut("type", ModuleName.ADAPTER_CONSOLE,
						"adapterOut", "adapterOutProperty")
				.withParser("type", ModuleName.PARSER_CSV,
					"columnMapping", "accountId=PayerAccountId;usageIntervalStart=UsageStartDate;usageIntervalEnd=UsageEndDate;zone=AvailabilityZone;product=ProductName;resourceId=ResourceId;usageType=UsageType;usage=UsageQuantity;cost=BlendedCost;tags=user:department,user:environment,user:team",
					"fieldSeparator", ",",
					"fieldTerminator", "\"",
					"escapeChar", "\\",
					"headerLineNo", "1",
					"skipLines", "1")
				.withFilter("type", ModuleName.FILTER_AWS)
				.build();
		
	}
}
