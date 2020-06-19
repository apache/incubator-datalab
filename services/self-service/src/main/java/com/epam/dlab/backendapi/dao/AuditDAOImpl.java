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

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.domain.AuditDTO;
import com.epam.dlab.backendapi.domain.AuditPaginationDTO;
import com.epam.dlab.exceptions.DlabException;
import com.mongodb.client.model.Facet;
import com.mongodb.client.model.Filters;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.epam.dlab.backendapi.dao.ComputationalDAO.PROJECT;
import static com.mongodb.client.model.Aggregates.count;
import static com.mongodb.client.model.Aggregates.facet;
import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.skip;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;

public class AuditDAOImpl extends BaseDAO implements AuditDAO {
    private final static String AUDIT_COLLECTION = "audit";
    private static final String RESOURCE_NAME_FIELD = "resourceName";
    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String COUNT_FIELD = "count";
    private static final String AUDIT_FACET = "audit";
    private static final String TOTAL_COUNT_FACET = "totalCount";

    @Override
    public void save(AuditDTO audit) {
        insertOne(AUDIT_COLLECTION, audit);
    }

    @Override
    public List<AuditPaginationDTO> getAudit(List<String> users, List<String> projects, List<String> resourceNames, String dateStart, String dateEnd,
                                             int pageNumber, int pageSize) {
        List<Bson> valuesPipeline = new ArrayList<>();
        List<Bson> countPipeline = new ArrayList<>();
        List<Bson> matchCriteria = matchCriteria(users, projects, resourceNames, dateStart, dateEnd);
        if (!matchCriteria.isEmpty()) {
            Bson match = match(Filters.and(matchCriteria));
            valuesPipeline.add(match);
            countPipeline.add(match);
        }
        countPipeline.add(count());
        valuesPipeline.addAll(Arrays.asList(skip(pageSize * (pageNumber - 1)), limit(pageSize)));

        List<Bson> facets = Collections.singletonList(facet(new Facet(AUDIT_FACET, valuesPipeline), new Facet(TOTAL_COUNT_FACET, countPipeline)));
        return StreamSupport.stream(aggregate(AUDIT_COLLECTION, facets).spliterator(), false)
                .map(this::toAuditDTO)
                .collect(Collectors.toList());
    }

    private List<Bson> matchCriteria(List<String> users, List<String> projects, List<String> resourceNames, String dateStart, String dateEnd) {
        List<Bson> searchCriteria = new ArrayList<>();
        inCriteria(searchCriteria, users, USER);
        inCriteria(searchCriteria, projects, PROJECT);
        inCriteria(searchCriteria, resourceNames, RESOURCE_NAME_FIELD);
        if (StringUtils.isNotEmpty(dateStart)) {
            Instant from = getInstant(dateStart);
            searchCriteria.add(gte(TIMESTAMP_FIELD, from));
        }
        if (StringUtils.isNotEmpty(dateEnd)) {
            Instant to = getInstant(dateEnd).plus(1, ChronoUnit.DAYS);
            searchCriteria.add(lte(TIMESTAMP_FIELD, to));
        }
        return searchCriteria;
    }

    private AuditPaginationDTO toAuditDTO(Document document) {
        List<Document> documents = (List<Document>) (document.get(TOTAL_COUNT_FACET));
        final int count = documents.isEmpty() ? 0 : documents.get(0).getInteger(COUNT_FIELD);
        List<AuditDTO> auditDTOs = (List<AuditDTO>) document.get(AUDIT_FACET);
        return AuditPaginationDTO.builder()
                .totalPageCount(count)
                .audit(auditDTOs)
                .build();
    }

    private Instant getInstant(String dateStart) {
        Instant from;
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
            from = simpleDateFormat.parse(dateStart).toInstant();
        } catch (ParseException e) {
            throw new DlabException(String.format("Cannot parse %s", dateStart), e);
        }
        return from;
    }

    private void inCriteria(List<Bson> searchCriteria, List<String> users, String user) {
        if (CollectionUtils.isNotEmpty(users)) {
            searchCriteria.add(in(user, users));
        }
    }
}
