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

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.dao.AuditDAO;
import com.epam.dlab.backendapi.domain.AuditActionEnum;
import com.epam.dlab.backendapi.domain.AuditCreateDTO;
import com.epam.dlab.backendapi.domain.AuditDTO;
import com.epam.dlab.backendapi.service.AuditService;
import com.google.inject.Inject;

import java.util.List;

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
                .action(AuditActionEnum.FOLLOW_NOTEBOOK_LINK)
                .info(audit.getInfo())
                .build();

        auditDAO.save(auditDTO);
    }

    @Override
    public List<AuditDTO> getAudit(int pageNumber, int pageSize) {
        return auditDAO.getAudit(pageNumber, pageSize);
    }
}
