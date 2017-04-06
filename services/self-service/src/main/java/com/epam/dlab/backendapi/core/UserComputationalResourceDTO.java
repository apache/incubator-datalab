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

package com.epam.dlab.backendapi.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Date;

/** Stores info about the user's computational resources for notebook.
 */
public class UserComputationalResourceDTO {
    @JsonProperty("computational_name")
    private String computationalName;
    @JsonProperty("computational_id")
    private String computationalId;
    @JsonProperty("instance_id")
    private String instanceId;
    @JsonProperty
    private String status;
    @JsonProperty("up_time")
    private Date uptime;
    @JsonProperty("master_node_shape")
    private String masterShape;
    @JsonProperty("slave_node_shape")
    private String slaveShape;
    @JsonProperty("slave_node_spot")
    private Boolean slaveSpot = false;
    @JsonProperty("slave_node_spot_pct_price")
    private Integer slaveSpotPctPrice;
    @JsonProperty("total_instance_number")
    private String slaveNumber;
    @JsonProperty("emr_version")
    private String version;

    /** Returns name of computational resource. */
    public String getComputationalName() {
        return computationalName;
    }

    /** Sets name of computational resource. */
    public void setComputationalName(String computationalName) {
        this.computationalName = computationalName;
    }

    /** Sets name of computational resource. */
    public UserComputationalResourceDTO withComputationalName(String computationalName) {
        setComputationalName(computationalName);
        return this;
    }

    /** Returns a unique id of computational resource. */
    public String getComputationalId() {
        return computationalId;
    }

    /** Sets a unique id of computational resource. */
    public void setComputationalId(String computationalId) {
        this.computationalId = computationalId;
    }

    /** Sets a unique id of computational resource. */
    public UserComputationalResourceDTO withComputationalId(String computationalId) {
        setComputationalId(computationalId);
        return this;
    }

    /** Returns the id of instance in Amazon. */
    public String getInstanceId() {
        return instanceId;
    }

    /** Sets the id of instance in Amazon. */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /** Sets the id of instance in Amazon. */
    public UserComputationalResourceDTO withInstanceId(String instanceId) {
    	setInstanceId(instanceId);
        return this;
    }

    /** Returns the status. */
    public String getStatus() {
        return status;
    }

    /** Sets the status. */
    public void setStatus(String status) {
        this.status = status;
    }

    /** Sets the status. */
    public UserComputationalResourceDTO withStatus(String status) {
        setStatus(status);
        return this;
    }

    /** Returns the date and time when the resource has created. */
    public Date getUptime() {
        return uptime;
    }

    /** Sets the date and time when the resource has created. */
    public void setUptime(Date uptime) {
        this.uptime = uptime;
    }

    /** Sets the date and time when the resource has created. */
    public UserComputationalResourceDTO withUptime(Date uptime) {
        setUptime(uptime);
        return this;
    }

    /** Returns the name of notebook shape where resource has been deployed. */
    public String getMasterShape() {
        return masterShape;
    }

    /** Sets the name of notebook shape where resource has been deployed. */
    public void setMasterShape(String masterShape) {
        this.masterShape = masterShape;
    }

    /** Sets the name of notebook shape where resource has been deployed. */
    public UserComputationalResourceDTO withMasterShape(String masterShape) {
        setMasterShape(masterShape);
        return this;
    }

    /** Returns the name of slave shape. */ 
    public String getSlaveShape() {
        return slaveShape;
    }


    /** Sets the name of slave shape. */
    public void setSlaveShape(String slaveShape) {
        this.slaveShape = slaveShape;
    }

    public Boolean isSlaveSpot() {
        return slaveSpot;
    }

    public void setSlaveSpot(Boolean slaveSpot) {
        this.slaveSpot = slaveSpot;

    }
    /** Sets the name of slave shape. */
    public UserComputationalResourceDTO withSlaveShape(String slaveShape) {
        setSlaveShape(slaveShape);
        return this;
    }

    /** Sets the name of slave shape. */
    public UserComputationalResourceDTO withSlaveSpot(Boolean slaveSpot) {
        setSlaveSpot(slaveSpot);
        return this;
    }

    /** Returns the number of slaves notebooks. */
    public String getSlaveNumber() {
        return slaveNumber;
    }

    /** Sets the number of slaves notebooks. */
    public void setSlaveNumber(String slaveNumber) {
        this.slaveNumber = slaveNumber;
    }

    /** Sets the number of slaves notebooks. */
    public UserComputationalResourceDTO withSlaveNumber(String slaveNumber) {
        setSlaveNumber(slaveNumber);
        return this;
    }

    /** Returns the EMR version. */
    public String getVersion() {
        return version;
    }

    /** Sets the EMR version. */
    public void setVersion(String version) {
        this.version = version;
    }

    /** Sets the EMR version. */
    public UserComputationalResourceDTO withVersion(String version) {
        setVersion(version);
        return this;
    }

    public Integer getSlaveSpotPctPrice() {
        return slaveSpotPctPrice;
    }

    public void setSlaveSpotPctPrice(Integer slaveSpotPctPrice) {
        this.slaveSpotPctPrice = slaveSpotPctPrice;
    }

    public UserComputationalResourceDTO withSlaveSpotPctPrice(Integer slaveSpotPctPrice) {
        setSlaveSpotPctPrice(slaveSpotPctPrice);
        return this;
    }
    
    @Override
    public String toString() {
    	return MoreObjects.toStringHelper(this)
    			.add("computationalId", computationalId)
    			.add("computationalName", computationalName)
    			.add("version", version)
    	        .add("masterShape", masterShape)
    			.add("slaveShape", slaveShape)
                .add("slaveSpot", slaveSpot)
                .add("slaveSpotPctPrice", slaveSpotPctPrice)
    			.add("slaveNumber", slaveNumber)
    			.add("uptime", uptime)
    	        .add("status", status)
    	        .toString();
    }
}
