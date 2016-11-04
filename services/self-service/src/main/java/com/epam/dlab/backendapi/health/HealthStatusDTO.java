/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

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
