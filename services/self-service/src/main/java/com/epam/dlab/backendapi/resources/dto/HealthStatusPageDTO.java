/***************************************************************************

 Copyright (c) 2016, EPAM SYSTEMS INC

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 ****************************************************************************/

package com.epam.dlab.backendapi.resources.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * Stores the health statuses for environment resources.
 */
public class HealthStatusPageDTO {
	@JsonProperty
	private String status;
	@JsonProperty("list_resources")
	private List<HealthStatusResource> listResources;
	@JsonProperty
	private boolean billingEnabled;
	@JsonProperty
	private boolean backupAllowed;
	@JsonProperty
	private boolean admin;

	/**
	 * Return the status of environment.
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Set the status of environment.
	 */
	public void setStatus(HealthStatusEnum status) {
		this.status = status == null ? null : status.toString();
	}

	/**
	 * Set the status of environment.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	public void setBillingEnabled(boolean billingEnabled) {
		this.billingEnabled = billingEnabled;
	}

	public void setBackupAllowed(boolean backupAllowed) {
		this.backupAllowed = backupAllowed;
	}

	/**
	 * Set the status of environment.
	 */
	public HealthStatusPageDTO withStatus(String status) {
		setStatus(status);
		return this;
	}

	/**
	 * Set the status of environment.
	 */
	public HealthStatusPageDTO withStatus(HealthStatusEnum status) {
		setStatus(status);
		return this;
	}

	public HealthStatusPageDTO withBackupAllowed(boolean backupAllowed) {
		setBackupAllowed(backupAllowed);
		return this;
	}

	/**
	 * Return the list of resources.
	 */
	public List<HealthStatusResource> getListResources() {
		return listResources;
	}

	/**
	 * Set the list of resources.
	 */
	public void setListResources(List<HealthStatusResource> listResources) {
		this.listResources = listResources;
	}

	/**
	 * Set the list of resources.
	 */
	public HealthStatusPageDTO withListResources(List<HealthStatusResource> listResources) {
		setListResources(listResources);
		return this;
	}

	/**
	 * Set billing enabled flag
	 */
	public HealthStatusPageDTO withBillingEnabled(boolean billingEnabled) {
		setBillingEnabled(billingEnabled);
		return this;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("status", status)
				.add("listResources", listResources)
				.add("billingEnabled", billingEnabled)
				.add("backupAllowed", backupAllowed)
				.add("admin", admin)
				.toString();
	}

	public HealthStatusPageDTO withAdmin(boolean isAdmin) {
		this.admin = isAdmin;
		return this;
	}

	public boolean isBillingEnabled() {
		return billingEnabled;
	}

	public boolean isBackupAllowed() {
		return backupAllowed;
	}

	public boolean isAdmin() {
		return admin;
	}
}