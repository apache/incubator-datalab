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

	/** Instantiate, initialize and return class instance which inherits from {@link com.epam.dlab.dto.ResourceBaseDTO}.
	 * Initialize: AWS region, AWS IAM user name, EDGE user name.
	 * @param userInfo the user info.
	 * @param resourceClass the class for instantiate.
	 * @return initialized <b>resourceClass</b> instance.
	 * @throws DlabException
	 */
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
    
    /** Instantiate, initialize and return class instance which inherits from {@link com.epam.dlab.dto.ResourceSysBaseDTO}.
	 * Initialize: AWS region, AWS IAM user name, EDGE user name, service base name, conf tag resource Id, conf OS family, conf OS user.
	 * @param userInfo the user info.
	 * @param resourceClass the class for instantiate.
	 * @return initialized <b>resourceClass</b> instance.
	 * @throws DlabException
	 */
    @SuppressWarnings("unchecked")
	public static <T extends ResourceSysBaseDTO<?>> T newResourceSysBaseDTO(UserInfo userInfo, Class<T> resourceClass) throws DlabException {
    	T resource = newResourceBaseDTO(userInfo, resourceClass);
    	return (T) resource
    			.withServiceBaseName(getSettingsDAO().getServiceBaseName())
    			.withConfTagResourceId(getSettingsDAO().getConfTagResourceId())
    			.withConfOsFamily(getSettingsDAO().getConfOsFamily())
            	.withConfOsUser(getSettingsDAO().getConfOsUser());

    }
    
    /** Returns the name of application for notebook: jupyter, rstudio, etc. */
    public static String getApplicationNameFromImage(String imageName) {
    	if (imageName != null) {
    		int pos = imageName.lastIndexOf('-');
    		if (pos > 0) {
    			return imageName.substring(pos + 1);
    		}
    	}
    	return "";
    }
}
