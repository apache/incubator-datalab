package com.epam.dlab.backendapi.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CSVFormatterTest {

	@Test
	public void formatLine() {
		List<String> values = Arrays.asList("aaa", "bbb", "ccc", "aa", "bb", "cc", "a", "b", "c");
		String expected = "aaa,bbb,ccc,aa,bb,cc,a,b,c\n";
		String actual = CSVFormatter.formatLine(values, ',');
		assertEquals(expected, actual);
	}

	@Test
	public void formatLineWithCustomQuote() {
		List<String> values = Arrays.asList("aaa", "bbb", "ccc", "aa", "bb", "cc", "a", "b", "c");
		String expected = "\"aaa\",\"bbb\",\"ccc\",\"aa\",\"bb\",\"cc\",\"a\",\"b\",\"c\"\n";
		String actual = CSVFormatter.formatLine(values, ',', '"');
		assertEquals(expected, actual);
	}
}