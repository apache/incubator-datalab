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

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.dto.azure.edge.EdgeInfoAzure;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Singleton
public class AzureKeyDao extends KeyDAO {

	public AzureKeyDao() {
		log.info("{} is initialized", getClass().getSimpleName());
	}

	@Override
	public EdgeInfoAzure getEdgeInfo(String user) {
		return super.getEdgeInfo(user, EdgeInfoAzure.class, new EdgeInfoAzure());
	}

	@Override
	public Optional<EdgeInfoAzure> getEdgeInfoWhereStatusIn(String user, UserInstanceStatus... statuses) {
		return super.getEdgeInfoWhereStatusIn(user, EdgeInfoAzure.class, statuses);
	}
}
