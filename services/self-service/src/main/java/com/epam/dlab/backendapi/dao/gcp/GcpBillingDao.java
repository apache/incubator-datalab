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

import com.epam.dlab.backendapi.dao.BaseBillingDAO;
import com.epam.dlab.backendapi.resources.dto.gcp.GcpBillingFilter;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Collections;
import java.util.List;

import static com.epam.dlab.MongoKeyWords.USAGE_FROM;
import static com.epam.dlab.MongoKeyWords.USAGE_TO;
import static com.epam.dlab.backendapi.dao.aws.AwsBillingDAO.DLAB_RESOURCE_TYPE;
import static com.epam.dlab.model.aws.ReportLine.*;
import static com.mongodb.client.model.Accumulators.*;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.sort;

public class GcpBillingDao extends BaseBillingDAO<GcpBillingFilter> {
    @Override
    protected Bson sortCriteria() {
        return sort(new Document(ID + "." + USER, 1)
                .append(ID + "." + FIELD_DLAB_ID, 1)
                .append(ID + "." + FIELD_PRODUCT, 1));
    }

    @Override
    protected Bson groupCriteria() {
        return group(getGroupingFields(USER, FIELD_DLAB_ID, DLAB_RESOURCE_TYPE, FIELD_PRODUCT,
                currencyCodeFieldName(), FIELD_PROJECT),
                sum(FIELD_COST, "$" + FIELD_COST),
                min(USAGE_FROM, "$" + FIELD_USAGE_DATE),
                max(USAGE_TO, "$" + FIELD_USAGE_DATE)
        );
    }

    @Override
    protected List<Bson> cloudMatchCriteria(GcpBillingFilter filter) {
        return Collections.emptyList();
    }


    @Override
    protected String getSsnShape() {
        return "t2.medium";
    }
}
