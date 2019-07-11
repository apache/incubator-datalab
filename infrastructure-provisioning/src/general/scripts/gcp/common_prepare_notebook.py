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

import logging
import json
import sys
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import os

if __name__ == "__main__":
    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    notebook_config['edge_user_name'] = (os.environ['edge_user_name']).lower().replace('_', '-')
    notebook_config['project_name'] = (os.environ['project_name']).lower().replace('_', '-')
    notebook_config['project_tag'] = (os.environ['project_tag']).lower().replace('_', '-')
    notebook_config['region'] = os.environ['gcp_region']
    notebook_config['zone'] = os.environ['gcp_zone']

    edge_status = GCPMeta().get_instance_status('{0}-{1}-edge'.format(notebook_config['service_base_name'],
                                                                      notebook_config['project_name']))
    if edge_status != 'RUNNING':
        logging.info('ERROR: Edge node is unavailable! Aborting...')
        print('ERROR: Edge node is unavailable! Aborting...')
        ssn_hostname = GCPMeta().get_private_ip_address(notebook_config['service_base_name'] + '-ssn')
        put_resource_status('edge', 'Unavailable', os.environ['ssn_dlab_path'], os.environ['conf_os_user'],
                            ssn_hostname)
        append_result("Edge node is unavailable")
        sys.exit(1)

    try:
        if os.environ['gcp_vpc_name'] == '':
            raise KeyError
        else:
            notebook_config['vpc_name'] = os.environ['gcp_vpc_name']
    except KeyError:
        notebook_config['vpc_name'] = '{}-ssn-vpc'.format(notebook_config['service_base_name'])
    try:
        notebook_config['exploratory_name'] = (os.environ['exploratory_name']).lower().replace('_', '-')
    except:
        notebook_config['exploratory_name'] = ''
    notebook_config['subnet_name'] = '{0}-{1}-subnet'.format(notebook_config['service_base_name'],
                                                             notebook_config['project_name'])
    notebook_config['instance_size'] = os.environ['gcp_notebook_instance_size']
    notebook_config['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    notebook_config['notebook_service_account_name'] = '{}-{}-ps'.format(notebook_config['service_base_name'],
                                                                         notebook_config['project_name']).replace('_', '-')

    if os.environ['conf_os_family'] == 'debian':
        initial_user = 'ubuntu'
        sudo_group = 'sudo'
    if os.environ['conf_os_family'] == 'redhat':
        initial_user = 'ec2-user'
        sudo_group = 'wheel'
    notebook_config['instance_name'] = '{0}-{1}-nb-{2}'.format(notebook_config['service_base_name'],
                                                               notebook_config['project_name'],
                                                               notebook_config['exploratory_name'])
    notebook_config['primary_disk_size'] = (lambda x: '30' if x == 'deeplearning' else '12')(os.environ['application'])
    notebook_config['secondary_disk_size'] = os.environ['notebook_disk_size']


    notebook_config['expected_primary_image_name'] = '{}-{}-notebook-primary-image'.format(
        notebook_config['service_base_name'], os.environ['application'])
    notebook_config['expected_secondary_image_name'] = '{}-{}-notebook-secondary-image'.format(
        notebook_config['service_base_name'], os.environ['application'])
    notebook_config['notebook_primary_image_name'] = (lambda x: os.environ['notebook_primary_image_name'] if x != 'None'
        else notebook_config['expected_primary_image_name'])(str(os.environ.get('notebook_primary_image_name')))
    print('Searching pre-configured images')
    notebook_config['primary_image_name'] = GCPMeta().get_image_by_name(notebook_config['expected_primary_image_name'])
    if notebook_config['primary_image_name'] == '':
        notebook_config['primary_image_name'] = os.environ['gcp_{}_image_name'.format(os.environ['conf_os_family'])]
    else:
        print('Pre-configured primary image found. Using: {}'.format(notebook_config['primary_image_name'].get('name')))
        notebook_config['primary_image_name'] = 'global/images/{}'.format(notebook_config['primary_image_name'].get('name'))

    notebook_config['secondary_image_name'] = GCPMeta().get_image_by_name(notebook_config['expected_secondary_image_name'])
    if notebook_config['secondary_image_name'] == '':
        notebook_config['secondary_image_name'] = 'None'
    else:
        print('Pre-configured secondary image found. Using: {}'.format(notebook_config['secondary_image_name'].get('name')))
        notebook_config['secondary_image_name'] = 'global/images/{}'.format(notebook_config['secondary_image_name'].get('name'))

    notebook_config['gpu_accelerator_type'] = 'None'

    if os.environ['application'] in ('tensor', 'tensor-rstudio', 'deeplearning'):
        notebook_config['gpu_accelerator_type'] = os.environ['gcp_gpu_accelerator_type']

    notebook_config['network_tag'] = '{0}-{1}-ps'.format(notebook_config['service_base_name'],
                                                         notebook_config['project_name'])
    notebook_config['labels'] = {"name": notebook_config['instance_name'],
                                 "sbn": notebook_config['service_base_name'],
                                 "project_name": notebook_config['project_name'],
                                 "project_tag": notebook_config['project_tag'],
                                 "user": notebook_config['edge_user_name'],
                                 "product": "dlab"}
    # launching instance for notebook server
    try:
        logging.info('[CREATE NOTEBOOK INSTANCE]')
        print('[CREATE NOTEBOOK INSTANCE]')
        params = "--instance_name {0} --region {1} --zone {2} --vpc_name {3} --subnet_name {4} --instance_size {5} " \
                 "--ssh_key_path {6} --initial_user {7} --service_account_name {8} --image_name {9} " \
                 "--secondary_image_name {10} --instance_class {11} --primary_disk_size {12} --secondary_disk_size {13} " \
                 "--gpu_accelerator_type {14} --network_tag {15} --labels '{16}'".\
            format(notebook_config['instance_name'], notebook_config['region'], notebook_config['zone'],
                   notebook_config['vpc_name'], notebook_config['subnet_name'], notebook_config['instance_size'],
                   notebook_config['ssh_key_path'], initial_user, notebook_config['notebook_service_account_name'],
                   notebook_config['primary_image_name'], notebook_config['secondary_image_name'], 'notebook',
                   notebook_config['primary_disk_size'], notebook_config['secondary_disk_size'],
                   notebook_config['gpu_accelerator_type'], notebook_config['network_tag'],
                   json.dumps(notebook_config['labels']))
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create instance.", str(err))
        GCPActions().remove_disk(notebook_config['instance_name'], notebook_config['zone'])
        GCPActions().remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)