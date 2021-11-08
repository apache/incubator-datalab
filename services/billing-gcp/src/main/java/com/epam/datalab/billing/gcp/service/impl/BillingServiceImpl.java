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

package com.epam.datalab.billing.gcp.service.impl;

import com.epam.datalab.billing.gcp.dao.BillingDAO;
import com.epam.datalab.billing.gcp.service.BillingService;
import com.epam.datalab.dto.billing.BillingData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class BillingServiceImpl implements BillingService {

    private final BillingDAO billingDAO;

    @Autowired
    public BillingServiceImpl(BillingDAO billingDAO) {
        this.billingDAO = billingDAO;
    }

    @Override
    public List<BillingData> getBillingData() {
        try {
            List<BillingData> billingData = billingDAO.getBillingData();
            log.info("TEST LOG BILLING: billingData: {}", billingData);
            return billingData;
        } catch (Exception e) {
            log.error("Can not update billing due to: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
