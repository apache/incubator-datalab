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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.backendapi.dao.AuditDAO;
import com.epam.datalab.backendapi.domain.*;
import com.epam.datalab.backendapi.resources.dto.AuditFilter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuditServiceImplTest {
    private static final String USER = "user";
    private static final String PROJECT = "project";
    private static final String RESOURCE_NAME = "resourceName";
    private static final String INFO = "info";

    @Mock
    private AuditDAO auditDAO;
    @InjectMocks
    private AuditServiceImpl auditService;

    @Test
    public void save() {
        AuditDTO auditDTO = getAuditDTO();

        auditService.save(auditDTO);

        verify(auditDAO).save(refEq(auditDTO));
    }

    @Test
    public void testSave() {
        AuditCreateDTO auditCreateDTO = getAuditCreateDTO();

        auditService.save(USER, auditCreateDTO);

        verify(auditDAO).save(eq(getAuditDTO(USER, auditCreateDTO)));
    }

    @Test
    public void getAudit() {
        List<String> users = new ArrayList<>();
        List<String> projects = new ArrayList<>();
        List<String> resourceNames = new ArrayList<>();
        List<String> resourceTypes = new ArrayList<>();
        String dateStart = "";
        String dateEnd = "";
        int pageNumber = 1;
        int pageSize = 10;

        auditService.getAudit(users, projects, resourceNames, resourceTypes, dateStart, dateEnd, pageNumber, pageSize);

        verify(auditDAO).getAudit(refEq(users), refEq(projects), refEq(resourceNames), refEq(resourceTypes), eq(dateStart), eq(dateEnd), eq(pageNumber), eq(pageSize));
    }

    @Test
    public void downloadAuditReport() {
        when(auditDAO.aggregateAuditReport(any(AuditFilter.class))).thenReturn(getAuditReportLines());

        String actualAuditReport = auditService.downloadAuditReport(getAuditFilter());
        assertEquals("reports should be equal", prepareAuditReport(), actualAuditReport);

        verify(auditDAO).aggregateAuditReport(getAuditFilter());
    }

    @Test
    public void getAuditReport() {
        when(auditDAO.aggregateAuditReport(any(AuditFilter.class))).thenReturn(getAuditReportLines());

        AuditReport actualAuditReport = auditService.getAuditReport(getAuditFilter());
        assertEquals("Audit Reports should be equal", getExpectedAuditReport(), actualAuditReport);

        verify(auditDAO).aggregateAuditReport(getAuditFilter());
    }

    private String prepareAuditReport() {
        StringBuilder auiditReport = new StringBuilder();
        auiditReport.append("\"Available reporting period from: ").append("17-Mar-2022 ").append("to: ").append("20-Mar-2022").append("\"\n");
        auiditReport.append(new StringJoiner(",").add("Date").add("User").add("Action").add("Project").add("Resource type").add("Resource\n"));
        auiditReport.append(new StringJoiner(",").add("2022-03-20").add("test").add("LOG_IN").add("").add("").add("\n"));
        auiditReport.append(new StringJoiner(",").add("2022-03-20").add("test").add("LOG_IN").add("").add("").add("\n"));
        auiditReport.append(new StringJoiner(",").add("2022-03-19").add("test").add("LOG_IN").add("").add("").add("\n"));
        auiditReport.append(new StringJoiner(",").add("2022-03-17").add("test").add("LOG_IN").add("").add("").add("\n"));

        return auiditReport.toString();
    }

    private AuditReport getExpectedAuditReport() {
        final LocalDate usageDateFrom = LocalDate.parse("2022-03-17");
        final LocalDate usageDateTo = LocalDate.parse("2022-03-20");
        return AuditReport.builder()
                .name("Audit Report")
                .usageDateFrom(usageDateFrom)
                .usageDateTo(usageDateTo)
                .reportLines(getAuditReportLines())
                .build();
    }

    private List<AuditReportLine> getAuditReportLines() {
        AuditReportLine line1 = AuditReportLine.builder().datalabId("123").user("test").action("LOG_IN").project("").resourceName("").resourceType("").timestamp(LocalDate.parse("2022-03-20")).build();
        AuditReportLine line2 = AuditReportLine.builder().datalabId("124").user("test").action("LOG_IN").project("").resourceName("").resourceType("").timestamp(LocalDate.parse("2022-03-20")).build();
        AuditReportLine line3 = AuditReportLine.builder().datalabId("126").user("test").action("LOG_IN").project("").resourceName("").resourceType("").timestamp(LocalDate.parse("2022-03-19")).build();
        AuditReportLine line4 = AuditReportLine.builder().datalabId("126").user("test").action("LOG_IN").project("").resourceName("").resourceType("").timestamp(LocalDate.parse("2022-03-17")).build();

        return Arrays.asList(line1, line2, line3, line4);
    }

    private AuditFilter getAuditFilter() {
        AuditFilter auditFilter = new AuditFilter();
        auditFilter.setDateStart("2022-03-17");
        auditFilter.setDateEnd("2022-03-20");
        auditFilter.setLocale("en-GB");

        return auditFilter;
    }

    private AuditDTO getAuditDTO() {
        return AuditDTO.builder()
                .user(USER)
                .project(PROJECT)
                .build();
    }

    private AuditDTO getAuditDTO(String user, AuditCreateDTO auditCreateDTO) {
        return AuditDTO.builder()
                .user(user)
                .resourceName(auditCreateDTO.getResourceName())
                .info(auditCreateDTO.getInfo())
                .type(auditCreateDTO.getType())
                .action(AuditActionEnum.FOLLOW_LINK)
                .build();
    }

    private AuditCreateDTO getAuditCreateDTO() {
        return new AuditCreateDTO(RESOURCE_NAME, INFO, AuditResourceTypeEnum.COMPUTE);
    }
}