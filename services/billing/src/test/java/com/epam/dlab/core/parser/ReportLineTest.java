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

package com.epam.dlab.core.parser;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

import com.epam.dlab.core.BillingUtils;
import com.epam.dlab.exception.ParseException;

import jersey.repackaged.com.google.common.collect.Maps;

public class ReportLineTest {

	private void checkGetters(ReportLine r) {
		assertEquals("dlabId", r.getDlabId());
		assertEquals("user", r.getUser());
		assertEquals("2016-01-22", r.getUsageDate());
		assertEquals("Amazon Elastic Compute Cloud", r.getProduct());
		assertEquals("usageType", r.getUsageType());
		assertEquals(56.7, r.getUsage());
		assertEquals(12.34, r.getCost());
		assertEquals("USD", r.getCurrencyCode());
		assertEquals("i-1234567890abcdefg", r.getResourceId());
		assertEquals("value1", r.getTags().get("tag1"));
		assertEquals("value2", r.getTags().get("tag2"));
	}
	
	@Test
	public void set() throws ParseException {
		ReportLine r = new ReportLine();
		
		r.setDlabId("dlabId");
		r.setCost(12.34);
		r.setCurrencyCode("USD");
		r.setProduct("Amazon Elastic Compute Cloud");
		r.setResourceTypeId("i-1234567890abcdefg");
		r.setUsage(56.7);
		r.setUsageDate("2016-01-22");
		r.setUsageType("usageType");
		r.setUser("user");
		r.setTags(Maps.newLinkedHashMap(BillingUtils.stringsToMap("tag1", "value1", "tag2", "value2")));

		checkGetters(r);
	}

	@Test
	public void resourceType() {
		// TODO Test: ResourceType + ResourceTypeId
		//assertEquals(ResourceType.IP_ADDRESS, r.getResourceType());
		//assertEquals("resourceTypeId", r.getResourceTypeId());
	}
}
