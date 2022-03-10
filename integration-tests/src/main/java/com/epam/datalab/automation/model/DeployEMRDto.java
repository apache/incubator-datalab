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

package com.epam.datalab.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class DeployEMRDto extends DeployClusterDto{

	@JsonProperty("emr_instance_count")
	private String emrInstanceCount;

	@JsonProperty("emr_master_instance_type")
	private String emrMasterInstanceType;

	@JsonProperty("emr_slave_instance_type")
	private String emrSlaveInstanceType;

	@JsonProperty("emr_slave_instance_spot")
	private boolean emrSlaveInstanceSpot = false;

	@JsonProperty("emr_slave_instance_spot_pct_price")
	private Integer emrSlaveInstanceSpotPctPrice = 0;

	@JsonProperty("emr_version")
	private String emrVersion;


	public String getEmrInstanceCount() {
		return emrInstanceCount;
	}

	public void setEmrInstanceCount(String emrInstanceCount) {
		this.emrInstanceCount = emrInstanceCount;
	}

	public String getEmrMasterInstanceType() {
		return emrMasterInstanceType;
	}

	public void setEmrMasterInstanceType(String emrMasterInstanceType) {
		this.emrMasterInstanceType = emrMasterInstanceType;
	}

	public String getEmrSlaveInstanceType() {
		return emrSlaveInstanceType;
	}

	public void setEmrSlaveInstanceType(String emrSlaveInstanceType) {
		this.emrSlaveInstanceType = emrSlaveInstanceType;
	}

	public boolean isEmrSlaveInstanceSpot() {
		return emrSlaveInstanceSpot;
	}

	public void setEmrSlaveInstanceSpot(boolean emrSlaveInstanceSpot) {
		this.emrSlaveInstanceSpot = emrSlaveInstanceSpot;
	}

	public Integer getEmrSlaveInstanceSpotPctPrice() {
		return emrSlaveInstanceSpotPctPrice;
	}

	public void setEmrSlaveInstanceSpotPctPrice(Integer emrSlaveInstanceSpotPctPrice) {
		this.emrSlaveInstanceSpotPctPrice = emrSlaveInstanceSpotPctPrice;
	}

	public String getEmrVersion() {
		return emrVersion;
	}

	public void setEmrVersion(String emrVersion) {
		this.emrVersion = emrVersion;
	}

	@Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
        		.add("image", getImage())
				.add("template_name", getTemplateName())
        		.add("name", getName())
				.add("notebook_name", getNotebookName())
				.add("emr_instance_count", emrInstanceCount)
				.add("emr_master_instance_type", emrMasterInstanceType)
				.add("emr_slave_instance_type", emrSlaveInstanceType)
				.add("emr_slave_instance_spot", emrSlaveInstanceSpot)
				.add("emr_slave_instance_spot_pct_price", emrSlaveInstanceSpotPctPrice)
				.add("emr_version", emrVersion)
        		.toString();
    }
}
