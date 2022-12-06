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
    # generating variables dictionary
    try:
        AzureMeta = datalab.meta_lib.AzureMeta()
        AzureActions = datalab.actions_lib.AzureActions()
        notebook_config = dict()
        notebook_config['user_name'] = os.environ['edge_user_name']
        notebook_config['project_name'] = os.environ['project_name']
        notebook_config['project_tag'] = notebook_config['project_name']
        notebook_config['endpoint_name'] = os.environ['endpoint_name']
        notebook_config['endpoint_tag'] = notebook_config['endpoint_name']
        notebook_config['application'] = os.environ['application'].lower()

        logging.info('Generating infrastructure names and tags')
        try:
            notebook_config['exploratory_name'] = os.environ['exploratory_name']
        except:
            notebook_config['exploratory_name'] = ''
        notebook_config['service_base_name'] = os.environ['conf_service_base_name']
        notebook_config['resource_group_name'] = os.environ['azure_resource_group_name']
        notebook_config['region'] = os.environ['azure_region']
        notebook_config['vpc_name'] = os.environ['azure_vpc_name']
        notebook_config['instance_size'] = os.environ['azure_notebook_instance_size']
        notebook_config['key_name'] = os.environ['conf_key_name']
        notebook_config['instance_name'] = '{}-{}-{}-nb-{}'.format(notebook_config['service_base_name'],
                                                                   notebook_config['project_name'],
                                                                   notebook_config['endpoint_name'],
                                                                   notebook_config['exploratory_name'])
        notebook_config['tags'] = {"Name": notebook_config['instance_name'],
                                   "SBN": notebook_config['service_base_name'],
                                   "User": notebook_config['user_name'],
                                   "project_tag": notebook_config['project_tag'],
                                   "endpoint_tag": notebook_config['endpoint_tag'],
                                   "Exploratory": notebook_config['exploratory_name'],
                                   "product": "datalab"}
        notebook_config['custom_tag'] = ''
        if 'custom_tag' in os.environ['tags']:
            notebook_config['custom_tag'] = json.loads(os.environ['tags'].replace("'", '"'))['custom_tag']
            if notebook_config['custom_tag']:
                notebook_config['tags']['custom_tag'] = notebook_config['custom_tag']
        notebook_config['network_interface_name'] = notebook_config['instance_name'] + "-nif"
        notebook_config['security_group_name'] = '{}-{}-{}-nb-sg'.format(notebook_config['service_base_name'],
                                                                         notebook_config['project_name'],
                                                                         notebook_config['endpoint_name'])
        notebook_config['private_subnet_name'] = '{}-{}-{}-subnet'.format(notebook_config['service_base_name'],
                                                                          notebook_config['project_name'],
                                                                          notebook_config['endpoint_name'])
        ssh_key_path = '{}{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        key = RSA.importKey(open(ssh_key_path, 'rb').read())
        notebook_config['public_ssh_key'] = key.publickey().exportKey("OpenSSH").decode('UTF-8')
        if os.environ['conf_deeplearning_cloud_ami'] == 'true' and os.environ['application'] == 'deeplearning':
            notebook_config['primary_disk_size'] = '150'
        else:
            notebook_config['primary_disk_size'] = '32'
        notebook_config['instance_storage_account_type'] = (lambda x: 'Standard_LRS' if x in ('deeplearning', 'tensor')
                                                            else 'Premium_LRS')(os.environ['application'])
        if os.environ['conf_os_family'] == 'debian':
            notebook_config['initial_user'] = 'ubuntu'
            notebook_config['sudo_group'] = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            notebook_config['initial_user'] = 'ec2-user'
            notebook_config['sudo_group'] = 'wheel'
        notebook_config['image_type'] = 'default'

        notebook_config['shared_image_enabled'] = os.environ['conf_shared_image_enabled']
        if notebook_config['shared_image_enabled'] == 'false':
            notebook_config['expected_image_name'] = '{0}-{1}-{2}-{3}-notebook-image'.format(
                notebook_config['service_base_name'],
                notebook_config['project_name'],
                notebook_config['endpoint_name'],
                notebook_config['application'])
        else:
            notebook_config['expected_image_name'] = '{0}-{1}-{2}-notebook-image'.format(
                notebook_config['service_base_name'],
                notebook_config['endpoint_name'],
                notebook_config['application'])

        logging.info('Searching pre-configured images')
        notebook_config['image_name'] = os.environ['azure_{}_image_name'.format(os.environ['conf_os_family'])]
        if os.environ['conf_deeplearning_cloud_ami'] == 'true' and os.environ['application'] == 'deeplearning':
            if AzureMeta.get_image(notebook_config['resource_group_name'], notebook_config['expected_image_name']):
                notebook_config['image_name'] = notebook_config['expected_image_name']
                notebook_config['image_type'] = 'pre-configured'
                logging.info('Pre-configured image found. Using: {}'.format(notebook_config['image_name']))
            else:
                notebook_config['image_name'] = os.environ['notebook_image_name']
                logging.info('Pre-configured deeplearning image found. Using: {}'.format(notebook_config['image_name']))
        else:
            notebook_config['notebook_image_name'] = (lambda x: '{0}-{1}-{2}-{3}-{4}'.format(
                notebook_config['service_base_name'], notebook_config['project_name'], notebook_config['endpoint_name'],
                os.environ['application'], os.environ['notebook_image_name']).replace('_', '-') if (x != 'None' and x != '')
            else notebook_config['expected_image_name'])(str(os.environ.get('notebook_image_name')))
            if AzureMeta.get_image(notebook_config['resource_group_name'], notebook_config['notebook_image_name']):
                notebook_config['image_name'] = notebook_config['notebook_image_name']
                notebook_config['image_type'] = 'pre-configured'
                logging.info('Pre-configured image found. Using: {}'.format(notebook_config['notebook_image_name']))
            else:
                os.environ['notebook_image_name'] = notebook_config['image_name']
                logging.info('No pre-configured image found. Using default one: {}'.format(notebook_config['image_name']))
    except Exception as err:
        logging.error("Failed to generate variables dictionary.")
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        sys.exit(1)

    try:
        edge_status = AzureMeta.get_instance_status(notebook_config['resource_group_name'],
                                                    '{0}-{1}-{2}-edge'.format(os.environ['conf_service_base_name'],
                                                                              notebook_config['project_name'],
                                                                              notebook_config['endpoint_name']))

        if edge_status != 'running':
            logging.info('ERROR: Edge node is unavailable! Aborting...')
            ssn_hostname = AzureMeta.get_private_ip_address(notebook_config['resource_group_name'],
                                                              os.environ['conf_service_base_name'] + '-ssn')
            datalab.fab.put_resource_status('edge', 'Unavailable', os.environ['ssn_datalab_path'],
                                            os.environ['conf_os_user'],
                                            ssn_hostname)
            datalab.fab.append_result("Edge node is unavailable")
            sys.exit(1)
    except Exception as err:
        datalab.fab.append_result("Failed to verify edge status.", str(err))
        sys.exit(1)

    with open('/root/result.json', 'w') as f:
        data = {"notebook_name": notebook_config['instance_name'], "error": ""}
        json.dump(data, f)

    # launching instance for notebook server
    try:
        logging.info('[CREATE NOTEBOOK INSTANCE]')
        params = "--instance_name {} --instance_size {} --region {} --vpc_name {} --network_interface_name {} \
            --security_group_name {} --subnet_name {} --service_base_name {} --resource_group_name {} \
            --datalab_ssh_user_name {} --public_ip_name {} --public_key '''{}''' --primary_disk_size {} \
            --instance_type {} --project_name {} --instance_storage_account_type {} --image_name {} \
            --image_type {} --tags '{}'". \
            format(notebook_config['instance_name'], notebook_config['instance_size'], notebook_config['region'],
                   notebook_config['vpc_name'], notebook_config['network_interface_name'],
                   notebook_config['security_group_name'], notebook_config['private_subnet_name'],
                   notebook_config['service_base_name'], notebook_config['resource_group_name'],
                   notebook_config['initial_user'], 'None', notebook_config['public_ssh_key'],
                   notebook_config['primary_disk_size'], 'notebook', notebook_config['project_name'],
                   notebook_config['instance_storage_account_type'], notebook_config['image_name'],
                   notebook_config['image_type'], json.dumps(notebook_config['tags']))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_instance', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        try:
            AzureActions.remove_instance(notebook_config['resource_group_name'], notebook_config['instance_name'])
        except:
            logging.error("The instance hasn't been created.")
        datalab.fab.append_result("Failed to create instance.", str(err))
        sys.exit(1)
