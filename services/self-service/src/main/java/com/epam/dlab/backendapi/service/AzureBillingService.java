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

package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.azure.AzureBillingDAO;
import com.epam.dlab.backendapi.resources.dto.azure.AzureBillingFilter;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@Slf4j
@Singleton
public class AzureBillingService implements BillingService<AzureBillingFilter> {

    @Inject
    private AzureBillingDAO billingDAO;

    @Override
    public Document getReport(UserInfo userInfo, AzureBillingFilter filter) {
        log.trace("Get billing report for user {} with filter {}", userInfo.getName(), filter);
        try {
            return billingDAO.getReport(userInfo, filter);
        } catch (RuntimeException t) {
            log.error("Cannot load billing report for user {} with filter {}", userInfo.getName(), filter, t);
            throw new DlabException("Cannot load billing report: " + t.getLocalizedMessage(), t);
        }
    }

    @Override
    public byte[] downloadReport(UserInfo userInfo, AzureBillingFilter filter) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String getReportFileName(UserInfo userInfo, AzureBillingFilter filter) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
