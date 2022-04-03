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

import com.epam.datalab.backendapi.domain.AuditDTO;
import com.epam.datalab.backendapi.domain.AuditPaginationDTO;
import com.epam.datalab.backendapi.domain.AuditReportLine;
import com.epam.datalab.backendapi.resources.dto.AuditFilter;
import com.epam.datalab.exceptions.DatalabException;
import com.mongodb.client.model.Facet;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.epam.datalab.backendapi.dao.ComputationalDAO.PROJECT;
import static com.mongodb.client.model.Aggregates.count;
import static com.mongodb.client.model.Aggregates.facet;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.skip;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;

public class AuditDAOImpl extends BaseDAO implements AuditDAO {
    private static final String AUDIT_COLLECTION = "audit";
    private static final String RESOURCE_NAME_FIELD = "resourceName";
    private static final String RESOURCE_TYPE_FIELD = "type";
    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String COUNT_FIELD = "count";
    private static final String AUDIT_FACET = "auditFacet";
    private static final String TOTAL_COUNT_FACET = "totalCountFacet";
    private static final String RESOURCE_NAME_FACET = "resourceNameFacet";
    private static final String USER_FACET = "userFacet";
    private static final String PROJECT_FACET = "projectFacet";
    private static final String RESOURCE_TYPE_FACET = "typeFacet";
    private static final String DATALAB_ID = "datalabId";
    private static final String ACTION = "action";

    @Override
    public void save(AuditDTO audit) {
        insertOne(AUDIT_COLLECTION, audit);
    }

    @Override
    public List<AuditPaginationDTO> getAudit(List<String> users, List<String> projects, List<String> resourceNames, List<String> resourceTypes, String dateStart, String dateEnd,
                                             int pageNumber, int pageSize) {
        List<Bson> facets = getFacets(users, projects, resourceNames, resourceTypes, dateStart, dateEnd, pageNumber, pageSize);
        return StreamSupport.stream(aggregate(AUDIT_COLLECTION, facets).spliterator(), false)
                .map(this::toAuditPaginationDTO)
                .collect(Collectors.toList());
    }

    public List<AuditReportLine> aggregateAuditReport(AuditFilter filter) {
        List<Bson> facets = getFacets(filter.getUsers(), filter.getProjects(), filter.getResourceNames(), filter.getResourceTypes(), filter.getDateStart(), filter.getDateEnd(), filter.getPageNumber(), filter.getPageSize());
        List<Document> auditDocuments  = new ArrayList<>();
        StreamSupport.stream(aggregate(AUDIT_COLLECTION, facets).spliterator(), false)
                .map(document -> (ArrayList<Document>)document.get(AUDIT_FACET))
                .forEach(auditDocuments::addAll);
        return auditDocuments.stream()
                .map(this::toAuditReport)
                .collect(Collectors.toList());
    }

    private List<Bson> getFacets(List<String> users, List<String> projects, List<String> resourceNames, List<String> resourceTypes, String dateStart, String dateEnd,
                                 int pageNumber, int pageSize){
        List<Bson> valuesPipeline = new ArrayList<>();
        List<Bson> countPipeline = new ArrayList<>();
        List<Bson> matchCriteria = matchCriteria(users, projects, resourceNames, resourceTypes, dateStart, dateEnd);
        if (!matchCriteria.isEmpty()) {
            Bson match = match(Filters.and(matchCriteria));
            valuesPipeline.add(match);
            countPipeline.add(match);
        }
        countPipeline.add(count());
        valuesPipeline.add(sortCriteria());
        valuesPipeline.addAll(Arrays.asList(skip(pageSize * (pageNumber - 1)), limit(pageSize)));

        List<Bson> userFilter = Collections.singletonList(group(getGroupingFields(USER)));
        List<Bson> projectFilter = Collections.singletonList(group(getGroupingFields(PROJECT)));
        List<Bson> resourceNameFilter = Collections.singletonList(group(getGroupingFields(RESOURCE_NAME_FIELD)));
        List<Bson> resourceTypeFilter = Collections.singletonList(group(getGroupingFields(RESOURCE_TYPE_FIELD)));

        return Collections.singletonList(facet(new Facet(AUDIT_FACET, valuesPipeline), new Facet(TOTAL_COUNT_FACET, countPipeline),
                new Facet(RESOURCE_NAME_FACET, resourceNameFilter), new Facet(USER_FACET, userFilter), new Facet(PROJECT_FACET, projectFilter),
                new Facet(RESOURCE_TYPE_FACET, resourceTypeFilter)));
    }

