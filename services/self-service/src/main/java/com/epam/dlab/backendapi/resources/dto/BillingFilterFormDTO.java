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

/** Stores info about billing report filter.
 */
public class BillingFilterFormDTO {
    @JsonProperty
    private String user;

    @JsonProperty
    private String product;

    @JsonProperty("resource_type")
    private String resourceType;
    
    @JsonProperty
    private String shape;

    @JsonProperty("date_start")
    private String dateStart;

    @JsonProperty("date_end")
    private String dateEnd;


    /** Return the name of user. */
    public String getUser() {
    	return user;
    }

    /** Set the name of user. */
    public void setUser(String user) {
    	this.user = user;
    }

    /** Return name of product. */
	public String getProduct() {
		return product;
	}

    /** Set name of product. */
	public void setProduct(String product) {
		this.product = product;
	}

	/** Return type of resource. */
	public String getResourceType() {
		return resourceType;
	}

	/** Set type of resource. */
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

    /** Return name of shape. */
    public String getShape() {
        return shape;
    }

    /** Set name of shape. */
    public void setShape(String shape) {
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
