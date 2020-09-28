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

package com.epam.datalab;

import com.epam.datalab.configuration.BillingToolConfiguration;
import com.epam.datalab.configuration.BillingToolConfigurationFactory;
import com.epam.datalab.core.parser.ParserBase;
import com.epam.datalab.dto.billing.BillingData;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.util.ServiceUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.datalab.model.aws.ReportLine.FIELD_COST;
import static com.epam.datalab.model.aws.ReportLine.FIELD_CURRENCY_CODE;
import static com.epam.datalab.model.aws.ReportLine.FIELD_DATALAB_ID;
import static com.epam.datalab.model.aws.ReportLine.FIELD_PRODUCT;
import static com.epam.datalab.model.aws.ReportLine.FIELD_RESOURCE_TYPE;
import static com.epam.datalab.model.aws.ReportLine.FIELD_USAGE_DATE;

@Service
public class BillingServiceImpl implements BillingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BillingServiceImpl.class);
    private static BillingToolConfiguration configuration;

    public List<BillingData> getBillingData() {
        try {
            ParserBase parser = configuration.build();

            List<BillingData> billingData = parser.parse()
                    .stream()
                    .map(this::toBillingData)
                    .collect(Collectors.toList());

            if (!parser.getStatistics().isEmpty()) {
                LOGGER.info("Billing report parser statistics:");
                for (int i = 0; i < parser.getStatistics().size(); i++) {
                    LOGGER.info("  {}", parser.getStatistics().get(i).toString());
                }
            }

            return billingData;
        } catch (Exception e) {
            LOGGER.error("Something went wrong ", e);
            return Collections.emptyList();
        }
    }

    private BillingData toBillingData(Document billingData) {
        return BillingData.builder()
                .tag(billingData.getString(FIELD_DATALAB_ID).toLowerCase())
                .usageDateFrom(Optional.ofNullable(billingData.getString(FIELD_USAGE_DATE)).map(LocalDate::parse).orElse(null))
                .usageDateTo(Optional.ofNullable(billingData.getString(FIELD_USAGE_DATE)).map(LocalDate::parse).orElse(null))
                .usageDate(billingData.getString(FIELD_USAGE_DATE))
                .product(billingData.getString(FIELD_PRODUCT))
                .usageType(billingData.getString(FIELD_RESOURCE_TYPE))
                .cost(billingData.getDouble(FIELD_COST))
                .currency(billingData.getString(FIELD_CURRENCY_CODE))
                .build();
    }

    public static void initialize(String filename) throws InitializationException {
        LOGGER.debug("Billing report configuration file: {}", filename);
        configuration = BillingToolConfigurationFactory.build(filename, BillingToolConfiguration.class);
    }

    public static void startApplication(String[] args) throws InitializationException {
        if (ServiceUtils.printAppVersion(BillingTool.class, args)) {
            return;
        }

        String confName = null;
        for (int i = 0; i < args.length; i++) {
            if (BillingTool.isKey("help", args[i])) {
                i++;
                Help.usage(i < args.length ? Arrays.copyOfRange(args, i, args.length) : null);
                return;
            } else if (BillingTool.isKey("conf", args[i])) {
                i++;
                if (i < args.length) {
                    confName = args[i];
                } else {
                    throw new InitializationException("Missing the name of configuration file");
                }
            }
        }

        if (confName == null) {
            Help.usage();
            throw new InitializationException("Missing arguments");
        }

        BillingTool.setLoggerLevel();
        try {
            initialize(confName);
        } catch (Exception e) {
            throw new DatalabException("Billing scheduler failed", e);
        }
    }
}
