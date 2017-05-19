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

package com.epam.dlab.backendapi.util;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplication;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.dto.ResourceBaseDTO;
import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.epam.dlab.exceptions.DlabException;

/** Utilities for resource methods.
 */
public class ResourceUtils {
	
	private static SettingsDAO settingsDAO;
	
	private static SettingsDAO getSettingsDAO() {
		if (settingsDAO == null) {
			settingsDAO = new SettingsDAO();
			SelfServiceApplication.getInjector().injectMembers(settingsDAO);
		}
		return settingsDAO;
	}

    @SuppressWarnings("unchecked")
	public static <T extends ResourceBaseDTO<?>> T newResourceBaseDTO(UserInfo userInfo, Class<T> resourceClass) throws DlabException {
		try {
			T resource = resourceClass.newInstance();
	    	return (T) resource
	    			.withAwsRegion(getSettingsDAO().getAwsRegion())
	    			.withAwsIamUser(userInfo.getName())
	    			.withEdgeUserName(userInfo.getSimpleName());
		} catch (Exception e) {
			throw new DlabException("Cannot create instance of resource class " + resourceClass.getName() + ". " +
					e.getLocalizedMessage(), e);
		}
    }
    
    @SuppressWarnings("unchecked")
	public static <T extends ResourceSysBaseDTO<?>> T newResourceSysBaseDTO(UserInfo userInfo, Class<T> resourceClass) throws DlabException {
    	T resource = newResourceBaseDTO(userInfo, resourceClass);
    	return (T) resource
    			.withServiceBaseName(getSettingsDAO().getServiceBaseName())
    			.withConfTagResourceId(getSettingsDAO().getConfTagResourceId())
    			.withConfOsFamily(getSettingsDAO().getConfOsFamily())
            	.withConfOsUser(getSettingsDAO().getConfOsUser());

    }
}
