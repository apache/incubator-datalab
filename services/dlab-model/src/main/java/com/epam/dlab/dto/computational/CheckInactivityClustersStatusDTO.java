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
import com.google.common.base.MoreObjects;
import lombok.Getter;

import java.util.Date;

@Getter
public class CheckInactivityClustersStatusDTO extends StatusBaseDTO<CheckInactivityClustersStatusDTO> {

	private CheckInactivityClusterCallbackDTO checkInactivityClusterCallbackDTO;
	private CheckInactivityClustersStatus checkInactivityClustersStatus;
	private Date lastActivityDate;

	public CheckInactivityClustersStatusDTO withCheckInactivityClusterCallbackDTO(CheckInactivityClusterCallbackDTO
																						  checkInactivityClusterCallbackDTO) {
		this.checkInactivityClusterCallbackDTO = checkInactivityClusterCallbackDTO;
		return this;
	}

	public CheckInactivityClustersStatusDTO withCheckInactivityClustersStatus(CheckInactivityClustersStatus
																					  checkInactivityClustersStatus) {
		this.checkInactivityClustersStatus = checkInactivityClustersStatus;
		return this;
	}

	public CheckInactivityClustersStatusDTO withLastActivityDate(Date lastActivityDate) {
		this.lastActivityDate = lastActivityDate;
		return this;
	}

	@Override
	public MoreObjects.ToStringHelper toStringHelper(Object self) {
		return super.toStringHelper(self)
				.add("checkInactivityClusterCallbackDTO", checkInactivityClusterCallbackDTO)
				.add("checkInactivityClustersStatus", checkInactivityClustersStatus)
				.add("lastActivityDate", lastActivityDate);
	}
}
