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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.resources.dto.azure.AzureBillingFilter;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.billing.BillingCalculationUtils;
import com.epam.dlab.billing.DlabResourceType;
import com.epam.dlab.MongoKeyWords;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.epam.dlab.backendapi.dao.MongoCollections.USER_EDGE;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

@Singleton
@Slf4j
public class AzureBillingDAO extends BillingDAO {
    public static final String SIZE = "size";

    public Document getReport(UserInfo userInfo, AzureBillingFilter filter) {

        boolean isFullReport = UserRoles.checkAccess(userInfo, RoleType.PAGE, "/api/infrastructure_provision/billing");
        if (isFullReport) {
            if (filter.getUser() != null) {
                filter.getUser().replaceAll(String::toLowerCase);
            }
        } else {
            filter.setUser(Lists.newArrayList(userInfo.getName().toLowerCase()));
        }

        List<Bson> matchCriteria = matchCriteria(filter);
        List<Bson> pipeline = new ArrayList<>();
        if (!matchCriteria.isEmpty()) {
            pipeline.add(Aggregates.match(Filters.and(matchCriteria)));
        }
        pipeline.add(groupCriteria());
        pipeline.add(sortCriteria());

        return prepareReport(
                filter.getNodeSize() != null && !filter.getNodeSize().isEmpty(),
                getCollection(MongoKeyWords.BILLING_DETAILS).aggregate(pipeline),
                getShapes(filter.getNodeSize()))
                .append(FULL_REPORT, isFullReport);
    }

    private Document prepareReport(boolean filterByShape, AggregateIterable<Document> agg,
                                   Map<String, ShapeInfo> shapes) {

        List<Document> reportItems = new ArrayList<>();

        String usageDateStart = null;
        String usageDateEnd = null;
        double costTotal = 0D;

        for (Document d : agg) {
            Document id = (Document) d.get(MongoKeyWords.MONGO_ID);
            String resourceId = id.getString(MongoKeyWords.DLAB_ID);
            ShapeInfo shape = shapes.get(resourceId);
            if (filterByShape && shape == null) {
                continue;
            }

            String dateStart = d.getString(MongoKeyWords.USAGE_FROM);
            if (StringUtils.compare(usageDateStart, dateStart, false) > 0) {
                usageDateStart = dateStart;
            }
            String dateEnd = d.getString(MongoKeyWords.USAGE_TO);
            if (StringUtils.compare(usageDateEnd, dateEnd) < 0) {
                usageDateEnd = dateEnd;
            }

            costTotal += d.getDouble(MongoKeyWords.COST);

            Document item = new Document()
                    .append(MongoKeyWords.DLAB_USER, id.getString(USER))
                    .append(MongoKeyWords.DLAB_ID, resourceId)
                    .append(SIZE, generateShapeName(shape))
                    .append(MongoKeyWords.METER_CATEGORY, id.getString(MongoKeyWords.METER_CATEGORY))
                    .append(MongoKeyWords.RESOURCE_TYPE,
                            DlabResourceType.getResourceTypeName(id.getString(MongoKeyWords.RESOURCE_TYPE)))
                    .append(MongoKeyWords.COST, d.getDouble(MongoKeyWords.COST))
                    .append(MongoKeyWords.COST_STRING, BillingCalculationUtils.formatDouble(d.getDouble(MongoKeyWords.COST)))
                    .append(MongoKeyWords.CURRENCY_CODE, id.getString(MongoKeyWords.CURRENCY_CODE))
                    .append(MongoKeyWords.USAGE_FROM, dateStart)
                    .append(MongoKeyWords.USAGE_TO, dateEnd);


            reportItems.add(item);
        }

        return new Document()
                .append(SERVICE_BASE_NAME, settings.getServiceBaseName())
                .append(MongoKeyWords.USAGE_FROM, usageDateStart)
                .append(MongoKeyWords.USAGE_TO, usageDateEnd)
                .append(ITEMS, reportItems)
                .append(MongoKeyWords.COST_STRING, BillingCalculationUtils.formatDouble(BillingCalculationUtils.round(costTotal, 2)))
                .append(MongoKeyWords.CURRENCY_CODE, (reportItems.isEmpty() ? null :
                        reportItems.get(0).getString(MongoKeyWords.CURRENCY_CODE)));

    }

    private List<Bson> matchCriteria(AzureBillingFilter filter) {

        List<Bson> searchCriteria = new ArrayList<>();

        if (filter.getUser() != null && !filter.getUser().isEmpty()) {
            searchCriteria.add(Filters.in(MongoKeyWords.DLAB_USER, filter.getUser()));
        }

        if (filter.getCategory() != null && !filter.getCategory().isEmpty()) {
            searchCriteria.add(Filters.in(MongoKeyWords.METER_CATEGORY, filter.getCategory()));
        }

        if (filter.getResourceType() != null && !filter.getResourceType().isEmpty()) {
            searchCriteria.add(Filters.in(MongoKeyWords.RESOURCE_TYPE,
                    DlabResourceType.getResourceTypeIds(filter.getResourceType())));
        }

        if (filter.getDlabId() != null && !filter.getDlabId().isEmpty()) {
            searchCriteria.add(regex(MongoKeyWords.DLAB_ID, filter.getDlabId(), "i"));
        }

        if (filter.getDateStart() != null && !filter.getDateStart().isEmpty()) {
            searchCriteria.add(gte(MongoKeyWords.USAGE_DAY, filter.getDateStart()));
        }
        if (filter.getDateEnd() != null && !filter.getDateEnd().isEmpty()) {
            searchCriteria.add(lte(MongoKeyWords.USAGE_DAY, filter.getDateEnd()));
        }

        return searchCriteria;
    }

    private Bson groupCriteria() {
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

    private Bson sortCriteria() {
        return Aggregates.sort(Sorts.ascending(
                MongoKeyWords.prependId(MongoKeyWords.DLAB_USER),
                MongoKeyWords.prependId(MongoKeyWords.DLAB_ID),
                MongoKeyWords.prependId(MongoKeyWords.RESOURCE_TYPE),
                MongoKeyWords.prependId(MongoKeyWords.METER_CATEGORY)));
    }

    @Override
    protected void appendSsnAndEdgeNodeType(List<String> shapeNames, Map<String, ShapeInfo> shapes) {

        String serviceBaseName = settings.getServiceBaseName().replace("_", "-").toLowerCase();

        final String ssnSize = settings.getAzureSsnInstanceSize();
        if (shapeNames == null || shapeNames.isEmpty() || shapeNames.contains(ssnSize)) {
            shapes.put(serviceBaseName + "-ssn", new BillingDAO.ShapeInfo(ssnSize));
        }


        final String edgeSize = settings.getAzureEdgeInstanceSize();
        if (shapeNames == null || shapeNames.isEmpty() || shapeNames.contains(edgeSize)) {
            FindIterable<Document> docs = getCollection(USER_EDGE).find().projection(fields(include(INSTANCE_ID)));
            for (Document d : docs) {
                shapes.put(d.getString(INSTANCE_ID), new BillingDAO.ShapeInfo(edgeSize));
            }
        }
    }
}
