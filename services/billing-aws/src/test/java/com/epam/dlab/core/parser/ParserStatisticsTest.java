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

public class ParserStatisticsTest {

	@Test
	public void test() {
		ParserStatistics s = new ParserStatistics("name");
		
		assertEquals("name", s.getEntryName());

		assertEquals(0, s.getRowFiltered());
		s.incrRowFiltered();
		assertEquals(1, s.getRowFiltered());
		
		assertEquals(0, s.getRowParsed());
		s.incrRowParsed();
		assertEquals(1, s.getRowParsed());
		
		assertEquals(0, s.getRowReaded());
		s.incrRowReaded();
		assertEquals(1, s.getRowReaded());
		
		assertEquals(0, s.getRowSkipped());
		s.incrRowSkipped();
		assertEquals(1, s.getRowSkipped());
		
		assertEquals(0, s.getRowWritten());
		s.incrRowWritten();
		assertEquals(1, s.getRowWritten());
		
		assertEquals(0, s.getElapsedTime());
		
		s.start();
		assertEquals(0, s.getRowFiltered());
		assertEquals(0, s.getRowParsed());
		assertEquals(0, s.getRowReaded());
		assertEquals(0, s.getRowSkipped());
		assertEquals(0, s.getRowWritten());
		assertEquals(0, s.getElapsedTime());
	}
}
