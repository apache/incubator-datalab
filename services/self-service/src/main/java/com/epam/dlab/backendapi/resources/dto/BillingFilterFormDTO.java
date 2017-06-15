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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/** Stores info about billing report filter.
 */
public class BillingFilterFormDTO {
    @JsonProperty
    private List<String> user;

    @JsonProperty
    private List<String> product;

    @JsonProperty("resource_type")
    private List<String> resourceType;
    
    @JsonProperty
    private List<String> shape;

    @JsonProperty("date_start")
    private String dateStart;

    @JsonProperty("date_end")
    private String dateEnd;


    /** Return the name of user. */
    public List<String> getUser() {
    	return user;
    }

    /** Set the name of user. */
    public void setUser(List<String> user) {
    	this.user = user;
    }

    /** Return name of product. */
	public List<String> getProduct() {
		return product;
	}

    /** Set name of product. */
	public void setProduct(List<String> product) {
		this.product = product;
	}

	/** Return type of resource. */
	public List<String> getResourceType() {
		return resourceType;
	}

	/** Set type of resource. */
	public void setResourceType(List<String> resourceType) {
		this.resourceType = resourceType;
	}

    /** Return name of shape. */
    public List<String> getShape() {
        return shape;
    }

    /** Set name of shape. */
    public void setShape(List<String> shape) {
        this.shape = shape;
    }

    /** Return start date. */
    public String getDateStart() {
        return dateStart;
    }

    /** Set start date. */
    public void setDateStart(String dateStart) {
        this.dateStart = dateStart;
    }

    /** Return end date. */
    public String getDateEnd() {
        return dateEnd;
    }

    /** Set end date. */
    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }

    
    @Override
    public String toString() {
    	return MoreObjects.toStringHelper(this)
    			.add("user", user)
    			.add("product", product)
    			.add("resourceType", resourceType)
    			.add("shape", shape)
    	        .add("dateStart", dateStart)
                .add("dateEnd", dateEnd)
    	        .toString();
    }
}