    private List<Bson> matchCriteria(List<String> users, List<String> projects, List<String> resourceNames, List<String> resourceTypes, String dateStart, String dateEnd) {
        List<Bson> searchCriteria = new ArrayList<>();
        inCriteria(searchCriteria, users, USER);
        inCriteria(searchCriteria, projects, PROJECT);
        inCriteria(searchCriteria, resourceNames, RESOURCE_NAME_FIELD);
        inCriteria(searchCriteria, resourceTypes, RESOURCE_TYPE_FIELD);
        if (StringUtils.isNotEmpty(dateStart)) {
            Instant from = getInstant(dateStart);
            Date date = new Date(from.toEpochMilli());
            searchCriteria.add(gte(TIMESTAMP_FIELD, date));
        }
        if (StringUtils.isNotEmpty(dateEnd)) {
            Instant to = getInstant(dateEnd).plus(1, ChronoUnit.DAYS);
            Date date = new Date(to.toEpochMilli());
            searchCriteria.add(lte(TIMESTAMP_FIELD, date));
        }
        return searchCriteria;
    }

    private Bson sortCriteria() {
        return sort(Sorts.descending(TIMESTAMP_FIELD));
    }

    private AuditPaginationDTO toAuditPaginationDTO(Document document) {
        List<Document> countDocuments = (List<Document>) document.get(TOTAL_COUNT_FACET);
        final int count = countDocuments.isEmpty() ? 0 : countDocuments.get(0).getInteger(COUNT_FIELD);
        Set<String> userFilter = getFilter(document, USER_FACET, USER);
        Set<String> projectFilter = getFilter(document, PROJECT_FACET, PROJECT);
        Set<String> resourceNameFilter = getFilter(document, RESOURCE_NAME_FACET, RESOURCE_NAME_FIELD);
        Set<String> resourceTypeFilter = getFilter(document, RESOURCE_TYPE_FACET, RESOURCE_TYPE_FIELD);
        List<AuditDTO> auditDTOs = (List<AuditDTO>) document.get(AUDIT_FACET);
        return AuditPaginationDTO.builder()
                .totalPageCount(count)
                .audit(auditDTOs)
                .userFilter(userFilter)
                .resourceNameFilter(resourceNameFilter)
                .resourceTypeFilter(resourceTypeFilter)
                .projectFilter(projectFilter)
                .build();
    }

    private AuditReportLine toAuditReport(Document doc) {
        return AuditReportLine.builder()
                .datalabId(doc.getString(DATALAB_ID))
                .project(doc.getString(PROJECT))
                .resourceName(doc.getString(RESOURCE_NAME_FIELD))
                .action(doc.getString(ACTION))
                .user(doc.getString(USER))
                .resourceType(doc.getString(RESOURCE_TYPE_FIELD))
                .timestamp(doc.getDate(TIMESTAMP_FIELD).toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .build();
    }

    private Set<String> getFilter(Document document, String facet, String field) {
        return ((List<Document>) document.get(facet))
                .stream()
                .map(d -> (Document) d.get(ID))
                .map(d -> d.getString(field))
                .collect(Collectors.toSet());
    }

    private Instant getInstant(String dateStart) {
        Instant from;
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
            from = simpleDateFormat.parse(dateStart).toInstant();
        } catch (ParseException e) {
            throw new DatalabException(String.format("Cannot parse %s", dateStart), e);
        }
        return from;
    }

    private void inCriteria(List<Bson> searchCriteria, List<String> users, String user) {
        if (CollectionUtils.isNotEmpty(users)) {
            searchCriteria.add(in(user, users));
        }
    }
}
