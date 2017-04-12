package com.epam.dlab.test;

import java.io.IOException;

import com.epam.dlab.configuration.BillingToolConfiguration;
import com.epam.dlab.configuration.BillingToolConfigurationFactory;
import com.epam.dlab.configuration.ConfigJsonGenerator;
import com.epam.dlab.exception.AdapterException;
import com.epam.dlab.exception.InitializationException;
import com.epam.dlab.exception.ParseException;
import com.epam.dlab.module.ModuleName;
import com.fasterxml.jackson.databind.JsonNode;

public class TestConfGenerator {
	
	public static void main(String[] args) throws IOException, InitializationException, AdapterException, ParseException {
		JsonNode config = new ConfigJsonGenerator()
				.withAdapterIn("type", ModuleName.ADAPTER_FILE,
								"file", "D:\\DLab\\BillingSampleData\\test_aws_detail_report.csv")
				.withAdapterOut("type", ModuleName.ADAPTER_CONSOLE)
				.withParser("type", ModuleName.PARSER_CSV,
							"columnMapping", "accountId=PayerAccountId;usageIntervalStart=UsageStartDate;usageIntervalEnd=UsageEndDate;zone=AvailabilityZone;product=ProductName;resourceId=ResourceId;usageType=UsageType;usage=UsageQuantity;cost=BlendedCost;tags=user:department,user:environment,user:team",
							"fieldSeparator", ",",
							"fieldTerminator", "\"",
							"escapeChar", "\\",
							"headerLineNo", "1",
							"skipLines", "1")
				.build();
		
		BillingToolConfiguration conf = BillingToolConfigurationFactory.build(config, BillingToolConfiguration.class);
		System.out.println("Config is " + conf);
		
		//new BillingTool().run(conf);
	}

}
