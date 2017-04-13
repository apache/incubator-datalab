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
	/** Base name of service. */
    SERIVICE_BASE_NAME("conf_service_base_name"),
	/** Name of AWS region. */
    AWS_REGION("aws_region"),
	/** Id of security group. */
    AWS_SECURITY_GROUPS("aws_security_groups_ids"),
	/** OS user name. */
    CONF_OS_USER("conf_os_user"),
	/** Name of OS family. */
    CONF_OS_FAMILY("conf_os_family"),
	/** Name of directory for user key. */
    CONF_KEY_DIRECTORY("conf_key_dir"),
	/** Id of virtual private cloud for AWS account. */
    AWS_VPC_ID("aws_vpc_id"),
	/** Id of virtual private cloud subnet for AWS account. */
    AWS_SUBNET_ID("aws_subnet_id"),
	/** Name of resource id. */
    CONF_TAG_RESOURCE_ID("conf_tag_resource_id");

    private String id;

    MongoSetting(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
