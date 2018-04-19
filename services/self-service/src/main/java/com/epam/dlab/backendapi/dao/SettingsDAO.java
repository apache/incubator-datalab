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

import com.epam.dlab.exceptions.DlabException;
import org.bson.Document;

import static com.epam.dlab.backendapi.dao.MongoCollections.SETTINGS;
import static com.epam.dlab.backendapi.dao.MongoSetting.*;
import static com.mongodb.client.model.Filters.eq;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Stores the environment settings.
 */
public class SettingsDAO extends BaseDAO {
    private static final String VALUE = "value";

    /**
     * Returns the base name of service.
     */
    public String getServiceBaseName() {
        return getSetting(SERIVICE_BASE_NAME);
    }

    /**
     * Returns the name of OS family.
     */
    public String getConfOsFamily() {
        return getSetting(CONF_OS_FAMILY);
    }

    /**
     * Returns the name of directory for user key.
     */
    public String getConfKeyDir() {
        return getSetting(CONF_KEY_DIRECTORY);
    }

    /**
     * Returns the name of tag for resource id.
     */
    public String getConfTagResourceId() {
        return getSetting(CONF_TAG_RESOURCE_ID);
    }

    /**
     * Returns the name of AWS region.
     */
    public String getAwsRegion() {
        return getSetting(AWS_REGION);
    }

    /**
     * Returns the id of security group.
     */
    public String getAwsSecurityGroups() {
        return getSetting(AWS_SECURITY_GROUPS);
    }

    /**
     * Returns the id of virtual private cloud for AWS account.
     */
    public String getAwsVpcId() {
        return getSetting(AWS_VPC_ID);
    }

    /**
     * Returns the id of virtual private cloud subnet for AWS account.
     */
    public String getAwsSubnetId() {
        return getSetting(AWS_SUBNET_ID);
    }

    public String getAzureRegion() {
        return getSetting(AZURE_REGION);
    }

    public String getAzureResourceGroupName() {
        return getSetting(AZURE_RESOURCE_GROUP_NAME);
    }

    public String getAzureSubnetName() {
        return getSetting(AZURE_SUBNET_NAME);
    }

    public String getAzureVpcName() {
        return getSetting(AZURE_VPC_NAME);
    }

    public String getAzureSecurityGroupName() {
        return getSetting(AZURE_SECURITY_GROUP_NAME);
    }

    public String getAzureEdgeInstanceSize() {
        return getSetting(AZURE_EDGE_INSTANCE_SIZE);
    }

    public String getAzureSsnInstanceSize() {
        return getSetting(AZURE_SSN_INSTANCE_SIZE);
    }

    public String getAzureDataLakeNameTag() {
        return getSetting(AZURE_DATA_LAKE_NAME_TAG, "");
    }

    public boolean isAzureDataLakeEnabled() {
        String dataLakeTagName = getAzureDataLakeNameTag();
        return dataLakeTagName != null && !dataLakeTagName.isEmpty();
    }

    public String getAzureDataLakeClientId() {
        return getSetting(AZURE_DATA_LAKE_CLIENT_ID);
    }

    public String getGcpRegion() {
        return getSetting(GCP_REGION);
    }

    public String getGcpZone() {
        return getSetting(GCP_ZONE);
    }

    public String getGcpSubnetName() {
        return getSetting(GCP_SUBNET_NAME);
    }

    public String getGcpProjectId() {
        return getSetting(GCP_PROJECT_ID);
    }

    public String getGcpVpcName(){
        return getSetting(GCP_VPC_NAME);
    }

    /**
     * Returns the value of property from Mongo database.
     *
     * @param setting the name of property.
     */
    private String getSetting(MongoSetting setting) {
        Document d = mongoService
                .getCollection(SETTINGS)
                .find(eq(ID, setting.getId()))
                .first();
        if (d == null) {
            throw new DlabException("Setting property " + setting + " not found");
        }
        return d.getOrDefault(VALUE, EMPTY).toString();
    }


    private String getSetting(MongoSetting setting, String defaultValue) {
        Document d = mongoService.getCollection(SETTINGS).find(eq(ID, setting.getId())).first();
        if (d == null) {
            return defaultValue;
        }
        return d.getOrDefault(VALUE, defaultValue).toString();
    }
}
