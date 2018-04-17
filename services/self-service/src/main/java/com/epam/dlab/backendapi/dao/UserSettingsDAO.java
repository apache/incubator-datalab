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

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.auth.UserInfo;
import io.dropwizard.auth.Auth;
import org.bson.Document;
import org.hibernate.validator.constraints.NotBlank;

import static com.epam.dlab.backendapi.dao.MongoCollections.USER_UI_SETTINGS;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/** DAO for the user preferences.
 */
public class UserSettingsDAO extends BaseDAO {
    private static final String VALUE = "userSettings";

    /** Returns a value or empty string from collection.
     * @param collectionName name of collection.
     * @param id value identifier.
     * @return value or empty string.
     */
	private String getValue(String collectionName, String id) {
		Document d = mongoService
				.getCollection(collectionName)
				.find(eq(ID, id))
				.first();
		return (d == null ? EMPTY :
					d.getOrDefault(VALUE, EMPTY).toString());
	}

	/** Returns the user preferences of UI dashboard.
	 * @param userInfo user info.
	 * @return JSON content.
	 */
	public String getUISettings(@Auth UserInfo userInfo) {
        return getValue(USER_UI_SETTINGS,
        				userInfo.getName());
    }
    
	/** Store the user preferences of UI dashboard.
	 * @param userInfo user info.
	 * @param settings user preferences in JSON format.
	 */
    public void setUISettings(@Auth UserInfo userInfo, @NotBlank String settings) {
    	updateOne(USER_UI_SETTINGS,
    			eq(ID, userInfo.getName()),
    			set(VALUE, settings),
    			true);
    }

}