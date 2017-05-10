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

package com.epam.dlab.core.parser;

/** Billing resource types.
 */
public enum ResourceType {
	COMPUTER,
	CLUSTER,
	STORAGE,
	STORAGE_BUCKET,
	IP_ADDRESS,
	OTHER;
	
    public static ResourceType of(String string) {
        if (string != null) {
            for (ResourceType value : ResourceType.values()) {
                if (string.equalsIgnoreCase(value.toString())) {
                    return value;
                }
            }
        }
        return null;
    }
    
    /** Return the category of resource.
     * @param resourceType the type of resource.
     */
    public static String category(ResourceType resourceType) {
    	switch (resourceType) {
		case COMPUTER:
			return "Compute";
		case CLUSTER:
			return "EC2";
		case STORAGE:
			return "S3";
		case STORAGE_BUCKET:
			return "Storage";
		case IP_ADDRESS:
			return "Static";
		default:
			return "Other";
		}
	}
    
    /** Return the category of resource. */
    public String category() {
    	return category(this);
    }

    @Override
    public String toString() {
    	return super.toString().toUpperCase();
    }
}
