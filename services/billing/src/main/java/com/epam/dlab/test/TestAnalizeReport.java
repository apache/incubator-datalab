package com.epam.dlab.test;

import java.io.IOException;

import com.epam.dlab.BillingTool;
import com.epam.dlab.configuration.BillingToolConfiguration;
import com.epam.dlab.configuration.BillingToolConfigurationFactory;
import com.epam.dlab.configuration.ConfigJsonGenerator;
import com.epam.dlab.exception.AdapterException;
import com.epam.dlab.exception.InitializationException;
import com.epam.dlab.exception.ParseException;
import com.epam.dlab.module.FilterAWS;
import com.epam.dlab.module.ModuleName;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;

public class TestAnalizeReport {
	
	public static void main(String[] args) throws IOException, InitializationException, AdapterException, ParseException {
		JsonNode config = new ConfigJsonGenerator()
				.withAdapterIn("type", ModuleName.ADAPTER_FILE,
								"file", "D:\\DLab\\BillingSampleData\\dlab-billing-detailed-tags-2017-03-27.csv")
				.withAdapterOut("type", ModuleName.ADAPTER_CONSOLE,
						"writeHeader", "true")
				//.withAdapterOut("type", ModuleName.ADAPTER_FILE,
				//		"writeHeader", "true",
				//		"file", "D:\\DLab\\BillingSampleData\\aws-billing-detailed_dlab_tag_0327c.csv")
				.withParser("type", ModuleName.PARSER_CSV,
							//"whereCondition", "RecordType == 'LineItem' && " + 
							//				  "UsageStartDate >= '2017-03-27 09:00:00' && UsageStartDate <= '2017-03-27 14:00:00' && " +
							//				  "user:user:tag == 'dlab_tag_0327c:dlab_tag_0327c-ssn'" +
							//				  "user:user:tag != 'dlab_tag_0327c:dlab_tag_0327c-oleh_martushevskyi-emr-om1-e1-85085'
							//				  "&& UsageType == 'USW2-BoxUsage:t2.medium'",
							"columnMapping", "dlabId=user:user:tag;usageIntervalStart=UsageStartDate;usageIntervalEnd=UsageEndDate;" +
											 "product=ProductName;usageType=UsageType;usage=UsageQuantity;cost=BlendedCost;" +
											 "resourceId=ResourceId;tags=Operation,ItemDescription",
							"aggregate", "day",
							"headerLineNo", "1",
							"skipLines", "1")
				.build();
		
		BillingToolConfiguration conf = BillingToolConfigurationFactory.build(config, BillingToolConfiguration.class);
		
		
		FilterAWS filter = new FilterAWS();
		filter.setCurrencyCode("USD");
		filter.setDlabTagName("user:user:tag");
		filter.setServiceBaseName("dlab_tag_0327c");
		conf.setFilter(ImmutableList.of(filter ));

		System.out.println("Config is " + conf);
		System.out.println("WhereCondition is " + conf.getParser().get(0).getWhereCondition());
		
		new BillingTool().run(conf);
	}

}
