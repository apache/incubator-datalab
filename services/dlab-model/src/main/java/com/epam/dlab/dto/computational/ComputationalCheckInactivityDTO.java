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

import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.MoreObjects;
import lombok.Getter;

@Getter
public class ComputationalCheckInactivityDTO extends ComputationalBase<ComputationalCheckInactivityDTO> {

	@JsonProperty("exploratory_id")
	private String exploratoryId;

	@JsonProperty("type")
	private DataEngineType type;

	@JsonSetter
	public void setType(@JsonProperty("type") String type) {
		this.type = DataEngineType.fromString(type);
	}

	public void setType(DataEngineType type) {
		this.type = type;
	}


	public ComputationalCheckInactivityDTO withExploratoryId(String exploratoryId) {
		this.exploratoryId = exploratoryId;
		return this;
	}

	public ComputationalCheckInactivityDTO withType(DataEngineType type) {
		setType(type);
		return this;
	}

	@Override
	public MoreObjects.ToStringHelper toStringHelper(Object self) {
		return super.toStringHelper(self)
				.add("exploratoryId", exploratoryId)
				.add("type", type);
	}

	@Override
	public String toString() {
		return toStringHelper(this).toString();
	}
}
