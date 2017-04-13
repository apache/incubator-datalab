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

package com.epam.dlab.mongo;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

public class ResourceItemListTest {

	@Test
	public void append() {
		ResourceItemList list = new ResourceItemList();
		list.append("tag-user-nb-exp", "exp", DlabResourceType.EXPLORATORY, "user", "exp");
		list.append("tag-user-emr-exp-comp", "comp", DlabResourceType.COMPUTATIONAL, "user", "exp");
		
		assertEquals(2, list.size());
		
		ResourceItem comp = list.get(0);
		assertEquals("tag-user-emr-exp-comp", comp.getResourceId());
		assertEquals("comp", comp.getResourceName());
		assertEquals(DlabResourceType.COMPUTATIONAL, comp.getType());
		assertEquals("user", comp.getUser());
		assertEquals("exp", comp.getExploratoryName());
		
		ResourceItem exp = list.get(1);
		assertEquals("tag-user-nb-exp", exp.getResourceId());
		assertEquals("exp", exp.getResourceName());
		assertEquals(DlabResourceType.EXPLORATORY, exp.getType());
		assertEquals("user", exp.getUser());
		assertEquals("exp", exp.getExploratoryName());
		
		list.clear();
		assertEquals(0, list.size());
	}
}
