/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.dlab.backendapi.service.azure;

import com.epam.dlab.MongoKeyWords;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.BaseBillingDAO;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.dao.azure.AzureBillingDAO;
import com.epam.dlab.backendapi.resources.dto.azure.AzureBillingFilter;
import com.epam.dlab.backendapi.service.BillingService;
import com.epam.dlab.backendapi.util.CSVFormatter;
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
public class AzureBillingService extends BillingService<AzureBillingFilter> {

    @Inject
    private BillingDAO billingDAO;

    @Override
    public String getReportFileName(UserInfo userInfo, AzureBillingFilter filter) {
        return "azure-billing-report.csv";
    }

    @Override
    public String getFirstLine(Document document) throws ParseException {
        SimpleDateFormat from = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat to = new SimpleDateFormat("MMM dd, yyyy");

        return String.format("Service base name: %s  " +
                        "Available reporting period from: %s to: %s",
                document.get(BaseBillingDAO.SERVICE_BASE_NAME),
                to.format(from.parse((String) document.get(MongoKeyWords.USAGE_FROM))),
                to.format(from.parse((String) document.get(MongoKeyWords.USAGE_TO))));
    }

    public List<String> getHeadersList(boolean full) {
        List<String> headers = new ArrayList<>();

        if (full) {
            headers.add("USER");
        }

        headers.add("PROJECT");
        headers.add("ENVIRONMENT NAME");
        headers.add("RESOURCE TYPE");
        headers.add("INSTANCE SIZE");
        headers.add("CATEGORY");
        headers.add("SERVICE CHARGES");

        return headers;
    }

    @Override
    public String getLine(boolean full, Document document) {
        List<String> items = new ArrayList<>();

        if (full) {
            items.add(getValueOrEmpty(document, MongoKeyWords.DLAB_USER));
        }

        items.add(getValueOrEmpty(document, ReportLine.FIELD_PROJECT));
        items.add(getValueOrEmpty(document, MongoKeyWords.DLAB_ID));
        items.add(getValueOrEmpty(document, MongoKeyWords.RESOURCE_TYPE));
        items.add(getValueOrEmpty(document, AzureBillingDAO.SIZE).replace(System.lineSeparator(), " "));
        items.add(getValueOrEmpty(document, MongoKeyWords.METER_CATEGORY));

        items.add(getValueOrEmpty(document, MongoKeyWords.COST_STRING)
                + " " + getValueOrEmpty(document, MongoKeyWords.CURRENCY_CODE));

        return CSVFormatter.formatLine(items, CSVFormatter.SEPARATOR);
    }

    @Override
    public String getTotal(boolean full, Document document) {
        int padding = getHeadersList(full).size() - 1;

        List<String> items = new ArrayList<>();
        while (padding-- > 0) {
            items.add("");
        }

        items.add(String.format("Total: %s %s", getValueOrEmpty(document, MongoKeyWords.COST_STRING),
                getValueOrEmpty(document, MongoKeyWords.CURRENCY_CODE)));

        return CSVFormatter.formatLine(items, CSVFormatter.SEPARATOR);
    }
}
