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

import static com.epam.dlab.backendapi.dao.MongoSetting.*;
import static com.mongodb.client.model.Filters.eq;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/** Stores the environment settings. */
public class SettingsDAO extends BaseDAO {
    private static final String VALUE = "value";

    /** Returns the base name of service. */
    public String getServiceBaseName() {
        return getSetting(SERIVICE_BASE_NAME);
    }

    /** Returns the name of AWS region. */
    public String getAwsRegion() {
        return getSetting(AWS_REGION);
    }

    /** Returns the id of security group. */
    public String getAwsSecurityGroups() {
        return getSetting(AWS_SECURITY_GROUPS);
    }

    /** Returns the OS user name. */
    public String getConfOsUser() {
        return getSetting(CONF_OS_USER);
    }

    /** Returns the name of OS family. */
    public String getConfOsFamily() {
        return getSetting(CONF_OS_FAMILY);
    }

    /** Returns the name of directory for user key. */
    public String getConfKeyDir() {
        return getSetting(CONF_KEY_DIRECTORY);
    }

    /** Returns the id of virtual private cloud for AWS account. */
    public String getAwsVpcId() {
        return getSetting(AWS_VPC_ID);
    }

    /** Returns the id of virtual private cloud subnet for AWS account. */
    public String getAwsSubnetId() {
        return getSetting(AWS_SUBNET_ID);
    }

    /** Returns the value of property from Mongo database.
     * @param setting the name of property.
     */
    private String getSetting(MongoSetting setting) {
        return mongoService.getCollection(SETTINGS)
        		.find(eq(ID, setting.getId()))
        		.first()
        		.getOrDefault(VALUE, EMPTY)
        		.toString();
    }
}
