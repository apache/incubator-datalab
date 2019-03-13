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

package com.epam.dlab.backendapi.dao.azure;

import com.epam.dlab.MongoKeyWords;
import com.epam.dlab.backendapi.dao.BaseBillingDAO;
import com.epam.dlab.backendapi.resources.dto.azure.AzureBillingFilter;
import com.epam.dlab.billing.DlabResourceType;
import com.google.inject.Singleton;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class AzureBillingDAO extends BaseBillingDAO<AzureBillingFilter> {
    public static final String SIZE = "size";

    @Override
    protected List<Bson> cloudMatchCriteria(AzureBillingFilter filter) {
        if (!filter.getCategory().isEmpty()) {
            return Collections.singletonList(Filters.in(MongoKeyWords.METER_CATEGORY, filter.getCategory()));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected Bson groupCriteria() {
        return Aggregates.group(getGroupingFields(
                MongoKeyWords.DLAB_USER,
                MongoKeyWords.DLAB_ID,
                MongoKeyWords.RESOURCE_TYPE,
                MongoKeyWords.METER_CATEGORY,
                MongoKeyWords.CURRENCY_CODE),
                Accumulators.sum(MongoKeyWords.COST, MongoKeyWords.prepend$(MongoKeyWords.COST)),
                Accumulators.min(MongoKeyWords.USAGE_FROM, MongoKeyWords.prepend$(MongoKeyWords.USAGE_DAY)),
                Accumulators.max(MongoKeyWords.USAGE_TO, MongoKeyWords.prepend$(MongoKeyWords.USAGE_DAY))
        );
    }

    @Override
    protected Bson sortCriteria() {
        return Aggregates.sort(Sorts.ascending(
                MongoKeyWords.prependId(MongoKeyWords.DLAB_USER),
                MongoKeyWords.prependId(MongoKeyWords.DLAB_ID),
                MongoKeyWords.prependId(MongoKeyWords.RESOURCE_TYPE),
                MongoKeyWords.prependId(MongoKeyWords.METER_CATEGORY)));
    }

    @Override
    protected String getServiceBaseName() {
        return settings.getServiceBaseName().replace("_", "-").toLowerCase();
    }

    @Override
    protected String getEdgeSize() {
        return settings.getAzureEdgeInstanceSize();
    }

    @Override
    protected String edgeId(Document d) {
        return d.getString(INSTANCE_ID);
    }

    @Override
    protected String getSsnShape() {
        return settings.getAzureSsnInstanceSize();
    }

    @Override
    protected String shapeFieldName() {
        return SIZE;
    }

    @Override
    protected String dlabIdFieldName() {
        return MongoKeyWords.DLAB_ID;
    }

    @Override
    protected String productFieldName() {
        return MongoKeyWords.METER_CATEGORY;
    }

    @Override
    protected String costFieldName() {
        return MongoKeyWords.COST_STRING;
    }

    @Override
    protected String usageDateFromFieldName() {
        return MongoKeyWords.USAGE_FROM;
    }

    @Override
    protected String usageDateToFieldName() {
        return MongoKeyWords.USAGE_TO;
    }

    @Override
    protected String currencyCodeFieldName() {
        return MongoKeyWords.CURRENCY_CODE;
    }

    @Override
    protected String resourceType(Document id) {
        return DlabResourceType.getResourceTypeName(id.getString(MongoKeyWords.RESOURCE_TYPE));
    }

}
