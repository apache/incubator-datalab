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

package com.epam.dlab.core;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

import org.junit.Test;

import com.epam.dlab.exception.InitializationException;

public class ModuleDataTest {
	
	@Test
	public void isModified() throws InitializationException {
		ModuleData d = new ModuleData(null);
		assertEquals(false, d.isModified());
		d.set("key1", "value1");
		assertEquals("value1", d.get("key1"));
		assertEquals(true, d.isModified());
		d.set("key1", null);
		assertEquals(null, d.get("key1"));
		
		try {
			d.store();
			fail("Method store() should failed");
		} catch (InitializationException e) {
			// OK
		}
	}
}
