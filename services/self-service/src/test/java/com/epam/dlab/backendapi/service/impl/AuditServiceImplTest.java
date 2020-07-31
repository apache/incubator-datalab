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
import com.epam.dlab.backendapi.domain.AuditResourceTypeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;

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