#!/usr/bin/python

# *****************************************************************************
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# ******************************************************************************

import datalab.actions_lib
import datalab.fab
import datalab.meta_lib
import datalab.ssn_lib
import json
import logging
import os
import sys
import traceback
from fabric.api import *


def terminate_ssn_node(resource_group_name, service_base_name, vpc_name, region):
    print("Terminating instances")
    try:
        for vm in AzureMeta.compute_client.virtual_machines.list(resource_group_name):
            if "SBN" in vm.tags and service_base_name == vm.tags["SBN"]:
                AzureActions.remove_instance(resource_group_name, vm.name)
                print("Instance {} has been terminated".format(vm.name))
    except Exception as err:
        datalab.fab.append_result("Failed to terminate instances", str(err))
        sys.exit(1)

    print("Removing network interfaces")
    try:
        for network_interface in AzureMeta.list_network_interfaces(resource_group_name):
            if "SBN" in network_interface.tags and service_base_name == network_interface.tags["SBN"]:
                AzureActions.delete_network_if(resource_group_name, network_interface.name)
                print("Network interface {} has been removed".format(network_interface.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove network interfaces", str(err))
        sys.exit(1)

    print("Removing static public IPs")
    try:
        for static_public_ip in AzureMeta.list_static_ips(resource_group_name):
            if "SBN" in static_public_ip.tags and service_base_name == static_public_ip.tags["SBN"]:
                AzureActions.delete_static_public_ip(resource_group_name, static_public_ip.name)
                print("Static public IP {} has been removed".format(static_public_ip.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove static IPs", str(err))
        sys.exit(1)

    print("Removing disks")
    try:
        for disk in AzureMeta.list_disks(resource_group_name):
            if "SBN" in disk.tags and service_base_name == disk.tags["SBN"]:
                AzureActions.remove_disk(resource_group_name, disk.name)
                print("Disk {} has been removed".format(disk.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove disks", str(err))
        sys.exit(1)

    print("Removing storage accounts")
    try:
        for storage_account in AzureMeta.list_storage_accounts(resource_group_name):
            if "SBN" in storage_account.tags and service_base_name == storage_account.tags["SBN"]:
                AzureActions.remove_storage_account(resource_group_name, storage_account.name)
                print("Storage account {} has been terminated".format(storage_account.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove storage accounts", str(err))
        sys.exit(1)

    print("Removing Data Lake Store")
    try:
        for datalake in AzureMeta.list_datalakes(resource_group_name):
            if "SBN" in datalake.tags and service_base_name == datalake.tags["SBN"]:
                AzureActions.delete_datalake_store(resource_group_name, datalake.name)
                print("Data Lake Store {} has been terminated".format(datalake.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove Data Lake", str(err))
        sys.exit(1)

    print("Removing images")
    try:
        for image in AzureMeta.list_images():
            if "SBN" in image.tags and service_base_name == image.tags["SBN"]:
                AzureActions.remove_image(resource_group_name, image.name)
                print("Image {} has been removed".format(image.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove images", str(err))
        sys.exit(1)

    print("Removing security groups")
    try:
        for sg in AzureMeta.network_client.network_security_groups.list(resource_group_name):
            if "SBN" in sg.tags and service_base_name == sg.tags["SBN"]:
                AzureActions.remove_security_group(resource_group_name, sg.name)
                print("Security group {} has been terminated".format(sg.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove security groups", str(err))
        sys.exit(1)

    print("Removing VPC")
    try:
        if AzureMeta.get_vpc(resource_group_name, service_base_name + '-vpc'):
            AzureActions.remove_vpc(resource_group_name, vpc_name)
            print("VPC {} has been terminated".format(vpc_name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove VPC", str(err))
        sys.exit(1)

    print("Removing Resource Group")
    try:
        if AzureMeta.get_resource_group(resource_group_name) and resource_group_name == '{}-resource-group'.format(service_base_name):
            AzureActions.remove_resource_group(resource_group_name, region)
            print("Resource group {} has been terminated".format(resource_group_name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove resource group", str(err))
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    # generating variables dictionary
    AzureMeta = datalab.meta_lib.AzureMeta()
    AzureActions = datalab.actions_lib.AzureActions()
    print('Generating infrastructure names and tags')
    ssn_conf = dict()
    ssn_conf['service_base_name'] = datalab.fab.replace_multi_symbols(os.environ['conf_service_base_name'][:20],
                                                                      '-', True)
    ssn_conf['resource_group_name'] = os.environ.get(
        'azure_resource_group_name', '{}-resource-group'.format(ssn_conf['service_base_name']))
    ssn_conf['region'] = os.environ['azure_region']
    ssn_conf['vpc_name'] = os.environ['azure_vpc_name']

    try:
        logging.info('[TERMINATE SSN]')
        print('[TERMINATE SSN]')
        try:
            terminate_ssn_node(ssn_conf['resource_group_name'], ssn_conf['service_base_name'], ssn_conf['vpc_name'],
                               ssn_conf['region'])
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to terminate ssn.", str(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": ssn_conf['service_base_name'],
                   "Action": "Terminate ssn with all service_base_name environment"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
