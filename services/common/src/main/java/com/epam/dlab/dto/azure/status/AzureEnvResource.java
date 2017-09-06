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

package com.epam.dlab.dto.azure.status;

import com.epam.dlab.dto.azure.AzureResource;
import com.epam.dlab.dto.status.EnvResourceList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class AzureEnvResource extends AzureResource<AzureEnvResource> {
    @JsonProperty("edge_list_resources")
    private EnvResourceList resourceList;

    public EnvResourceList getResourceList() {
        return resourceList;
    }

    public void setResourceList(EnvResourceList resourceList) {
        this.resourceList = resourceList;
    }

    public AzureEnvResource withResourceList(EnvResourceList resourceList) {
        setResourceList(resourceList);
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("resourceList", resourceList);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
