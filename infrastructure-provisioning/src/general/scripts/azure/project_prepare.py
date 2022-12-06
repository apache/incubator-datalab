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

import datalab.fab
import datalab.actions_lib
import datalab.meta_lib
import json
from datalab.logger import logging
import os
import sys
import traceback
import subprocess
from Crypto.PublicKey import RSA
from fabric import *

if __name__ == "__main__":
    try:
        logging.info('Generating infrastructure names and tags')
        AzureMeta = datalab.meta_lib.AzureMeta()
        AzureActions = datalab.actions_lib.AzureActions()
        project_conf = dict()
        project_conf['service_base_name'] = os.environ['conf_service_base_name'] = datalab.fab.replace_multi_symbols(
            os.environ['conf_service_base_name'][:20], '-', True)
        project_conf['project_name'] = (os.environ['project_name'])
        project_conf['project_tag'] = project_conf['project_name']
        project_conf['endpoint_name'] = (os.environ['endpoint_name'])
        project_conf['endpoint_tag'] = project_conf['endpoint_name']
        project_conf['resource_group_name'] = os.environ['azure_resource_group_name']

        project_conf['azure_ad_user_name'] = os.environ['azure_iam_user']
        project_conf['key_name'] = os.environ['conf_key_name']
        project_conf['tag_name'] = project_conf['service_base_name'] + '-tag'
        project_conf['vpc_name'] = os.environ['azure_vpc_name']
        project_conf['subnet_name'] = os.environ['azure_subnet_name']
        project_conf['private_subnet_name'] = '{}-{}-{}-subnet'.format(project_conf['service_base_name'],
                                                                       project_conf['project_name'],
                                                                       project_conf['endpoint_name'])
        if os.environ['conf_network_type'] == 'private':
            project_conf['static_public_ip_name'] = 'None'
        else:
            project_conf['static_public_ip_name'] = '{}-{}-{}-edge-static-ip'.format(project_conf['service_base_name'],
                                                                                     project_conf['project_name'],
                                                                                     project_conf['endpoint_name'])
        project_conf['region'] = os.environ['azure_region']
        project_conf['vpc_cidr'] = os.environ['conf_vpc_cidr']
        project_conf['private_subnet_prefix'] = os.environ['conf_private_subnet_prefix']

        project_conf['instance_name'] = '{0}-{1}-{2}-edge'.format(project_conf['service_base_name'],
                                                                  project_conf['project_name'],
                                                                  project_conf['endpoint_tag'])
        project_conf['network_interface_name'] = '{0}-nif'.format(project_conf['instance_name'])
        project_conf['primary_disk_name'] = project_conf['instance_name'] + '-volume-0'
        project_conf['edge_security_group_name'] = project_conf['instance_name'] + '-sg'
        project_conf['notebook_security_group_name'] = '{}-{}-{}-nb-sg'.format(project_conf['service_base_name'],
                                                                               project_conf['project_name'],
                                                                               project_conf['endpoint_name'])
        project_conf['master_security_group_name'] = '{}-{}-{}-de-master-sg'.format(project_conf['service_base_name'],
                                                                                    project_conf['project_name'],
                                                                                    project_conf['endpoint_name'])
        project_conf['slave_security_group_name'] = '{}-{}-{}-de-slave-sg'.format(project_conf['service_base_name'],
                                                                                  project_conf['project_name'],
                                                                                  project_conf['endpoint_name'])
        project_conf['edge_storage_account_name'] = ('{0}-{1}-{2}-bucket'.format(project_conf['service_base_name'],
                                                                                 project_conf['project_name'],
                                                                                 project_conf['endpoint_name'])).lower()
        project_conf['edge_container_name'] = ('{0}-{1}-{2}-bucket'.format(project_conf['service_base_name'],
                                                                           project_conf['project_name'],
                                                                           project_conf['endpoint_name'])).lower()
        project_conf['datalake_store_name'] = '{}-ssn-datalake'.format(project_conf['service_base_name'])
        project_conf['datalake_user_directory_name'] = '{0}-{1}-{2}-folder'.format(project_conf['service_base_name'],
                                                                                   project_conf['project_name'],
                                                                                   project_conf['endpoint_name'])
        ssh_key_path = os.environ['conf_key_dir'] + os.environ['conf_key_name'] + '.pem'
        key = RSA.importKey(open(ssh_key_path, 'rb').read())
        project_conf['public_ssh_key'] = key.publickey().exportKey("OpenSSH").decode('UTF-8')
        project_conf['instance_storage_account_type'] = 'Premium_LRS'
        project_conf['image_name'] = os.environ['azure_{}_image_name'.format(os.environ['conf_os_family'])]
        project_conf['instance_tags'] = {"Name": project_conf['instance_name'],
                                         "SBN": project_conf['service_base_name'],
                                         "project_tag": project_conf['project_tag'],
                                         "endpoint_tag": project_conf['endpoint_tag'],
                                         os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value']}
        project_conf['storage_account_tags'] = {"Name": project_conf['edge_storage_account_name'],
                                                "SBN": project_conf['service_base_name'],
                                                "project_tag": project_conf['project_tag'],
                                                "endpoint_tag": project_conf['endpoint_tag'],
                                                os.environ['conf_billing_tag_key']:
                                                    os.environ['conf_billing_tag_value'],
                                                project_conf['tag_name']: project_conf['edge_storage_account_name']}
        project_conf['primary_disk_size'] = '32'
        project_conf['shared_storage_account_name'] = ('{0}-{1}-shared-bucket'.format(
            project_conf['service_base_name'], project_conf['endpoint_name'])).lower()
        project_conf['shared_container_name'] = ('{}-{}-shared-bucket'.format(project_conf['service_base_name'],
                                                                              project_conf['endpoint_name'])).lower()
        project_conf['shared_storage_account_tags'] = {"Name": project_conf['shared_storage_account_name'],
                                                       "SBN": project_conf['service_base_name'],
                                                       os.environ['conf_billing_tag_key']: os.environ[
                                                       'conf_billing_tag_value'], "endpoint_tag":
                                                           project_conf['endpoint_tag'],
                                                       project_conf['tag_name']:
                                                           project_conf['shared_storage_account_name']}

        # FUSE in case of absence of user's key
        try:
            project_conf['user_key'] = os.environ['key']
            try:
                subprocess.run('echo "{0}" >> {1}{2}.pub'.format(project_conf['user_key'], os.environ['conf_key_dir'],
                                                        project_conf['project_name']), shell=True, check=True)
            except:
                logging.info("ADMINSs PUBLIC KEY DOES NOT INSTALLED")
        except KeyError:
            logging.error("ADMINSs PUBLIC KEY DOES NOT UPLOADED")
            sys.exit(1)

        logging.info("Will create exploratory environment with edge node as access point as following: {}".format(json.dumps(
            project_conf, sort_keys=True, indent=4, separators=(',', ': '))))
        logging.info(json.dumps(project_conf))
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        traceback.print_exc()
        sys.exit(1)

    try:
        logging.info('[CREATE SUBNET]')
        params = "--resource_group_name {} --vpc_name {} --region {} --vpc_cidr {} --subnet_name {} --prefix {}".\
            format(project_conf['resource_group_name'], project_conf['vpc_name'], project_conf['region'],
                   project_conf['vpc_cidr'], project_conf['private_subnet_name'], project_conf['private_subnet_prefix'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_subnet', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        try:
            AzureActions.remove_subnet(project_conf['resource_group_name'], project_conf['vpc_name'],
                                       project_conf['private_subnet_name'])
        except:
            logging.info("Subnet hasn't been created.")
        datalab.fab.append_result("Failed to create subnet.", str(err))
        sys.exit(1)

    project_conf['private_subnet_cidr'] = AzureMeta.get_subnet(project_conf['resource_group_name'],
                                                               project_conf['vpc_name'],
                                                               project_conf['private_subnet_name']).address_prefix
    logging.info('NEW SUBNET CIDR CREATED: {}'.format(project_conf['private_subnet_cidr']))

    try:
        if 'azure_edge_security_group_name' in os.environ:
            logging.info('Security group predefined, adding new rule with endpoint IP')
            logging.info('Security group predefined, adding new rule with endpoint IP')
            if project_conf['endpoint_name'] == 'local':
                endpoint_ip = AzureMeta.get_instance_public_ip_address(project_conf['resource_group_name'],
                                                          '{}-ssn'.format(project_conf['service_base_name']))
            else:
                endpoint_ip = AzureMeta.get_instance_public_ip_address(project_conf['resource_group_name'],
                                                         '{}-{}-endpoint'.format(project_conf['service_base_name'], project_conf['endpoint_name']))
            priority = 110
            priorities = list()
            rules_list = AzureMeta.get_security_group(project_conf['resource_group_name'], os.environ['azure_edge_security_group_name'])
            for rule in rules_list.as_dict()['security_rules']:
                priorities.append(rule['priority'])
            while priority in priorities:
                priority += 10
            edge_list_rules = [
                {
                    "name": '{}-{}-{}-rule'.format(project_conf['service_base_name'],
                                                 project_conf['project_name'],
                                                 project_conf['endpoint_tag']),
                    "protocol": "*",
                    "source_port_range": "*",
                    "destination_port_range": "*",
                    "source_address_prefix": endpoint_ip,
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": priority,
                    "direction": "Inbound"
                }
            ]
            params = "--resource_group_name {} --security_group_name {} --region {} --tags '{}' --list_rules '{}'". \
                format(project_conf['resource_group_name'], os.environ['azure_edge_security_group_name'],
                       project_conf['region'], json.dumps({"product": "datalab"}), json.dumps(edge_list_rules))
            try:
                subprocess.run("~/scripts/{}.py {}".format('common_create_security_group', params), shell=True, check=True)
            except Exception as err:
                AzureActions.remove_subnet(project_conf['resource_group_name'], project_conf['vpc_name'],
                                           project_conf['private_subnet_name'])
        else:
            logging.info('[CREATE SECURITY GROUP FOR EDGE NODE]')
            logging.info('[CREATE SECURITY GROUP FOR EDGE]')
            edge_list_rules = [
                {
                    "name": "in-1",
                    "protocol": "*",
                    "source_port_range": "*",
                    "destination_port_range": "*",
                    "source_address_prefix": project_conf['private_subnet_cidr'],
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 100,
                    "direction": "Inbound"
                },
                {
                    "name": "in-2",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "22",
                    "source_address_prefix": "*",
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 110,
                    "direction": "Inbound"
                },
                {
                    "name": "in-3",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "3128",
                    "source_address_prefix": "*",
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 120,
                    "direction": "Inbound"
                },
                {
                    "name": "in-4",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "80",
                    "source_address_prefix": "*",
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 130,
                    "direction": "Inbound"
                },
                {
                    "name": "in-5",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "443",
                    "source_address_prefix": "*",
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 140,
                    "direction": "Inbound"
                },
                {
                    "name": "out-1",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "22",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 100,
                    "direction": "Outbound"
                },
                {
                    "name": "out-2",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "8888",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 110,
                    "direction": "Outbound"
                },
                {
                    "name": "out-3",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "8080",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 120,
                    "direction": "Outbound"
                },
                {
                    "name": "out-4",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "8787",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 130,
                    "direction": "Outbound"
                },
                {
                    "name": "out-5",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "6006",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 140,
                    "direction": "Outbound"
                },
                {
                    "name": "out-6",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "20888",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 150,
                    "direction": "Outbound"
                },
                {
                    "name": "out-7",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "8088",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 160,
                    "direction": "Outbound"
                },
                {
                    "name": "out-8",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "18080",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 170,
                    "direction": "Outbound"
                },
                {
                    "name": "out-9",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "50070",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 180,
                    "direction": "Outbound"
                },
                {
                    "name": "out-10",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "8085",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 190,
                    "direction": "Outbound"
                },
                {
                    "name": "out-11",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "8081",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 200,
                    "direction": "Outbound"
                },
                {
                    "name": "out-12",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "4040-4140",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 210,
                    "direction": "Outbound"
                },
                {
                    "name": "out-13",
                    "protocol": "Udp",
                    "source_port_range": "*",
                    "destination_port_range": "53",
                    "source_address_prefix": '*',
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 220,
                    "direction": "Outbound"
                },
                {
                    "name": "out-14",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "80",
                    "source_address_prefix": '*',
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 230,
                    "direction": "Outbound"
                },
                {
                    "name": "out-15",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "443",
                    "source_address_prefix": '*',
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 240,
                    "direction": "Outbound"
                },
                {
                    "name": "out-16",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "389",
                    "source_address_prefix": '*',
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 250,
                    "direction": "Outbound"
                },
                {
                    "name": "out-17",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "8042",
                    "source_address_prefix": "*",
                    "destination_address_prefix": project_conf['private_subnet_cidr'],
                    "access": "Allow",
                    "priority": 260,
                    "direction": "Outbound"
                },
                {
                    "name": "out-18",
                    "protocol": "Udp",
                    "source_port_range": "*",
                    "destination_port_range": "123",
                    "source_address_prefix": "*",
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 270,
                    "direction": "Outbound"
                },
                {
                    "name": "out-19",
                    "protocol": "*",
                    "source_port_range": "*",
                    "destination_port_range": "*",
                    "source_address_prefix": "*",
                    "destination_address_prefix": "*",
                    "access": "Deny",
                    "priority": 300,
                    "direction": "Outbound"
                }
            ]
            params = "--resource_group_name {} --security_group_name {} --region {} --tags '{}' --list_rules '{}'". \
                format(project_conf['resource_group_name'], project_conf['edge_security_group_name'],
                       project_conf['region'], json.dumps(project_conf['instance_tags']), json.dumps(edge_list_rules))
            try:
                subprocess.run("~/scripts/{}.py {}".format('common_create_security_group', params), shell=True, check=True)
            except Exception as err:
                AzureActions.remove_subnet(project_conf['resource_group_name'], project_conf['vpc_name'],
                                           project_conf['private_subnet_name'])
                try:
                    AzureActions.remove_security_group(project_conf['resource_group_name'],
                                                       project_conf['edge_security_group_name'])
                except:
                    logging.info("Edge Security group hasn't been created.")
                traceback.print_exc()
                datalab.fab.append_result("Failed creating security group for edge node.", str(err))
                raise Exception
    except:
        traceback.print_exc()
        sys.exit(1)

    try:
        logging.info('[CREATE SECURITY GROUP FOR PRIVATE SUBNET]')
        notebook_list_rules = [
            {
                "name": "in-1",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": project_conf['private_subnet_cidr'],
                "destination_address_prefix": "*",
                "access": "Allow",
                "priority": 100,
                "direction": "Inbound"
            },
            {
                "name": "in-2",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": AzureMeta.get_subnet(project_conf['resource_group_name'],
                                                              project_conf['vpc_name'],
                                                              project_conf['subnet_name']).address_prefix,
                "destination_address_prefix": "*",
                "access": "Allow",
                "priority": 110,
                "direction": "Inbound"
            },
            {
                "name": "in-3",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": "*",
                "destination_address_prefix": "*",
                "access": "Deny",
                "priority": 200,
                "direction": "Inbound"
            },
            {
                "name": "out-1",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": "*",
                "destination_address_prefix": project_conf['private_subnet_cidr'],
                "access": "Allow",
                "priority": 100,
                "direction": "Outbound"
            },
            {
                "name": "out-2",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": "*",
                "destination_address_prefix": AzureMeta.get_subnet(project_conf['resource_group_name'],
                                                                   project_conf['vpc_name'],
                                                                   project_conf['subnet_name']).address_prefix,
                "access": "Allow",
                "priority": 110,
                "direction": "Outbound"
            },
            {
                "name": "out-3",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "443",
                "source_address_prefix": "*",
                "destination_address_prefix": "*",
                "access": "Allow",
                "priority": 120,
                "direction": "Outbound"
            },
            {
                "name": "out-4",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": "*",
                "destination_address_prefix": "*",
                "access": "Deny",
                "priority": 200,
                "direction": "Outbound"
            }
            ]
        params = "--resource_group_name {} --security_group_name {} --region {} --tags '{}' --list_rules '{}'". \
            format(project_conf['resource_group_name'], project_conf['notebook_security_group_name'],
                   project_conf['region'], json.dumps(project_conf['instance_tags']), json.dumps(notebook_list_rules))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_security_group', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed creating security group for private subnet.", str(err))
        AzureActions.remove_subnet(project_conf['resource_group_name'], project_conf['vpc_name'],
                                   project_conf['private_subnet_name'])
        if 'azure_edge_security_group_name' not in os.environ:
            AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['edge_security_group_name'])
        try:
            AzureActions.remove_security_group(project_conf['resource_group_name'],
                                               project_conf['notebook_security_group_name'])
        except:
            logging.info("Notebook Security group hasn't been created.")
        sys.exit(1)

    logging.info('[CREATING SECURITY GROUPS FOR MASTER NODE]')
    try:
        cluster_list_rules = [
            {
                "name": "in-1",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": project_conf['private_subnet_cidr'],
                "destination_address_prefix": "*",
                "access": "Allow",
                "priority": 100,
                "direction": "Inbound"
            },
            {
                "name": "in-2",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": AzureMeta.get_subnet(project_conf['resource_group_name'],
                                                              project_conf['vpc_name'],
                                                              project_conf['subnet_name']).address_prefix,
                "destination_address_prefix": "*",
                "access": "Allow",
                "priority": 110,
                "direction": "Inbound"
            },
            {
                "name": "in-3",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": "*",
                "destination_address_prefix": "*",
                "access": "Deny",
                "priority": 200,
                "direction": "Inbound"
            },
            {
                "name": "out-1",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": "*",
                "destination_address_prefix": project_conf['private_subnet_cidr'],
                "access": "Allow",
                "priority": 100,
                "direction": "Outbound"
            },
            {
                "name": "out-2",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": "*",
                "destination_address_prefix": AzureMeta.get_subnet(project_conf['resource_group_name'],
                                                                   project_conf['vpc_name'],
                                                                   project_conf['subnet_name']).address_prefix,
                "access": "Allow",
                "priority": 110,
                "direction": "Outbound"
            },
            {
                "name": "out-3",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "443",
                "source_address_prefix": "*",
                "destination_address_prefix": "*",
                "access": "Allow",
                "priority": 120,
                "direction": "Outbound"
            },
            {
                "name": "out-4",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": "*",
                "destination_address_prefix": "*",
                "access": "Deny",
                "priority": 200,
                "direction": "Outbound"
            }
        ]
        params = "--resource_group_name {} --security_group_name {} --region {} --tags '{}' --list_rules '{}'".format(
            project_conf['resource_group_name'], project_conf['master_security_group_name'], project_conf['region'],
            json.dumps(project_conf['instance_tags']), json.dumps(cluster_list_rules))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_security_group', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        AzureActions.remove_subnet(project_conf['resource_group_name'], project_conf['vpc_name'],
                                   project_conf['private_subnet_name'])
        if 'azure_edge_security_group_name' not in os.environ:
            AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['edge_security_group_name'])
        AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['notebook_security_group_name'])
        try:
            AzureActions.remove_security_group(project_conf['resource_group_name'],
                                               project_conf['master_security_group_name'])
        except:
            logging.info("Master Security group hasn't been created.")
        datalab.fab.append_result("Failed to create Security groups. Exception:" + str(err))
        sys.exit(1)

    logging.info('[CREATING SECURITY GROUPS FOR SLAVE NODES]')
    try:
        params = "--resource_group_name {} --security_group_name {} --region {} --tags '{}' --list_rules '{}'".format(
            project_conf['resource_group_name'], project_conf['slave_security_group_name'], project_conf['region'],
            json.dumps(project_conf['instance_tags']), json.dumps(cluster_list_rules))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_security_group', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        AzureActions.remove_subnet(project_conf['resource_group_name'], project_conf['vpc_name'],
                                   project_conf['private_subnet_name'])
        if 'azure_edge_security_group_name' not in os.environ:
            AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['edge_security_group_name'])
        AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['notebook_security_group_name'])
        AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['master_security_group_name'])
        try:
            AzureActions.remove_security_group(project_conf['resource_group_name'],
                                               project_conf['slave_security_group_name'])
        except:
            logging.info("Slave Security group hasn't been created.")
        datalab.fab.append_result("Failed to create Security groups. Exception:" + str(err))
        sys.exit(1)

    try:
        logging.info('[CREATE SHARED STORAGE ACCOUNT AND CONTAINER]')
        params = "--container_name {} --account_tags '{}' --resource_group_name {} --region {}". \
            format(project_conf['shared_container_name'], json.dumps(project_conf['shared_storage_account_tags']),
                   project_conf['resource_group_name'], project_conf['region'])
        subprocess.run("~/scripts/{}.py {}".format('common_create_storage_account', params), shell=True, check=True)
    except Exception as err:
        datalab.fab.append_result("Failed to create storage account.", str(err))
        AzureActions.remove_subnet(project_conf['resource_group_name'], project_conf['vpc_name'],
                                   project_conf['private_subnet_name'])
        if 'azure_edge_security_group_name' not in os.environ:
            AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['edge_security_group_name'])
        AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['notebook_security_group_name'])
        AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['master_security_group_name'])
        AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['slave_security_group_name'])
        for storage_account in AzureMeta.list_storage_accounts(project_conf['resource_group_name']):
            if project_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions.remove_storage_account(project_conf['resource_group_name'], storage_account.name)
        sys.exit(1)

    try:
        logging.info('[CREATE STORAGE ACCOUNT AND CONTAINERS]')

        params = "--container_name {} --account_tags '{}' --resource_group_name {} --region {}". \
            format(project_conf['edge_container_name'], json.dumps(project_conf['storage_account_tags']),
                   project_conf['resource_group_name'], project_conf['region'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_storage_account', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create storage account.", str(err))
        AzureActions.remove_subnet(project_conf['resource_group_name'], project_conf['vpc_name'],
                                   project_conf['private_subnet_name'])
        if 'azure_edge_security_group_name' not in os.environ:
            AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['edge_security_group_name'])
        AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['notebook_security_group_name'])
        AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['master_security_group_name'])
        AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['slave_security_group_name'])
        for storage_account in AzureMeta.list_storage_accounts(project_conf['resource_group_name']):
            if project_conf['edge_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions.remove_storage_account(project_conf['resource_group_name'], storage_account.name)
            if project_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions.remove_storage_account(project_conf['resource_group_name'], storage_account.name)
        sys.exit(1)

    if os.environ['azure_datalake_enable'] == 'true':
        try:
            logging.info('[CREATE DATA LAKE STORE DIRECTORY]')
            logging.info('[CREATE DATA LAKE STORE DIRECTORY]')
            params = "--resource_group_name {} --datalake_name {} --directory_name {} --ad_user {} " \
                     "--service_base_name {}".format(project_conf['resource_group_name'],
                                                     project_conf['datalake_store_name'],
                                                     project_conf['datalake_user_directory_name'],
                                                     project_conf['azure_ad_user_name'],
                                                     project_conf['service_base_name'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('common_create_datalake_directory', params), shell=True, check=True)
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            datalab.fab.append_result("Failed to create Data Lake Store directory.", str(err))
            AzureActions.remove_subnet(project_conf['resource_group_name'], project_conf['vpc_name'],
                                       project_conf['private_subnet_name'])
            if 'azure_edge_security_group_name' not in os.environ:
                AzureActions.remove_security_group(project_conf['resource_group_name'],
                                               project_conf['edge_security_group_name'])
            AzureActions.remove_security_group(project_conf['resource_group_name'],
                                               project_conf['notebook_security_group_name'])
            AzureActions.remove_security_group(project_conf['resource_group_name'],
                                               project_conf['master_security_group_name'])
            AzureActions.remove_security_group(project_conf['resource_group_name'],
                                               project_conf['slave_security_group_name'])
            for storage_account in AzureMeta.list_storage_accounts(project_conf['resource_group_name']):
                if project_conf['edge_storage_account_name'] == storage_account.tags["Name"]:
                    AzureActions.remove_storage_account(project_conf['resource_group_name'], storage_account.name)
                if project_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                    AzureActions.remove_storage_account(project_conf['resource_group_name'], storage_account.name)
           # try:
           #    for datalake in AzureMeta.list_datalakes(project_conf['resource_group_name']):
           #         if project_conf['datalake_store_name'] == datalake.tags["Name"]:
           #             AzureActions.remove_datalake_directory(datalake.name,
           #                                                      project_conf['datalake_user_directory_name'])
           # except:
           #     logging.info("Data Lake Store directory hasn't been created.")
           # sys.exit(1)

    if os.environ['conf_os_family'] == 'debian':
        project_conf['initial_user'] = 'ubuntu'
        project_conf['sudo_group'] = 'sudo'
    if os.environ['conf_os_family'] == 'redhat':
        project_conf['initial_user'] = 'ec2-user'
        project_conf['sudo_group'] = 'wheel'

    try:
        logging.info('[CREATE EDGE INSTANCE]')
        if 'azure_edge_security_group_name' in os.environ:
            project_conf['edge_security_group_name'] = os.environ['azure_edge_security_group_name']
        params = "--instance_name {} --instance_size {} --region {} --vpc_name {} --network_interface_name {} \
            --security_group_name {} --subnet_name {} --service_base_name {} --resource_group_name {} \
            --datalab_ssh_user_name {} --public_ip_name {} --public_key '''{}''' --primary_disk_size {} \
            --instance_type {} --project_name {} --instance_storage_account_type {} --image_name {} --tags '{}'".\
            format(project_conf['instance_name'], os.environ['azure_edge_instance_size'], project_conf['region'],
                   project_conf['vpc_name'], project_conf['network_interface_name'],
                   project_conf['edge_security_group_name'], project_conf['subnet_name'],
                   project_conf['service_base_name'], project_conf['resource_group_name'],
                   project_conf['initial_user'], project_conf['static_public_ip_name'], project_conf['public_ssh_key'],
                   project_conf['primary_disk_size'], 'edge', project_conf['project_name'],
                   project_conf['instance_storage_account_type'],
                   project_conf['image_name'], json.dumps(project_conf['instance_tags']))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_instance', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        try:
            AzureActions.remove_instance(project_conf['resource_group_name'], project_conf['instance_name'])
        except:
            logging.info("The instance hasn't been created.")
        AzureActions.remove_subnet(project_conf['resource_group_name'], project_conf['vpc_name'],
                                   project_conf['private_subnet_name'])
        if 'azure_edge_security_group_name' not in os.environ:
            AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['edge_security_group_name'])
        AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['notebook_security_group_name'])
        AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['master_security_group_name'])
        AzureActions.remove_security_group(project_conf['resource_group_name'],
                                           project_conf['slave_security_group_name'])
        for storage_account in AzureMeta.list_storage_accounts(project_conf['resource_group_name']):
            if project_conf['edge_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions.remove_storage_account(project_conf['resource_group_name'], storage_account.name)
            if project_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions.remove_storage_account(project_conf['resource_group_name'], storage_account.name)
        if os.environ['azure_datalake_enable'] == 'true':
           for datalake in AzureMeta.list_datalakes(project_conf['resource_group_name']):
                if project_conf['datalake_store_name'] == datalake.tags["Name"]:
                    AzureActions.remove_datalake_directory(datalake.name,
                                                           project_conf['datalake_user_directory_name'])
        datalab.fab.append_result("Failed to create instance. Exception:" + str(err))
        sys.exit(1)
