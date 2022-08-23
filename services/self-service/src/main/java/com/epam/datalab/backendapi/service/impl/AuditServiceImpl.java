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
import com.epam.datalab.backendapi.service.AuditService;
import com.epam.datalab.backendapi.util.AuditUtils;
import com.google.inject.Inject;

import java.time.LocalDate;
import java.util.List;

import static com.epam.datalab.backendapi.domain.AuditActionEnum.FOLLOW_LINK;

public class AuditServiceImpl implements AuditService {
    private final AuditDAO auditDAO;

    @Inject
    public AuditServiceImpl(AuditDAO auditDAO) {
        this.auditDAO = auditDAO;
    }

    @Override
    public void save(AuditDTO audit) {
        auditDAO.save(audit);
    }

    @Override
    public void save(String user, AuditCreateDTO audit) {
        AuditDTO auditDTO = AuditDTO.builder()
                .user(user)
                .resourceName(audit.getResourceName())
                .action(FOLLOW_LINK)
                .type(audit.getType())
                .info(audit.getInfo())
                .build();
        auditDAO.save(auditDTO);
    }

    @Override
    public List<AuditPaginationDTO> getAudit(List<String> users, List<String> projects, List<String> resourceNames, List<String> resourceTypes,
                                             String dateStart, String dateEnd, int pageNumber, int pageSize) {
        return auditDAO.getAudit(users, projects, resourceNames, resourceTypes, dateStart, dateEnd, pageNumber, pageSize);
    }

    @Override
    public AuditReport getAuditReport(AuditFilter filter) {
        List<AuditReportLine> auditReportLines = auditDAO.aggregateAuditReport(filter);
        final LocalDate dateFrom = LocalDate.parse(filter.getDateStart());
        final LocalDate dateTo = LocalDate.parse(filter.getDateEnd());
        return AuditReport.builder()
                .name("Audit Report")
                .reportLines(auditReportLines)
                .usageDateFrom(dateFrom)
                .usageDateTo(dateTo)
                .build();
    }

    public String downloadAuditReport(AuditFilter filter) {
        List<AuditReportLine> auditReportLines = auditDAO.aggregateAuditReport(filter);
        final LocalDate dateFrom = LocalDate.parse(filter.getDateStart());
        final LocalDate dateTo = LocalDate.parse(filter.getDateEnd());
        StringBuilder reportHead = new StringBuilder(AuditUtils.getFirstLine(dateFrom, dateTo,  filter.getLocale()));
        reportHead.append(AuditUtils.getHeader());
        auditReportLines.forEach(r -> reportHead.append(AuditUtils.printLine(r)));

        return reportHead.toString();
    }
}
