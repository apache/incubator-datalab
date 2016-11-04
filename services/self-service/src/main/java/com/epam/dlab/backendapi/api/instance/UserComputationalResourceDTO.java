/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.api.instance;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserComputationalResourceDTO {
    @JsonProperty("computational_name")
    private String computationalName;
    @JsonProperty
    private String status;
    @JsonProperty("up_time_since")
    private String upTimeSince;
    @JsonProperty("master_node_shape")
    private String masterShape;
    @JsonProperty("slave_node_shape")
    private String slaveShape;
    @JsonProperty("slave_instance_number")
    private String slaveNumber;

    public String getComputationalName() {
        return computationalName;
    }

    public void setComputationalName(String computationalName) {
        this.computationalName = computationalName;
    }

    public UserComputationalResourceDTO withComputationalName(String computationalName) {
        setComputationalName(computationalName);
        return this;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UserComputationalResourceDTO withStatus(String status) {
        setStatus(status);
        return this;
    }

    public String getUpTimeSince() {
        return upTimeSince;
    }

    public void setUpTimeSince(String upTimeSince) {
        this.upTimeSince = upTimeSince;
    }

    public UserComputationalResourceDTO withUpTimeSince(String upTimeSince) {
        setUpTimeSince(upTimeSince);
        return this;
    }

    public String getMasterShape() {
        return masterShape;
    }

    public void setMasterShape(String masterShape) {
        this.masterShape = masterShape;
    }

    public UserComputationalResourceDTO withMasterShape(String masterShape) {
        setMasterShape(masterShape);
        return this;
    }

    public String getSlaveShape() {
        return slaveShape;
    }

    public void setSlaveShape(String slaveShape) {
        this.slaveShape = slaveShape;
    }

    public UserComputationalResourceDTO withSlaveShape(String slaveShape) {
        setSlaveShape(slaveShape);
        return this;
    }

    public String getSlaveNumber() {
        return slaveNumber;
    }

    public void setSlaveNumber(String slaveNumber) {
        this.slaveNumber = slaveNumber;
    }

    public UserComputationalResourceDTO withSlaveNumber(String slaveNumber) {
        setSlaveNumber(slaveNumber);
        return this;
    }
}
