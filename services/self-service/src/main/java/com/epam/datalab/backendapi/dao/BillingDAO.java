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

package com.epam.datalab.backendapi.dao;

import com.epam.datalab.backendapi.domain.BillingReportLine;
import com.epam.datalab.backendapi.resources.dto.BillingFilter;

import java.time.LocalDate;
import java.util.List;

public interface BillingDAO {
    Double getTotalCost();

    Double getUserCost(String user);

    Double getOverallProjectCost(String project);

    Double getMonthlyProjectCost(String project, LocalDate date);

    int getBillingQuoteUsed();

    int getBillingUserQuoteUsed(String user);

    boolean isBillingQuoteReached();

    boolean isUserQuoteReached(String user);

    List<BillingReportLine> findBillingData(String project, String endpoint, List<String> resourceNames);

    List<BillingReportLine> aggregateBillingData(BillingFilter filter);

    void deleteByUsageDate(String application, String usageDate);

    void deleteByUsageDateRegex(String application, String usageDate);

    void save(List<BillingReportLine> billingData);
}
