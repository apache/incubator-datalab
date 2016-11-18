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

package com.epam.dlab.backendapi.health;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HealthStatusDTO {
    @JsonProperty("mongo_alive")
    private boolean mongoAlive;
    @JsonProperty("provisioning_alive")
    private boolean provisioningAlive;

    public boolean isMongoAlive() {
        return mongoAlive;
    }

    public void setMongoAlive(boolean mongoAlive) {
        this.mongoAlive = mongoAlive;
    }

    public HealthStatusDTO withMongoAlive(boolean mongoAlive) {
        setMongoAlive(mongoAlive);
        return this;
    }

    public boolean isProvisioningAlive() {
        return provisioningAlive;
    }

    public void setProvisioningAlive(boolean provisioningAlive) {
        this.provisioningAlive = provisioningAlive;
    }

    public HealthStatusDTO withProvisioningAlive(boolean provisioningAlive) {
        setProvisioningAlive(provisioningAlive);
        return this;
    }
}
