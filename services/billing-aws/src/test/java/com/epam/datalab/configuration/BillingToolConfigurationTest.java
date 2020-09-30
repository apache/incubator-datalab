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

package com.epam.datalab.configuration;

import com.epam.datalab.core.AdapterBase.Mode;
import com.epam.datalab.core.aggregate.AggregateGranularity;
import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.module.AdapterConsole;
import com.epam.datalab.module.AdapterFile;
import com.epam.datalab.module.ModuleName;
import com.epam.datalab.module.ParserCsv;
import com.epam.datalab.module.aws.FilterAWS;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class BillingToolConfigurationTest {

	@Test
	@Ignore
	public void config() throws InitializationException {
		JsonNode node = new ConfigJsonGenerator()
				.withAdapterIn("type", ModuleName.ADAPTER_FILE,
						"file", "fileIn.csv")
				.withAdapterOut("type", ModuleName.ADAPTER_CONSOLE)
				.withFilter("type", ModuleName.FILTER_AWS,
						"currencyCode", "USD",
						"columnDatalabTag", "user:user:tag",
						"serviceBaseName", "sbn")
				.withParser("type", ModuleName.PARSER_CSV,
						"columnMapping", "datalab_id=user:user:tag;usage_date=UsageStartDate;product=ProductName;" +
								"tags=Operation,ItemDescription",
						"whereCondition", "UsageStartDate >= '2017-04-12'",
						"aggregate", "day",
						"headerLineNo", "5",
						"skipLines", "10",
						"fieldSeparator", ";",
						"fieldTerminator", "^",
						"escapeChar", "/",
						"decimalSeparator", ",",
						"groupingSeparator", "_")
				.build();
		BillingToolConfiguration conf = BillingToolConfigurationFactory.build(node, BillingToolConfiguration.class);
		ParserCsv parser = (ParserCsv) conf.build();

		assertNotNull("Parser was not build", parser);

		AdapterFile in = (AdapterFile) parser.getAdapterIn();
		assertEquals(ModuleName.ADAPTER_FILE, in.getType());
		assertEquals(Mode.READ, in.getMode());
		assertEquals("fileIn.csv", in.getFile());

		AdapterConsole out = (AdapterConsole) parser.getAdapterOut();
		assertEquals(ModuleName.ADAPTER_CONSOLE, out.getType());
		assertEquals(Mode.WRITE, out.getMode());

		FilterAWS filter = (FilterAWS) parser.getFilter();
		assertEquals(ModuleName.FILTER_AWS, filter.getType());
		assertEquals("USD", filter.getCurrencyCode());
		assertEquals("user:user:tag", filter.getColumnDatalabTag());
		assertEquals("sbn", filter.getServiceBaseName());
		assertEquals(parser, filter.getParser());

		assertEquals(ModuleName.PARSER_CSV, parser.getType());
		assertEquals("datalab_id=user:user:tag;usage_date=UsageStartDate;product=ProductName;tags=Operation,ItemDescription",
				parser.getColumnMapping());
		assertEquals("UsageStartDate >= '2017-04-12'", parser.getWhereCondition());
		assertEquals(AggregateGranularity.DAY, parser.getAggregate());
		assertEquals(5, parser.getHeaderLineNo());
		assertEquals(10, parser.getSkipLines());
		assertEquals(';', parser.getFieldSeparator());
		assertEquals('^', parser.getFieldTerminator());
		assertEquals('/', parser.getEscapeChar());
		assertEquals(',', parser.getDecimalSeparator());
		assertEquals('_', parser.getGroupingSeparator());
	}
}
