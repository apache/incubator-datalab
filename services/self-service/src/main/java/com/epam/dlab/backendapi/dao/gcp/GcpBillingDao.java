/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.epam.dlab.backendapi.dao.gcp;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.BaseBillingDAO;
import com.epam.dlab.backendapi.resources.dto.gcp.GcpBillingFilter;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.billing.BillingCalculationUtils;
import com.epam.dlab.billing.DlabResourceType;
import com.epam.dlab.dto.UserInstanceStatus;
import com.mongodb.client.AggregateIterable;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.epam.dlab.MongoKeyWords.*;
import static com.epam.dlab.backendapi.dao.MongoCollections.BILLING;
import static com.epam.dlab.backendapi.dao.aws.AwsBillingDAO.DLAB_RESOURCE_TYPE;
import static com.epam.dlab.backendapi.dao.aws.AwsBillingDAO.TAG_RESOURCE_ID;
import static com.epam.dlab.model.aws.ReportLine.*;
import static com.mongodb.client.model.Accumulators.*;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.in;

public class GcpBillingDao extends BaseBillingDAO<GcpBillingFilter> {
    @Override
    public Double getTotalCost() {
        return null;
    }

    @Override
    public Double getUserCost(String user) {
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
    protected void appendSsnAndEdgeNodeType(List<String> shapeNames, Map<String, ShapeInfo> shapes) {

    }

    public Document getReport(UserInfo userInfo, GcpBillingFilter filter) {
        // Create filter
        List<Bson> conditions = new ArrayList<>();
        boolean isFullReport = UserRoles.checkAccess(userInfo, RoleType.PAGE, "/api/infrastructure_provision/billing");
        setUserFilter(userInfo, filter, isFullReport);
        addCondition(conditions, USER, filter.getUser());
        addCondition(conditions, FIELD_PRODUCT, filter.getProduct());

        // Create aggregation conditions

        List<Bson> pipeline = new ArrayList<>();
        if (!conditions.isEmpty()) {
            pipeline.add(match(and(conditions)));
        }
        pipeline.add(
                group(getGroupingFields(USER, FIELD_DLAB_ID, DLAB_RESOURCE_TYPE, FIELD_PRODUCT, FIELD_RESOURCE_TYPE,
                        FIELD_CURRENCY_CODE),
                        sum(FIELD_COST, "$" + FIELD_COST),
                        min(USAGE_FROM, "$" + FIELD_USAGE_DATE),
                        max(USAGE_TO, "$" + FIELD_USAGE_DATE)
                ));
        pipeline.add(
                sort(new Document(ID + "." + USER, 1)
                        .append(ID + "." + FIELD_DLAB_ID, 1)
                        .append(ID + "." + RESOURCE_TYPE, 1)
                        .append(ID + "." + FIELD_PRODUCT, 1))
        );

        // Get billing report and the list of shape info
        AggregateIterable<Document> agg = getCollection(BILLING).aggregate(pipeline);
        Map<String, ShapeInfo> shapes = getShapes(filter.getShape());

        // Build billing report lines
        List<Document> reportItems = new ArrayList<>();
        boolean filterByShape = !(filter.getShape() == null || filter.getShape().isEmpty());
        String usageDateStart = null;
        String usageDateEnd = null;
        double costTotal = 0;

        for (Document d : agg) {
            Document id = (Document) d.get(ID);
            String resourceId = id.getString(FIELD_DLAB_ID);
            ShapeInfo shape = shapes.get(resourceId);
            final UserInstanceStatus status = Optional.ofNullable(shape).map(ShapeInfo::getStatus).orElse(null);
            if ((filterByShape && shape == null) || (!filter.getStatuses().isEmpty() && filter.getStatuses().stream()
                    .noneMatch(s -> s.equals(status)))) {
                continue;
            }

            String resourceTypeId = DlabResourceType.getResourceTypeName(id.getString(DLAB_RESOURCE_TYPE));
            String shapeName = generateShapeName(shape);
            String dateStart = d.getString(USAGE_FROM);
            if (StringUtils.compare(usageDateStart, dateStart, false) > 0) {
                usageDateStart = dateStart;
            }
            String dateEnd = d.getString(USAGE_TO);
            if (StringUtils.compare(usageDateEnd, dateEnd) < 0) {
                usageDateEnd = dateEnd;
            }
            double cost = BillingCalculationUtils.round(d.getDouble(FIELD_COST), 2);
            costTotal += cost;

            Document item = new Document()
                    .append(FIELD_USER_ID, getUserOrDefault(id.getString(USER)))
                    .append(FIELD_DLAB_ID, resourceId)
                    .append(DLAB_RESOURCE_TYPE, resourceTypeId)
                    .append(SHAPE, shapeName)
                    .append(STATUS,
                            Optional.ofNullable(status).map(UserInstanceStatus::toString).orElse(StringUtils.EMPTY))
                    .append(FIELD_PRODUCT, id.getString(FIELD_PRODUCT))
                    .append(FIELD_RESOURCE_TYPE, id.getString(FIELD_RESOURCE_TYPE))
                    .append(FIELD_COST, BillingCalculationUtils.formatDouble(cost))
                    .append(FIELD_CURRENCY_CODE, id.getString(FIELD_CURRENCY_CODE))
                    .append(USAGE_FROM, dateStart)
                    .append(USAGE_TO, dateEnd);
            reportItems.add(item);
        }

        return new Document()
                .append(SERVICE_BASE_NAME, settings.getServiceBaseName())
                .append(TAG_RESOURCE_ID, settings.getConfTagResourceId())
                .append(USAGE_FROM, usageDateStart)
                .append(USAGE_TO, usageDateEnd)
                .append(ITEMS, reportItems)
                .append(COST_TOTAL, BillingCalculationUtils.formatDouble(BillingCalculationUtils.round(costTotal, 2)))
                .append(FIELD_CURRENCY_CODE, (reportItems.isEmpty() ? null :
                        reportItems.get(0).getString(FIELD_CURRENCY_CODE)))
                .append(FULL_REPORT, isFullReport);
    }

    private void addCondition(List<Bson> conditions, String fieldName, List<String> values) {
        if (values != null && !values.isEmpty()) {
            conditions.add(in(fieldName, values));
        }
    }

}
