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

package com.epam.datalab.mongo;

import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.exceptions.ParseException;
import com.epam.datalab.model.aws.ReportLine;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mongodb.client.model.Filters.eq;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Provides Mongo DAO for billing resources in DataLab.
 */
public class DatalabResourceTypeDAO implements MongoConstants {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatalabResourceTypeDAO.class);

    /**
     * Mongo database connection.
     */
    private final MongoDbConnection connection;

    /**
     * Service base name.
     */
    private String serviceBaseName;
    private String serviceBaseNameId;

    /**
     * Instantiate DAO for billing resources.
     *
     * @param connection the connection to Mongo DB.
     * @throws InitializationException
     */
    public DatalabResourceTypeDAO(MongoDbConnection connection) throws InitializationException {
        this.connection = connection;
        setServiceBaseName();
    }

    /**
     * Returns the base name of service.
     */
    public String getServiceBaseName() {
        return serviceBaseName;
    }

    /**
     * Set the base name of service.
     *
     * @throws InitializationException
     */
    private void setServiceBaseName() throws InitializationException {
        Document d = connection.getCollection(COLLECTION_SETTINGS)
                .find(eq(FIELD_ID, FIELD_SERIVICE_BASE_NAME))
                .first();
        if (d == null) {
            throw new InitializationException("Service base name property " + COLLECTION_SETTINGS +
                    "." + FIELD_SERIVICE_BASE_NAME + " in Mongo DB not found");
        }
        String value = d.getOrDefault("value", EMPTY).toString();
        if (d.isEmpty()) {
            throw new InitializationException("Service base name property " + COLLECTION_SETTINGS +
                    "." + FIELD_SERIVICE_BASE_NAME + " in Mongo DB is empty");
        }
        serviceBaseName = value;
        serviceBaseNameId = value + ":";
        LOGGER.debug("serviceBaseName is {}", serviceBaseName);
    }

    /**
     * Convert and return the report line of billing to Mongo document.
     *
     * @param row report line.
     * @return Mongo document.
     * @throws ParseException
     */
    public Document transform(ReportLine row) throws ParseException {
        String resourceId = row.getDatalabId();
        if (resourceId == null || !resourceId.startsWith(serviceBaseNameId)) {
            throw new ParseException("DatalabId is not match: expected start with " + serviceBaseNameId + ", actual " +
                    resourceId);
        }
        resourceId = resourceId.substring(serviceBaseNameId.length());
        Document d = new Document(ReportLine.FIELD_DATALAB_ID, resourceId);
        return d.append(ReportLine.FIELD_USAGE_DATE, row.getUsageDate())
                .append(ReportLine.FIELD_PRODUCT, row.getProduct())
                .append(ReportLine.FIELD_USAGE_TYPE, row.getUsageType())
                .append(ReportLine.FIELD_USAGE, row.getUsage())
                .append(ReportLine.FIELD_COST, row.getCost())
                .append(ReportLine.FIELD_CURRENCY_CODE, row.getCurrencyCode())
                .append(ReportLine.FIELD_RESOURCE_TYPE, row.getResourceType().category())
                .append(ReportLine.FIELD_RESOURCE_ID, row.getResourceId())
                .append(ReportLine.FIELD_TAGS, row.getTags());
    }
}
