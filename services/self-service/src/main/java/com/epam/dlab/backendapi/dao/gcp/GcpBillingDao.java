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

package com.epam.dlab.backendapi.dao.gcp;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.resources.dto.BillingFilter;
import org.bson.Document;

public class GcpBillingDao implements BillingDAO<BillingFilter> {
	@Override
	public Double getTotalCost() {
		return null;
	}

	@Override
	public Double getUserCost(String user) {
		return null;
	}

	@Override
	public Double getProjectCost(String project) {
		return null;
	}

	@Override
	public int getBillingQuoteUsed() {
		return 0;
	}

	@Override
	public int getBillingUserQuoteUsed(String user) {
		return 0;
	}

	@Override
	public boolean isBillingQuoteReached() {
		return false;
	}

	@Override
	public boolean isUserQuoteReached(String user) {
		return false;
	}

	@Override
	public boolean isProjectQuoteReached(String project) {
		return false;
	}

	@Override
	public Document getReport(UserInfo userInfo, BillingFilter filter) {
		return null;
	}
}
