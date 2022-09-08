#!/usr/bin/python3

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
from datalab.logger import logging
import os
import sys
import traceback
import requests
from fabric import *


def terminate_ssn_node(resource_group_name, service_base_name, vpc_name, region):
    logging.info("Terminating HDINSIGHT clusters")
    try:
        for cluster in AzureMeta.list_hdinsight_clusters(resource_group_name):
            if "SBN" in cluster.tags and service_base_name == cluster.tags["SBN"]:
                AzureActions.terminate_hdinsight_cluster(resource_group_name, cluster.name)
                logging.info("Cluster {} has been terminated".format(cluster.name))
    except Exception as err:
        datalab.fab.append_result("Failed to terminate HDINSIGHT clusters", str(err))
        sys.exit(1)

    logging.info("Terminating instances")
    try:
        for vm in AzureMeta.compute_client.virtual_machines.list(resource_group_name):
            if "SBN" in vm.tags and service_base_name == vm.tags["SBN"]:
                AzureActions.remove_instance(resource_group_name, vm.name)
                logging.info("Instance {} has been terminated".format(vm.name))
    except Exception as err:
        datalab.fab.append_result("Failed to terminate instances", str(err))
        sys.exit(1)

    logging.info("Removing network interfaces")
    try:
        for network_interface in AzureMeta.list_network_interfaces(resource_group_name):
            if "SBN" in network_interface.tags and service_base_name == network_interface.tags["SBN"]:
                AzureActions.delete_network_if(resource_group_name, network_interface.name)
                logging.info("Network interface {} has been removed".format(network_interface.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove network interfaces", str(err))
        sys.exit(1)

    logging.info("Removing static public IPs")
    try:
        for static_public_ip in AzureMeta.list_static_ips(resource_group_name):
            if "SBN" in static_public_ip.tags and service_base_name == static_public_ip.tags["SBN"]:
                AzureActions.delete_static_public_ip(resource_group_name, static_public_ip.name)
                logging.info("Static public IP {} has been removed".format(static_public_ip.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove static IPs", str(err))
        sys.exit(1)

    logging.info("Removing disks")
    try:
        for disk in AzureMeta.list_disks(resource_group_name):
            if "SBN" in disk.tags and service_base_name == disk.tags["SBN"]:
                AzureActions.remove_disk(resource_group_name, disk.name)
                logging.info("Disk {} has been removed".format(disk.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove disks", str(err))
        sys.exit(1)

    logging.info("Removing storage accounts")
    try:
        for storage_account in AzureMeta.list_storage_accounts(resource_group_name):
            if "SBN" in storage_account.tags and service_base_name == storage_account.tags["SBN"]:
                AzureActions.remove_storage_account(resource_group_name, storage_account.name)
                logging.info("Storage account {} has been terminated".format(storage_account.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove storage accounts", str(err))
        sys.exit(1)

   # logging.info("Removing Data Lake Store")
   # try:
   #     for datalake in AzureMeta.list_datalakes(resource_group_name):
   #         if "SBN" in datalake.tags and service_base_name == datalake.tags["SBN"]:
   #             AzureActions.delete_datalake_store(resource_group_name, datalake.name)
   #             logging.info("Data Lake Store {} has been terminated".format(datalake.name))
   # except Exception as err:
   #     datalab.fab.append_result("Failed to remove Data Lake", str(err))
   #     sys.exit(1)

    logging.info("Removing images")
    try:
        for image in AzureMeta.list_images():
            if "SBN" in image.tags and service_base_name == image.tags["SBN"]:
                AzureActions.remove_image(resource_group_name, image.name)
                logging.info("Image {} has been removed".format(image.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove images", str(err))
        sys.exit(1)

    logging.info("Removing security groups")
    try:
        for sg in AzureMeta.network_client.network_security_groups.list(resource_group_name):
            if "SBN" in sg.tags and service_base_name == sg.tags["SBN"]:
                AzureActions.remove_security_group(resource_group_name, sg.name)
                logging.info("Security group {} has been terminated".format(sg.name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove security groups", str(err))
        sys.exit(1)

    if 'azure_vpc_name' in os.environ:
        logging.info("Removing subnets in predefined VPC")
        try:
            for subnet in AzureMeta.list_subnets(resource_group_name, os.environ['azure_vpc_name']):
                subnet_name = str(subnet)[str(subnet).find("'name': '") + 9 : str(subnet).find("', 'etag':")]
                if service_base_name in subnet_name:
                    AzureActions.remove_subnet(resource_group_name, os.environ['azure_vpc_name'], subnet_name)
                    logging.info("Subnet {} has been removed from VPC {}".format(subnet_name, os.environ['azure_vpc_name']))
        except Exception as err:
            datalab.fab.append_result("Failed to remove subnets in predefined VPC", str(err))
            sys.exit(1)

    logging.info("Removing rules in predefined edge security group")
    try:
        if 'azure_edge_security_group_name' in os.environ:
            for rule in AzureMeta.list_security_group_rules(resource_group_name, os.environ['azure_edge_security_group_name']):
                rule_name = str(rule)[str(rule).find("'name': '") + 9 : str(rule).find("', 'etag':")]
                if service_base_name in rule_name:
                    AzureActions.remove_security_rules(os.environ['azure_edge_security_group_name'],
                                               resource_group_name, rule_name)
                    logging.info("Rule {} is removed".format(rule_name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove rules in predefined edge security group", str(err))
        sys.exit(1)

    logging.info("Removing VPC")
    try:
        if AzureMeta.get_vpc(resource_group_name, service_base_name + '-vpc'):
            AzureActions.remove_vpc(resource_group_name, vpc_name)
            logging.info("VPC {} has been terminated".format(vpc_name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove VPC", str(err))
        sys.exit(1)

    logging.info("Removing Resource Group")
    try:
        if AzureMeta.get_resource_group(resource_group_name) and resource_group_name == '{}-resource-group'.format(service_base_name):
            AzureActions.remove_resource_group(resource_group_name, region)
            logging.info("Resource group {} has been terminated".format(resource_group_name))
    except Exception as err:
        datalab.fab.append_result("Failed to remove resource group", str(err))
        sys.exit(1)

    try:
        logging.info('[KEYCLOAK SSN CLIENT DELETE]')
        keycloak_auth_server_url = '{}/realms/master/protocol/openid-connect/token'.format(
            os.environ['keycloak_auth_server_url'])
        keycloak_client_url = '{0}/admin/realms/{1}/clients'.format(os.environ['keycloak_auth_server_url'],
                                                                           os.environ['keycloak_realm_name'])

        keycloak_auth_data = {
            "username": os.environ['keycloak_user'],
            "password": os.environ['keycloak_user_password'],
            "grant_type": "password",
            "client_id": "admin-cli",
        }

        client_params = {
            "clientId": '{}-ui'.format(ssn_conf['service_base_name'])
        }

        keycloak_token = requests.post(keycloak_auth_server_url, data=keycloak_auth_data).json()

        keycloak_get_id_client = requests.get(keycloak_client_url, data=keycloak_auth_data, params=client_params,
                                              headers={"Authorization": "Bearer " + keycloak_token.get("access_token"),
                                                       "Content-Type": "application/json"})
        json_keycloak_client_id = json.loads(keycloak_get_id_client.text)
        if not json_keycloak_client_id:
            logging.info("Unable to find {}-* Keycloak clients".format(ssn_conf['service_base_name']))
        else:
            keycloak_id_client = json_keycloak_client_id[0]['id']
            keycloak_client_delete_url = '{0}/admin/realms/{1}/clients/{2}'.format(os.environ['keycloak_auth_server_url'],
                                                                                   os.environ['keycloak_realm_name'],
                                                                                   keycloak_id_client)
            keycloak_client = requests.delete(keycloak_client_delete_url,
                                              headers={"Authorization": "Bearer {}".format(keycloak_token.get("access_token")),
                                                       "Content-Type": "application/json"})
    except Exception as err:
        logging.info("Failed to remove ssn client from Keycloak", str(err))


if __name__ == "__main__":
    # generating variables dictionary
    AzureMeta = datalab.meta_lib.AzureMeta()
    AzureActions = datalab.actions_lib.AzureActions()
    logging.info('Generating infrastructure names and tags')
    ssn_conf = dict()
    ssn_conf['service_base_name'] = datalab.fab.replace_multi_symbols(os.environ['conf_service_base_name'][:20],
                                                                      '-', True)
    ssn_conf['resource_group_name'] = os.environ.get(
        'azure_resource_group_name', '{}-resource-group'.format(ssn_conf['service_base_name']))
    ssn_conf['region'] = os.environ['azure_region']
    ssn_conf['vpc_name'] = os.environ['azure_vpc_name']

    try:
        logging.info('[TERMINATE SSN]')
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
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
