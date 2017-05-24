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


package com.epam.dlab.backendapi.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epam.dlab.exceptions.DlabException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;

import io.dropwizard.util.Duration;

/** Class to store the info about libraries.
 */
public class ExploratoryLibList {
	
	/**	Timeout in milliseconds when the info is out of date. */
	private static final long EXPIRED_TIMEOUT_MILLIS = Duration.hours(2).toMilliseconds();

	/**	Timeout in milliseconds until the is out of date. */
	private static final long UPDATE_TIMEOUT_MILLIS = Duration.minutes(5).toMilliseconds();
	
	/**	Timeout in milliseconds for request to update lib. */
	private static final long UPDATE_REQUEST_TIMEOUT_MILLIS = Duration.minutes(10).toMilliseconds();
	
	/** Image name. */
	private String imageName;

	/**	List of libraries group:libraries. */
	private Map<String, List<String>> libs = new HashMap<>();
	
	/**	Time in milliseconds when the info is out of date. */
	private long expiredTimeMillis = 0;
	
	/**	Last access time in milliseconds to the info. */
	private long accessTimeMillis = 0;
	
	/**	Update start time in milliseconds. */
	private long updateStartTimeMillis = 0;
	
	/** Update in progress. */
	private boolean updating = false;
	
	
	/** Instantiate the list of libraries.
	 * @param imageName the name of docker's image.
	 * @param content JSON string.
	 */
	public ExploratoryLibList(String imageName, String content) {
		this.imageName = imageName;
		if (content != null) {
			setLibs(content);
		}
	}
	
	/** Return the list of all groups. */
	public List<String> getGroupList() {
		List<String> list = new ArrayList<>();
		for(String key : libs.keySet()) {
			list.add(key);
		}
		Collections.sort(list);
		return list;
	}
	
	/** Return the name of docker image;
	 */
	public String getImageName() {
		return imageName;
	}
	
	/** Return the full list of libraries for group.
	 * @param group the name of group.
	 */
	public List<String> getLibs(String group) {
		return libs.get(group);
	}
	
	/** Return the full list of libraries for group.
	 * @param content JSON string.
	 * @exception DlabException
	 */
	private void setLibs(String content) throws DlabException {
		ObjectMapper mapper = new ObjectMapper();
		try {
			synchronized (this) {
				libs.clear();
				@SuppressWarnings("unchecked")
				Map<String, Map<String, String>> map = mapper.readValue(content, Map.class);
				for (String groupName : map.keySet()) {
					Map<String, String> group = map.get(groupName);
					List<String> libList = new ArrayList<>();
					for (String libName : group.keySet()) {
						libList.add(libName);
					}
					Collections.sort(libList);
					libs.put(groupName, libList);
				}
				expiredTimeMillis = System.currentTimeMillis() + EXPIRED_TIMEOUT_MILLIS;
				accessTimeMillis = System.currentTimeMillis();
				updating = false;
			}
		} catch (IOException e) {
			throw new DlabException("Cannot deserialize the list of libraries. " + e.getLocalizedMessage(), e);
		}
	}
	
	/** Search and return the list of libraries for name's prefix <b>startWith</b>.
	 * @param group the name of group.
	 * @param startWith the prefix for library name.
	 */
	public List<String> getLibs(String group, String startWith) {
		List<String> libList = getLibs(group);
		List<String> list = new ArrayList<>();
		if (libList == null) {
			return list;
		}
		
		int fromIndex = Collections.binarySearch(libList, startWith);
		if (fromIndex < 0) {
			fromIndex = -fromIndex;
			if (fromIndex > libList.size()) {
				return list;
			}
			fromIndex--;
		}
		
		int toIndex = (libList.get(fromIndex).startsWith(startWith) ? libList.size() : fromIndex);
		for (int i = fromIndex; i < libList.size(); i++) {
			if (!libList.get(i).startsWith(startWith)) {
				toIndex = i;
				break;
			}
		}
		
		list = libList.subList(fromIndex, toIndex);
		return list;
	}
	
	/** Set last access time.
	 */
	private void touch() {
		accessTimeMillis = System.currentTimeMillis();
	}
	
	/** Return <b>true</b> if the info is out of date.
	 */
	public boolean isExpired() {
		touch();
		return (expiredTimeMillis > System.currentTimeMillis());
	}
	
	/** Return <b>true</b> if the info needs to update.
	 */
	public boolean isUpdateNeeded() {
		touch();
		return (accessTimeMillis > expiredTimeMillis - UPDATE_TIMEOUT_MILLIS);
	}
	
	/** Set updating in progress.
	 */
	public void setUpdating() {
		updateStartTimeMillis = System.currentTimeMillis();
		updating = true;
	}
	
	/** Return <b>true</b> if the update in progress.
	 */
	public boolean isUpdating() {
		if (updating &&
			updateStartTimeMillis + UPDATE_REQUEST_TIMEOUT_MILLIS < System.currentTimeMillis()) {
			updating = false;
		}
		return updating;
	}
	
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("imageName", imageName)
				.add("expiredTimeMillis", expiredTimeMillis)
				.add("accessTimeMillis", accessTimeMillis)
				.add("updateStartTimeMillis", updateStartTimeMillis)
				.add("isUpdating", updating)
				.add("libs", (libs == null ? "null" : "..."))
				.toString();
	}
}
