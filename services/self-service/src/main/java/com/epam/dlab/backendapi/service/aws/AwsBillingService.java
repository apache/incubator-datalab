/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
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
import com.epam.dlab.backendapi.service.BillingService;
import com.epam.dlab.backendapi.util.CSVFormatter;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.aws.ReportLine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class AwsBillingService implements BillingService<AwsBillingFilter> {

	@Inject
	private AwsBillingDAO billingDAO;

	@Override
	public Document getReport(UserInfo userInfo, AwsBillingFilter filter) {
		log.trace("Get billing report for user {} with filter {}", userInfo.getName(), filter);
		try {
			return billingDAO.getReport(userInfo, filter);
		} catch (RuntimeException t) {
			log.error("Cannot load billing report for user {} with filter {}", userInfo.getName(), filter, t);
			throw new DlabException("Cannot load billing report: " + t.getLocalizedMessage(), t);
		}
	}

	@Override
	public byte[] downloadReport(UserInfo userInfo, AwsBillingFilter filter) {
		log.trace("Download billing report for user {} with filter {}", userInfo.getName(), filter);
		return BillingService.super.downloadReport(userInfo, filter);
	}

	@Override
	public String getReportFileName(UserInfo userInfo, AwsBillingFilter filter) {
		return "aws-billing-report.csv";
	}

	public String getFirstLine(Document document) throws ParseException {

		SimpleDateFormat from = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat to = new SimpleDateFormat("MMM dd, yyyy");

		return String.format("Service base name: %s  " +
						"Resource tag ID: %s  " +
						"Available reporting period from: %s to: %s",
				document.get(AwsBillingDAO.SERVICE_BASE_NAME), document.get(AwsBillingDAO.TAG_RESOURCE_ID),
				to.format(from.parse((String) document.get(AwsBillingDAO.USAGE_DATE_START))),
				to.format(from.parse((String) document.get(AwsBillingDAO.USAGE_DATE_END))));

	}

	public List<String> getHeadersList(boolean full) {
		List<String> headers = new ArrayList<>();

		if (full) {
			headers.add("USER");
		}

		headers.add("ENVIRONMENT NAME");
		headers.add("RESOURCE TYPE");
		headers.add("SHAPE");
		headers.add("SERVICE");
		headers.add("SERVICE CHARGES");

		return headers;
	}

	public String getLine(boolean full, Document document) {
		List<String> items = new ArrayList<>();

		if (full) {
			items.add(getValueOrEmpty(document, ReportLine.FIELD_USER_ID));
		}

		items.add(getValueOrEmpty(document, ReportLine.FIELD_DLAB_ID));
		items.add(getValueOrEmpty(document, AwsBillingDAO.DLAB_RESOURCE_TYPE));
		items.add(getValueOrEmpty(document, AwsBillingDAO.SHAPE).replace(System.lineSeparator(), " "));
		items.add(getValueOrEmpty(document, ReportLine.FIELD_PRODUCT));

		items.add(getValueOrEmpty(document, ReportLine.FIELD_COST)
				+ " " + getValueOrEmpty(document, ReportLine.FIELD_CURRENCY_CODE));

		return CSVFormatter.formatLine(items, CSVFormatter.SEPARATOR);
	}

	public String getTotal(boolean full, Document document) {
		int padding = getHeadersList(full).size() - 1;

		List<String> items = new ArrayList<>();
		while (padding-- > 0) {
			items.add("");
		}

		items.add(String.format("Total: %s %s", getValueOrEmpty(document, AwsBillingDAO.COST_TOTAL),
				getValueOrEmpty(document, ReportLine.FIELD_CURRENCY_CODE)));

		return CSVFormatter.formatLine(items, CSVFormatter.SEPARATOR);

	}
}
