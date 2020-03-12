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

import json
import sys
import time
import os
import dlab.fab
import dlab.actions_lib
import dlab.meta_lib
import logging
import traceback


def terminate_edge_node(resource_group_name, service_base_name, project_tag, subnet_name, vpc_name):
    print("Terminating EDGE, notebook and dataengine virtual machines")
    try:
        for vm in AzureMeta.compute_client.virtual_machines.list(resource_group_name):
            try:
                if project_tag == vm.tags["project_tag"]:
                    AzureActions.remove_instance(resource_group_name, vm.name)
                    print("Instance {} has been terminated".format(vm.name))
            except:
                pass
    except Exception as err:
        dlab.fab.append_result("Failed to terminate instance", str(err))
        sys.exit(1)

    print("Removing network interfaces")
    try:
        for network_interface in AzureMeta.list_network_interfaces(resource_group_name):
            try:
                if project_tag == network_interface.tags["project_tag"]:
                    AzureActions.delete_network_if(resource_group_name, network_interface.name)
                    print("Network interface {} has been removed".format(network_interface.name))
            except:
                pass
    except Exception as err:
        dlab.fab.append_result("Failed to remove network interfaces", str(err))
        sys.exit(1)

    print("Removing static public IPs")
    try:
        for static_public_ip in AzureMeta.list_static_ips(resource_group_name):
            try:
                if project_tag in static_public_ip.tags["project_tag"]:
                    AzureActions.delete_static_public_ip(resource_group_name, static_public_ip.name)
                    print("Static public IP {} has been removed".format(static_public_ip.name))
            except:
                pass
    except Exception as err:
        dlab.fab.append_result("Failed to remove static IPs", str(err))
        sys.exit(1)

    print("Removing disks")
    try:
        for disk in AzureMeta.list_disks(resource_group_name):
            try:
                if project_tag in disk.tags["project_tag"]:
                    AzureActions.remove_disk(resource_group_name, disk.name)
                    print("Disk {} has been removed".format(disk.name))
            except:
                pass
    except Exception as err:
        dlab.fab.append_result("Failed to remove disks", str(err))
        sys.exit(1)

    print("Removing storage account")
    try:
        for storage_account in AzureMeta.list_storage_accounts(resource_group_name):
            try:
                if project_tag == storage_account.tags["project_tag"]:
                    AzureActions.remove_storage_account(resource_group_name, storage_account.name)
                    print("Storage account {} has been terminated".format(storage_account.name))
            except:
                pass
    except Exception as err:
        dlab.fab.append_result("Failed to remove storage accounts", str(err))
        sys.exit(1)

    print("Deleting Data Lake Store directory")
    try:
        for datalake in AzureMeta.list_datalakes(resource_group_name):
            try:
                if service_base_name == datalake.tags["SBN"]:
                    AzureActions.remove_datalake_directory(datalake.name, project_tag + '-folder')
                    print("Data Lake Store directory {} has been deleted".format(project_tag + '-folder'))
            except:
                pass
    except Exception as err:
        dlab.fab.append_result("Failed to remove Data Lake", str(err))
        sys.exit(1)

    print("Removing security groups")
    try:
        for sg in AzureMeta.network_client.network_security_groups.list(resource_group_name):
            try:
                if project_tag == sg.tags["project_tag"]:
                    AzureActions.remove_security_group(resource_group_name, sg.name)
                    print("Security group {} has been terminated".format(sg.name))
            except:
                pass
    except Exception as err:
        dlab.fab.append_result("Failed to remove security groups", str(err))
        sys.exit(1)

    print("Removing private subnet")
    try:
        AzureActions.remove_subnet(resource_group_name, vpc_name, subnet_name)
        print("Private subnet {} has been terminated".format(subnet_name))
    except Exception as err:
        dlab.fab.append_result("Failed to remove subnet", str(err))
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print('Generating infrastructure names and tags')
    AzureMeta = dlab.meta_lib.AzureMeta()
    AzureActions = dlab.actions_lib.AzureActions()
    edge_conf = dict()
    edge_conf['service_base_name'] = os.environ['conf_service_base_name']
    edge_conf['resource_group_name'] = os.environ['azure_resource_group_name']
    edge_conf['user_name'] = os.environ['edge_user_name']
    edge_conf['project_name'] = os.environ['project_name']
    edge_conf['project_tag'] = edge_conf['project_name']
    edge_conf['endpoint_name'] = os.environ['endpoint_name']
    edge_conf['private_subnet_name'] = "{}-{}-{}-subnet".format(edge_conf['service_base_name'],
                                                                edge_conf['project_name'], edge_conf['endpoint_name'])
    edge_conf['vpc_name'] = os.environ['azure_vpc_name']


    try:
        logging.info('[TERMINATE EDGE]')
        print('[TERMINATE EDGE]')
        try:
            terminate_edge_node(edge_conf['resource_group_name'], edge_conf['service_base_name'],
                                edge_conf['project_tag'], edge_conf['private_subnet_name'], edge_conf['vpc_name'])
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed to terminate edge.", str(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": os.environ['conf_service_base_name'],
                   "project_name": edge_conf['project_name'],
                   "Action": "Terminate edge node"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
