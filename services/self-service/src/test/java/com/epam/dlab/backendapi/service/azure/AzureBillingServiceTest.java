package com.epam.dlab.backendapi.service.azure;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.azure.AzureBillingDAO;
import com.epam.dlab.backendapi.resources.dto.azure.AzureBillingFilter;
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
public class AzureBillingServiceTest {

	private UserInfo userInfo;
	private AzureBillingFilter billingFilter;
	private Document basicDocument;

	@Mock
	private AzureBillingDAO billingDAO;

	@InjectMocks
	private AzureBillingService azureBillingService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		userInfo = getUserInfo();
		billingFilter = new AzureBillingFilter();
		basicDocument = getBasicDocument();
	}

	@Test
	public void getReportWithTheSameInstanceOfDocument() {
		Document expectedDocument = new Document();
		when(billingDAO.getReport(any(UserInfo.class), any(AzureBillingFilter.class))).thenReturn(expectedDocument);

		Document actualDocument = azureBillingService.getReport(userInfo, billingFilter);
		assertEquals(expectedDocument, actualDocument);

		verify(billingDAO).getReport(userInfo, billingFilter);
		verifyNoMoreInteractions(billingDAO);
	}

	@Test
	public void getReportWithAnotherInstanceOfDocument() {
		Document expectedDocument = new Document().append("someField", "someValue");
		Document anotherDocument = new Document().append("someField", "anotherValue");
		when(billingDAO.getReport(any(UserInfo.class), any(AzureBillingFilter.class))).thenReturn(anotherDocument);

		Document actualDocument = azureBillingService.getReport(userInfo, billingFilter);
		assertNotEquals(expectedDocument, actualDocument);

		verify(billingDAO).getReport(userInfo, billingFilter);
		verifyNoMoreInteractions(billingDAO);
	}

	@Test
	public void getReportWithException() {
		doThrow(new RuntimeException()).when(billingDAO).getReport(any(UserInfo.class), any(AzureBillingFilter.class));

		try {
			azureBillingService.getReport(userInfo, billingFilter);
		} catch (DlabException e) {
			assertEquals("Cannot load billing report: null", e.getMessage());
		}

		verify(billingDAO).getReport(userInfo, billingFilter);
		verifyNoMoreInteractions(billingDAO);
	}

	@Test
	public void downloadReport() {
		when(billingDAO.getReport(any(UserInfo.class), any(AzureBillingFilter.class))).thenReturn(basicDocument);

		byte[] result = azureBillingService.downloadReport(userInfo, billingFilter);
		assertNotNull(result);
		assertTrue(result.length > 0);

		verify(billingDAO).getReport(userInfo, billingFilter);
		verifyNoMoreInteractions(billingDAO);
	}

	@Test
	public void downloadReportWithInapproprietaryDateFormatInDocument() {
		basicDocument.put("from", "someDateStart");
		when(billingDAO.getReport(any(UserInfo.class), any(AzureBillingFilter.class))).thenReturn(basicDocument);

		try {
			azureBillingService.downloadReport(userInfo, billingFilter);
		} catch (DlabException e) {
			assertEquals("Cannot prepare CSV file", e.getMessage());
		}

		verify(billingDAO).getReport(userInfo, billingFilter);
		verifyNoMoreInteractions(billingDAO);
	}

	@Test
	public void downloadReportWhenDocumentHasNotAllRequiredFields() {
		basicDocument.remove("lines");
		when(billingDAO.getReport(any(UserInfo.class), any(AzureBillingFilter.class))).thenReturn(basicDocument);

		expectedException.expect(NullPointerException.class);

		azureBillingService.downloadReport(userInfo, billingFilter);
	}

	@Test
	public void getReportFileName() {
		String result = azureBillingService.getReportFileName(userInfo, billingFilter);
		assertEquals("azure-billing-report.csv", result);
	}

	@Test
	public void getFirstLine() throws ParseException {
			String result = azureBillingService.getFirstLine(basicDocument);
			assertEquals("Service base name: someSBN  Available reporting period from: Mar 21, 2018 " +
					"to: Mar 22, 2018", result);
	}

	@Test
	public void getFirstLineWithException() throws ParseException {
		basicDocument.put("from", "someStartDate");

		expectedException.expect(ParseException.class);

		expectedException.expectMessage("Unparseable date: \"someStartDate\"");
		azureBillingService.getFirstLine(basicDocument);
	}

	@Test
	public void getHeadersList() {
		List<String> expectedResult1 =
				Arrays.asList("USER", "ENVIRONMENT NAME", "RESOURCE TYPE", "INSTANCE SIZE", "CATEGORY", "SERVICE " +
						"CHARGES");
		List<String> expectedResult2 = expectedResult1.subList(1, expectedResult1.size());

		List<String> actualResult1 = azureBillingService.getHeadersList(true);
		assertEquals(expectedResult1, actualResult1);

		List<String> actualResult2 = azureBillingService.getHeadersList(false);
		assertEquals(expectedResult2, actualResult2);
	}

	@Test
	public void getLine() {
		String expectedResult1 = "someUser,someId,someResType,someSize,someMeterCategory,someCost someCode\n";
		String actualResult1 = azureBillingService.getLine(true, basicDocument);
		assertEquals(expectedResult1, actualResult1);

		basicDocument.remove("user");
		String expectedResult2 = "someId,someResType,someSize,someMeterCategory,someCost someCode\n";
		String actualResult2 = azureBillingService.getLine(false, basicDocument);
		assertEquals(expectedResult2, actualResult2);
	}

	@Test
	public void getTotal() {
		String expectedResult1 = ",,,,,Total: someCost someCode\n";
		String actualResult1 = azureBillingService.getTotal(true, basicDocument);
		assertEquals(expectedResult1, actualResult1);

		String expectedResult2 = ",,,,Total: someCost someCode\n";
		String actualResult2 = azureBillingService.getTotal(false, basicDocument);
		assertEquals(expectedResult2, actualResult2);
	}

	private UserInfo getUserInfo() {
		return new UserInfo("user", "token");
	}

	private Document getBasicDocument() {
		return new Document().append("service_base_name", "someSBN").append("user", "someUser")
				.append("dlabId", "someId").append("resourceType", "someResType")
				.append("from", "2018-03-21").append("size", "someSize")
				.append("to", "2018-03-22").append("full_report", false)
				.append("meterCategory", "someMeterCategory").append("costString", "someCost")
				.append("currencyCode", "someCode").append("lines", Collections.singletonList(new Document()));
	}

}
