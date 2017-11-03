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

package com.epam.dlab.mongo;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import com.epam.dlab.billing.DlabResourceType;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.MoreObjects;

/** List of the DLab's resources.
 */
public class ResourceItemList {
	/** List of the resources. */
	private final Vector<ResourceItem> list;
	
	
	/** Constructs an empty list of resources.
     */
	public ResourceItemList() {
		list = new Vector<>();
	}
	
	
	/** Appends the resource to the list and returns it.
	 * @param resourceId the resource id.
	 * @param resourceName the user friendly name of resource.
	 * @param type the type of resource.
	 * @param user the name of user.
	 * @param exploratoryName the name of exploratory.
	 * @return Instance of the resource.
	 */
	public ResourceItem append(String resourceId, String resourceName, DlabResourceType type, String user, String exploratoryName) {
		ResourceItem item = new ResourceItem(resourceId, resourceName, type, user, exploratoryName);
	    synchronized (this) {
			int index = Collections.binarySearch(list, item);
			if (index < 0) {
				index = -index;
				if (index > list.size()) {
					list.add(item);
				} else {
					list.add(index - 1, item);
				}
			} else {
				item = list.get(index);
			}
	    }
		return item;
	}

	/** Returns the number of the range in list. */
	public int size() {
		return list.size();
	}

	/** Returns the resource.
	 * @param index index of the resource.
	 */
	public ResourceItem get(int index) {
		return list.get(index);
	}

	/** Comparator for search resource item by resource id. */
	private final ResourceItem findItemById = new ResourceItem(null, null, null, null, null);
	private final ComparatorByName compareByName = new ComparatorByName();
	private class ComparatorByName implements Comparator<ResourceItem> {
		
		@Override
		public int compare(ResourceItem o1, ResourceItem o2) {
			return StringUtils.compare(o1.resourceId, o2.resourceId);
		}
		
	}
	
	/** Find and return the resource by resource id.
	 * @param index index of the resource.
	 */
	public ResourceItem getById(String resourceId) {
		findItemById.resourceId = resourceId;
		int index = Collections.binarySearch(list, findItemById, compareByName);
		
		return (index < 0 ? null : list.get(index));
	}

	/** Removes all of the elements from list.
	 */
	public void clear() {
		list.clear();
	}
	
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("items", list).toString();
	}
}
