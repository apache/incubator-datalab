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
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.ResourceBaseDTO;
import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;

/** Utilities for resource methods.
 */
public class ResourceUtils {

	@Inject
	private static SettingsDAO settingsDAO;

	/** Instantiate, initialize and return class instance which inherits from {@link com.epam.dlab.dto.ResourceBaseDTO}.
	 * Initialize: AWS region, AWS IAM user name, EDGE user name.
	 * @param userInfo the user info.
	 * @param resourceClass the class for instantiate.
	 * @return initialized <b>resourceClass</b> instance.
	 * @throws DlabException
	 */
    //TODO @dto
	@SuppressWarnings("unchecked")
	public static <T extends ResourceBaseDTO<?>> T newResourceBaseDTO(UserInfo userInfo, Class<T> resourceClass, CloudProvider cloudProvider) throws DlabException {
		try {
			T resource = resourceClass.newInstance();
			switch (cloudProvider) {
				case AWS:
					return (T) resource
							.withAwsRegion(settingsDAO.getAwsRegion())
							.withAwsIamUser(userInfo.getName())
							.withEdgeUserName(userInfo.getSimpleName());
				case AZURE:
				case GCP:
					return (T) resource.withEdgeUserName(userInfo.getSimpleName());
				default:
					throw new DlabException("Unknown cloud provider" + cloudProvider);
			}

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
	//TODO @dto
    @SuppressWarnings("unchecked")
	public static <T extends ResourceSysBaseDTO<?>> T newResourceSysBaseDTO(UserInfo userInfo, Class<T> resourceClass, CloudProvider cloudProvider) throws DlabException {
    	T resource = newResourceBaseDTO(userInfo, resourceClass, cloudProvider);

    	switch (cloudProvider) {
			case AWS:
				resource.withConfTagResourceId(settingsDAO.getConfTagResourceId());
				break;

		}
    	return (T) resource
    			.withServiceBaseName(settingsDAO.getServiceBaseName())
    			.withConfOsFamily(settingsDAO.getConfOsFamily());

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
