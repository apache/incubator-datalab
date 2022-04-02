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

import java.util.List;

public interface AuditDAO {
    void save(AuditDTO audit);

    List<AuditPaginationDTO> getAudit(List<String> users, List<String> projects, List<String> resourceNames, List<String> resourceTypes, String dateStart, String dateEnd, int pageNumber, int pageSize);

    List<AuditReportLine> aggregateAuditReport(AuditFilter auditFilter);
}
