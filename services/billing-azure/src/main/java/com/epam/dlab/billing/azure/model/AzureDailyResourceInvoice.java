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

package com.epam.dlab.billing.azure.model;

import com.epam.dlab.billing.DlabResourceType;
import com.epam.dlab.billing.azure.MongoDocument;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureDailyResourceInvoice extends MongoDocument<AzureDailyResourceInvoice> {
	@JsonProperty
	private String dlabId;
	@JsonProperty
	private String user;
	@JsonProperty
	private String exploratoryId;
	@JsonProperty
	private String computationalId;
	@JsonProperty
	private DlabResourceType resourceType;
	@JsonProperty
	private String resourceName;
	@JsonProperty
	private String meterCategory;
	@JsonProperty
	private String usageStartDate;
	@JsonProperty
	private String usageEndDate;
	@JsonProperty
	private String day;
	@JsonProperty
	private double cost;
	@JsonProperty
	private String currencyCode;

	@Builder
	public AzureDailyResourceInvoice(AzureDlabBillableResource azureDlabBillableResource) {
		this.dlabId = azureDlabBillableResource.getId();
		this.user = azureDlabBillableResource.getUser();
		this.resourceType = azureDlabBillableResource.getType();
		this.resourceName = azureDlabBillableResource.getResourceName();

		if (resourceType == DlabResourceType.EXPLORATORY) {
			this.exploratoryId = azureDlabBillableResource.getId();
		} else if (resourceType == DlabResourceType.COMPUTATIONAL) {
			this.computationalId = azureDlabBillableResource.getId();
			this.exploratoryId = azureDlabBillableResource.getNotebookId();
		} else if (resourceType == DlabResourceType.VOLUME) {
			this.exploratoryId = azureDlabBillableResource.getNotebookId();
		}
	}
}
