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

package com.epam.dlab.auth.dao.script;

import com.epam.dlab.auth.UserInfo;
import org.junit.*;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class ScriptHolderTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws ScriptException {
		ScriptHolder sh = new ScriptHolder();
		Map<String,Object> map = new HashMap<>();
		map.put("key1", "val1");
		map.put("key2", "val2");
		UserInfo ui1 = sh.evalOnce("first", "javascript", "var enrichUserInfo=function(ui,context){ui.addRole(context['key1']);ui.setFirstName(\"Mike\");return ui;}").apply(new UserInfo("",""), map);
		System.out.println(ui1);
		assertNotNull(ui1);

		UserInfo ui2 = sh.evalOnce("second", "python", "def enrichUserInfo(ui,context):\n   ui.addRole(context['key2'])\n   ui.setLastName(\"Teplitskiy\")\n   return ui\n").apply(ui1, map);
		System.out.println(ui2);
		assertNotNull(ui2);

	}

}
