/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.service.aws;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.aws.AwsBillingDAO;
import com.epam.dlab.backendapi.resources.dto.aws.AwsBillingFilter;
import com.epam.dlab.exceptions.DlabException;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AwsBillingServiceTest {

	private UserInfo userInfo;
	private AwsBillingFilter billingFilter;
	private Document basicDocument;

	@Mock
	private AwsBillingDAO billingDAO;

	@InjectMocks
	private AwsBillingService awsBillingService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		userInfo = getUserInfo();
		billingFilter = new AwsBillingFilter();
		basicDocument = getBasicDocument();
	}

	@Test
	public void getReportWithTheSameInstanceOfDocument() {
		Document expectedDocument = new Document();
		when(billingDAO.getReport(any(UserInfo.class), any(AwsBillingFilter.class))).thenReturn(expectedDocument);

		Document actualDocument = awsBillingService.getReport(userInfo, billingFilter);
		assertEquals(expectedDocument, actualDocument);

		verify(billingDAO).getReport(userInfo, billingFilter);
		verifyNoMoreInteractions(billingDAO);
	}

	@Test
	public void getReportWithAnotherInstanceOfDocument() {
		Document expectedDocument = new Document().append("someField", "someValue");
		Document anotherDocument = new Document().append("someField", "anotherValue");
		when(billingDAO.getReport(any(UserInfo.class), any(AwsBillingFilter.class))).thenReturn(anotherDocument);

		Document actualDocument = awsBillingService.getReport(userInfo, billingFilter);
		assertNotEquals(expectedDocument, actualDocument);

		verify(billingDAO).getReport(userInfo, billingFilter);
		verifyNoMoreInteractions(billingDAO);
	}

	@Test
	public void getReportWithException() {
		doThrow(new RuntimeException()).when(billingDAO).getReport(any(UserInfo.class), any(AwsBillingFilter.class));

		try {
			awsBillingService.getReport(userInfo, billingFilter);
		} catch (DlabException e) {
			assertEquals("Cannot load billing report: null", e.getMessage());
		}

		verify(billingDAO).getReport(userInfo, billingFilter);
		verifyNoMoreInteractions(billingDAO);
	}

	@Test
	public void downloadReport() {
		when(billingDAO.getReport(any(UserInfo.class), any(AwsBillingFilter.class))).thenReturn(basicDocument);

		byte[] result = awsBillingService.downloadReport(userInfo, billingFilter);
		assertNotNull(result);
		assertTrue(result.length > 0);

		verify(billingDAO).getReport(userInfo, billingFilter);
		verifyNoMoreInteractions(billingDAO);
	}

	@Test
	public void downloadReportWithInapproprietaryDateFormatInDocument() {
		basicDocument.put("usage_date_start", "someDateStart");
		when(billingDAO.getReport(any(UserInfo.class), any(AwsBillingFilter.class))).thenReturn(basicDocument);

		try {
			awsBillingService.downloadReport(userInfo, billingFilter);
		} catch (DlabException e) {
			assertEquals("Cannot prepare CSV file", e.getMessage());
		}

		verify(billingDAO).getReport(userInfo, billingFilter);
		verifyNoMoreInteractions(billingDAO);
	}

	@Test
	public void downloadReportWhenDocumentHasNotAllRequiredFields() {
		basicDocument.remove("lines");
		when(billingDAO.getReport(any(UserInfo.class), any(AwsBillingFilter.class))).thenReturn(basicDocument);

		expectedException.expect(NullPointerException.class);

		awsBillingService.downloadReport(userInfo, billingFilter);
	}

	@Test
	public void getReportFileName() {
		String result = awsBillingService.getReportFileName(userInfo, billingFilter);
		assertEquals("aws-billing-report.csv", result);
	}

	@Test
	public void getFirstLine() throws ParseException {
		String result = awsBillingService.getFirstLine(basicDocument);
		assertEquals("Service base name: someSBN  Resource tag ID: someTagResourceId  Available reporting " +
				"period from: Mar 21, 2018 to: Mar 22, 2018", result);
	}

	@Test
	public void getFirstLineWithException() throws ParseException {
		basicDocument.put("usage_date_start", "someStartDate");

		expectedException.expect(ParseException.class);
		expectedException.expectMessage("Unparseable date: \"someStartDate\"");

		awsBillingService.getFirstLine(basicDocument);

	}

	@Test
	public void getHeadersList() {
		List<String> expectedResult1 =
				Arrays.asList("USER", "ENVIRONMENT NAME", "RESOURCE TYPE", "SHAPE", "SERVICE", "SERVICE CHARGES");
		List<String> expectedResult2 = expectedResult1.subList(1, expectedResult1.size());

		List<String> actualResult1 = awsBillingService.getHeadersList(true);
		assertEquals(expectedResult1, actualResult1);

		List<String> actualResult2 = awsBillingService.getHeadersList(false);
		assertEquals(expectedResult2, actualResult2);
	}

	@Test
	public void getLine() {
		String expectedResult1 = "someUser,someId,someResType,someShape,someProduct,someCost someCode\n";
		String actualResult1 = awsBillingService.getLine(true, basicDocument);
		assertEquals(expectedResult1, actualResult1);

		basicDocument.remove("user");
		String expectedResult2 = "someId,someResType,someShape,someProduct,someCost someCode\n";
		String actualResult2 = awsBillingService.getLine(false, basicDocument);
		assertEquals(expectedResult2, actualResult2);
	}

	@Test
	public void getTotal() {
		String expectedResult1 = ",,,,,Total: someCostTotal someCode\n";
		String actualResult1 = awsBillingService.getTotal(true, basicDocument);
		assertEquals(expectedResult1, actualResult1);

		String expectedResult2 = ",,,,Total: someCostTotal someCode\n";
		String actualResult2 = awsBillingService.getTotal(false, basicDocument);
		assertEquals(expectedResult2, actualResult2);
	}

	private UserInfo getUserInfo() {
		return new UserInfo("user", "token");
	}

	private Document getBasicDocument() {
		return new Document().append("service_base_name", "someSBN").append("user", "someUser")
				.append("dlab_id", "someId").append("dlab_resource_type", "someResType")
				.append("tag_resource_id", "someTagResourceId").append("usage_date_start", "2018-03-21")
				.append("usage_date_end", "2018-03-22").append("full_report", false)
				.append("shape", "someShape").append("product", "someProduct").append("cost", "someCost")
				.append("cost_total", "someCostTotal").append("currency_code", "someCode")
				.append("lines", Collections.singletonList(new Document()));
	}

}
