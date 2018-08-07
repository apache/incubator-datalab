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
package com.epam.dlab.dto.computational;

import com.epam.dlab.dto.StatusBaseDTO;
import com.epam.dlab.dto.status.EnvResource;
import com.google.common.base.MoreObjects;
import lombok.Getter;

import java.util.List;

@Getter
public class CheckInactivityClustersStatusDTO extends StatusBaseDTO<CheckInactivityClustersStatusDTO> {

	private String id;
	private List<EnvResource> clusters;
	private CheckInactivityClustersStatus checkInactivityClustersStatus;

	public CheckInactivityClustersStatusDTO withId(String id) {
		this.id = id;
		return this;
	}

	public CheckInactivityClustersStatusDTO withClusters(List<EnvResource> clusters) {
		this.clusters = clusters;
		return this;
	}

	public CheckInactivityClustersStatusDTO withCheckInactivityClustersStatus(CheckInactivityClustersStatus
																					  checkInactivityClustersStatus) {
		this.checkInactivityClustersStatus = checkInactivityClustersStatus;
		return this;
	}

	@Override
	public MoreObjects.ToStringHelper toStringHelper(Object self) {
		return super.toStringHelper(self)
				.add("id", id)
				.add("clusters", clusters)
				.add("checkInactivityClustersStatus", checkInactivityClustersStatus);
	}
}
