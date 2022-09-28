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
        AzureMeta = datalab.meta_lib.AzureMeta()
        AzureActions = datalab.actions_lib.AzureActions()
        data_engine = dict()
        data_engine['user_name'] = os.environ['edge_user_name']
        data_engine['project_name'] = os.environ['project_name']
        data_engine['endpoint_name'] = os.environ['endpoint_name']
        data_engine['project_tag'] = data_engine['project_name']
        data_engine['endpoint_tag'] = data_engine['endpoint_name']
        logging.info('Generating infrastructure names and tags')
        if 'exploratory_name' in os.environ:
            data_engine['exploratory_name'] = os.environ['exploratory_name']
        else:
            data_engine['exploratory_name'] = ''
        if 'computational_name' in os.environ:
            data_engine['computational_name'] = os.environ['computational_name']
        else:
            data_engine['computational_name'] = ''
        data_engine['service_base_name'] = os.environ['conf_service_base_name']
        data_engine['resource_group_name'] = os.environ['azure_resource_group_name']
        data_engine['region'] = os.environ['azure_region']
        data_engine['key_name'] = os.environ['conf_key_name']
        data_engine['vpc_name'] = os.environ['azure_vpc_name']
        data_engine['private_subnet_name'] = '{}-{}-{}-subnet'.format(data_engine['service_base_name'],
                                                                      data_engine['project_name'],
                                                                      data_engine['endpoint_name'])
        data_engine['private_subnet_cidr'] = AzureMeta.get_subnet(data_engine['resource_group_name'],
                                                                  data_engine['vpc_name'],
                                                                  data_engine['private_subnet_name']).address_prefix
        data_engine['master_security_group_name'] = '{}-{}-{}-de-master-sg'.format(
            data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_name'])
        data_engine['slave_security_group_name'] = '{}-{}-{}-de-slave-sg'.format(
            data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_name'])
        data_engine['cluster_name'] = '{}-{}-{}-de-{}'.format(data_engine['service_base_name'],
                                                              data_engine['project_name'],
                                                              data_engine['endpoint_name'],
                                                              data_engine['computational_name'])
        data_engine['master_node_name'] = '{}-m'.format(data_engine['cluster_name'])
        data_engine['slave_node_name'] = '{}-s'.format(data_engine['cluster_name'])
        data_engine['master_network_interface_name'] = '{}-nif'.format(data_engine['master_node_name'])
        data_engine['master_size'] = os.environ['azure_dataengine_master_size']
        key = RSA.importKey(open('{}{}.pem'.format(os.environ['conf_key_dir'],
                                                   os.environ['conf_key_name']), 'rb').read())
        data_engine['public_ssh_key'] = key.publickey().exportKey("OpenSSH").decode('UTF-8')
        data_engine['instance_count'] = int(os.environ['dataengine_instance_count'])
        data_engine['slave_size'] = os.environ['azure_dataengine_slave_size']
        data_engine['instance_storage_account_type'] = 'Premium_LRS'
        data_engine['notebook_name'] = os.environ['notebook_instance_name']
        data_engine['slave_tags'] = {"Name": data_engine['cluster_name'],
                                     "SBN": data_engine['service_base_name'],
                                     "User": data_engine['user_name'],
                                     "project_tag": data_engine['project_tag'],
                                     "endpoint_tag": data_engine['endpoint_tag'],
                                     "Type": "slave",
                                     "notebook_name": data_engine['notebook_name'],
                                     os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value']}
        data_engine['master_tags'] = {"Name": data_engine['cluster_name'],
                                      "SBN": data_engine['service_base_name'],
                                      "User": data_engine['user_name'],
                                      "project_tag": data_engine['project_tag'],
                                      "endpoint_tag": data_engine['endpoint_tag'],
                                      "Type": "master",
                                      "notebook_name": data_engine['notebook_name'],
                                      os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value']}
        data_engine['custom_tag'] = ''
        if 'custom_tag' in os.environ['tags']:
            data_engine['custom_tag'] = json.loads(os.environ['tags'].replace("'", '"'))['custom_tag']
        if data_engine['custom_tag']:
            data_engine['slave_tags']['custom_tag'] = data_engine['custom_tag']
            data_engine['master_tags']['custom_tag'] = data_engine['custom_tag']
        # data_engine['primary_disk_size'] = '32'
        if os.environ['conf_deeplearning_cloud_ami'] == 'true' and os.environ['application'] == 'deeplearning':
            data_engine['primary_disk_size'] = '150'
        else:
            data_engine['primary_disk_size'] = '32'

        data_engine['image_type'] = 'default'

        if os.environ['conf_shared_image_enabled'] == 'false':
            data_engine['expected_image_name'] = '{0}-{1}-{2}-{3}-notebook-image'.format(
                data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_name'],
                os.environ['application'])
        else:
            data_engine['expected_image_name'] = '{0}-{1}-{2}-notebook-image'.format(data_engine['service_base_name'],
                                                                                     data_engine['endpoint_name'],
                                                                                     os.environ['application'])

        data_engine['notebook_image_name'] = (lambda x: os.environ['notebook_image_name'] if x != 'None'
                    else data_engine['expected_image_name'])(str(os.environ.get('notebook_image_name')))

        logging.info('Searching pre-configured images')
        if AzureMeta.get_image(data_engine['resource_group_name'], data_engine['notebook_image_name']) and \
                        os.environ['application'] in os.environ['dataengine_image_notebooks'].split(','):
            data_engine['image_name'] = data_engine['notebook_image_name']
            data_engine['image_type'] = 'pre-configured'
            logging.info('Pre-configured image found. Using: {}'.format(data_engine['notebook_image_name']))
        else:
            data_engine['image_name'] = os.environ['azure_{}_image_name'.format(os.environ['conf_os_family'])]
            logging.info('No pre-configured image found. Using default one: {}'.format(data_engine['image_name']))
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary", str(err))
        sys.exit(1)

    try:
        edge_status = AzureMeta.get_instance_status(data_engine['resource_group_name'],
                                                    '{0}-{1}-{2}-edge'.format(data_engine['service_base_name'],
                                                                              data_engine['project_name'],
                                                                              data_engine['endpoint_name']))
        if edge_status != 'running':
            logging.info('ERROR: Edge node is unavailable! Aborting...')
            ssn_hostname = AzureMeta.get_private_ip_address(data_engine['resource_group_name'],
                                                            data_engine['service_base_name'] + '-ssn')
            datalab.fab.put_resource_status('edge', 'Unavailable', os.environ['ssn_datalab_path'],
                                            os.environ['conf_os_user'],
                                            ssn_hostname)
            datalab.fab.append_result("Edge node is unavailable")
            sys.exit(1)
    except Exception as err:
        datalab.fab.append_result("Failed to verify edge status.", str(err))
        sys.exit(1)

    if os.environ['conf_os_family'] == 'debian':
        initial_user = 'ubuntu'
        sudo_group = 'sudo'
    if os.environ['conf_os_family'] == 'redhat':
        initial_user = 'ec2-user'
        sudo_group = 'wheel'

    try:
        logging.info('[CREATE MASTER NODE]')

        if 'NC' in data_engine['master_size']:
            data_engine['instance_storage_account_type'] = 'Standard_LRS'

        params = "--instance_name {} --instance_size {} --region {} --vpc_name {} --network_interface_name {} \
            --security_group_name {} --subnet_name {} --service_base_name {} --resource_group_name {} \
            --datalab_ssh_user_name {} --public_ip_name {} --public_key '''{}''' --primary_disk_size {} \
            --instance_type {} --project_name {} --instance_storage_account_type {} --image_name {} \
            --image_type {} --tags '{}'". \
            format(data_engine['master_node_name'], data_engine['master_size'], data_engine['region'],
                   data_engine['vpc_name'], data_engine['master_network_interface_name'],
                   data_engine['master_security_group_name'], data_engine['private_subnet_name'],
                   data_engine['service_base_name'], data_engine['resource_group_name'], initial_user, 'None',
                   data_engine['public_ssh_key'], data_engine['primary_disk_size'], 'dataengine',
                   data_engine['project_name'], data_engine['instance_storage_account_type'],
                   data_engine['image_name'], data_engine['image_type'], json.dumps(data_engine['master_tags']))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_instance', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        try:
            AzureActions.remove_instance(data_engine['resource_group_name'], data_engine['master_node_name'])
        except:
            logging.info("The instance hasn't been created.")
        datalab.fab.append_result("Failed to create master instance.", str(err))
        sys.exit(1)

    try:
        for i in range(data_engine['instance_count'] - 1):
            logging.info('[CREATE SLAVE NODE {}]'.format(i + 1))

            slave_name = data_engine['slave_node_name'] + '{}'.format(i + 1)
            slave_nif_name = slave_name + '-nif'
            if 'NC' in data_engine['slave_size']:
                data_engine['instance_storage_account_type'] = 'Standard_LRS'

            params = "--instance_name {} --instance_size {} --region {} --vpc_name {} --network_interface_name {} \
                --security_group_name {} --subnet_name {} --service_base_name {} --resource_group_name {} \
                --datalab_ssh_user_name {} --public_ip_name {} --public_key '''{}''' --primary_disk_size {} \
                --instance_type {} --project_name {} --instance_storage_account_type {} --image_name {} \
                --image_type {} --tags '{}'". \
                format(slave_name, data_engine['slave_size'], data_engine['region'], data_engine['vpc_name'],
                       slave_nif_name, data_engine['slave_security_group_name'], data_engine['private_subnet_name'],
                       data_engine['service_base_name'], data_engine['resource_group_name'], initial_user, 'None',
                       data_engine['public_ssh_key'], data_engine['primary_disk_size'], 'dataengine',
                       data_engine['project_name'], data_engine['instance_storage_account_type'],
                       data_engine['image_name'], data_engine['image_type'], json.dumps(data_engine['slave_tags']))
            try:
                subprocess.run("~/scripts/{}.py {}".format('common_create_instance', params), shell=True, check=True)
            except:
                traceback.print_exc()
                raise Exception
    except Exception as err:
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            try:
                AzureActions.remove_instance(data_engine['resource_group_name'], slave_name)
            except:
                logging.info("The slave instance {} hasn't been created.".format(slave_name))
        AzureActions.remove_instance(data_engine['resource_group_name'], data_engine['master_node_name'])
        datalab.fab.append_result("Failed to create slave instances.", str(err))
        sys.exit(1)
