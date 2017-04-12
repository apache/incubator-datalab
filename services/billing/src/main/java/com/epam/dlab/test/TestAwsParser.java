package com.epam.dlab.test;

import com.epam.dlab.BillingTool;

public class TestAwsParser {
	
	public static void main(String[] args) {
		try {
			new BillingTool().run("billing.yml");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
