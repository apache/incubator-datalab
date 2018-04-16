package com.epam.dlab.backendapi.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DateRemoverUtilTest {

	@Test
	public void removeDateFormErrorMessageWithErrorDateFormat() {
		String errorMessage = "All dates with format '[Error-2018-04-12 15:30:35]:' are erroneous";
		String expected = "All dates with format 'yyyy-MM-dd' are erroneous";
		String actual = DateRemoverUtil.removeDateFormErrorMessage(errorMessage, "\\[Error-\\d{4}-\\d{2}-" +
				"\\d{2} \\d{2}:\\d{2}:\\d{2}\\]:", "yyyy-MM-dd");
		assertEquals(expected, actual);
	}

	@Test
	public void removeDateFormErrorMessage1() {
		String errorMessage = "All dates with format '[Error-2018-04-12 15:30:35]:' are erroneous";
		String expected = "All dates with format '[Error]:' are erroneous";
		String actual = DateRemoverUtil.removeDateFormErrorMessage(errorMessage);
		assertEquals(expected, actual);
	}
}