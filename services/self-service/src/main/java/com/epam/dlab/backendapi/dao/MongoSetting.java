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

/** Name of fields in the Mongo collection {@link MongoCollections#SETTINGS}. */
public enum MongoSetting {

    // General properties
	/** Base name of service. */
    SERIVICE_BASE_NAME("conf_service_base_name"),
    /** Name of directory for user key. */
    CONF_KEY_DIRECTORY("conf_key_dir"),
    /** Name of resource id. */
    CONF_TAG_RESOURCE_ID("conf_tag_resource_id"),
    /** Name of OS family. */
    CONF_OS_FAMILY("conf_os_family"),


    // AWS Related properties
    /** Name of AWS region. */
    AWS_REGION("aws_region"),
	/** Id of security group. */
    AWS_SECURITY_GROUPS("aws_security_groups_ids"),
	/** Id of virtual private cloud for AWS account. */
    AWS_VPC_ID("aws_vpc_id"),
	/** Id of virtual private cloud subnet for AWS account. */
    AWS_SUBNET_ID("aws_subnet_id"),


    // Azure related properties
    AZURE_REGION("azure_region"),
    AZURE_RESOURCE_GROUP_NAME("azure_resource_group_name"),
    AZURE_SUBNET_NAME("azure_subnet_name"),
    AZURE_VPC_NAME("azure_vpc_name"),
    AZURE_SECURITY_GROUP_NAME("azure_security_group_name"),
    AZURE_EDGE_INSTANCE_SIZE("edge_instance_size"),
    AZURE_SSN_INSTANCE_SIZE("ssn_instance_size");


    private String id;

    MongoSetting(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
