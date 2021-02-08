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
import logging
import os
import sys
import traceback
from fabric import *

if __name__ == "__main__":
    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    try:
        GCPMeta = datalab.meta_lib.GCPMeta()
        GCPActions = datalab.actions_lib.GCPActions()
        print('Generating infrastructure names and tags')
        notebook_config = dict()
        notebook_config['service_base_name'] = (os.environ['conf_service_base_name'])
        notebook_config['edge_user_name'] = (os.environ['edge_user_name'])
        notebook_config['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
        notebook_config['project_tag'] = notebook_config['project_name']
        notebook_config['endpoint_name'] = os.environ['endpoint_name'].replace('_', '-').lower()
        notebook_config['endpoint_tag'] = notebook_config['endpoint_name']
        notebook_config['region'] = os.environ['gcp_region']
        notebook_config['zone'] = os.environ['gcp_zone']

        edge_status = GCPMeta.get_instance_status('{0}-{1}-{2}-edge'.format(notebook_config['service_base_name'],
                                                                            notebook_config['project_name'],
                                                                            notebook_config['endpoint_tag']))
        if edge_status != 'RUNNING':
            logging.info('ERROR: Edge node is unavailable! Aborting...')
            print('ERROR: Edge node is unavailable! Aborting...')
            ssn_hostname = GCPMeta.get_private_ip_address(notebook_config['service_base_name'] + '-ssn')
            datalab.fab.put_resource_status('edge', 'Unavailable', os.environ['ssn_datalab_path'],
                                            os.environ['conf_os_user'],
                                            ssn_hostname)
            datalab.fab.append_result("Edge node is unavailable")
            sys.exit(1)

        try:
            if os.environ['gcp_vpc_name'] == '':
                raise KeyError
            else:
                notebook_config['vpc_name'] = os.environ['gcp_vpc_name']
        except KeyError:
            notebook_config['vpc_name'] = '{}-vpc'.format(notebook_config['service_base_name'])
        try:
            notebook_config['exploratory_name'] = (os.environ['exploratory_name']).replace('_', '-').lower()
        except:
            notebook_config['exploratory_name'] = ''
        notebook_config['subnet_name'] = '{0}-{1}-{2}-subnet'.format(notebook_config['service_base_name'],
                                                                     notebook_config['project_name'],
                                                                     notebook_config['endpoint_tag'])
        notebook_config['instance_size'] = os.environ['gcp_notebook_instance_size']
        notebook_config['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        notebook_config['notebook_service_account_name'] = '{}-{}-{}-ps-sa'.format(notebook_config['service_base_name'],
                                                                                   notebook_config['project_name'],
                                                                                   notebook_config['endpoint_name'])

        if os.environ['conf_os_family'] == 'debian':
            notebook_config['initial_user'] = 'ubuntu'
            notebook_config['sudo_group'] = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            notebook_config['initial_user'] = 'ec2-user'
            notebook_config['sudo_group'] = 'wheel'
        notebook_config['instance_name'] = '{0}-{1}-{2}-nb-{3}'.format(notebook_config['service_base_name'],
                                                                       notebook_config['project_name'],
                                                                       notebook_config['endpoint_name'],
                                                                       notebook_config['exploratory_name'])
        notebook_config['primary_disk_size'] = (lambda x: '30' if x == 'deeplearning' else '16')(
            os.environ['application'])
        notebook_config['secondary_disk_size'] = os.environ['notebook_disk_size']

        notebook_config['shared_image_enabled'] = os.environ['conf_shared_image_enabled']
        if notebook_config['shared_image_enabled'] == 'false':
            notebook_config['expected_primary_image_name'] = '{}-{}-{}-{}-primary-image'.format(
                notebook_config['service_base_name'], notebook_config['project_name'], notebook_config['endpoint_tag'],
                os.environ['application']).lower()
            notebook_config['expected_secondary_image_name'] = '{}-{}-{}-{}-secondary-image'.format(
                notebook_config['service_base_name'], notebook_config['project_name'], notebook_config['endpoint_tag'],
                os.environ['application']).lower()
        else:
            notebook_config['expected_primary_image_name'] = '{}-{}-{}-primary-image'.format(
                notebook_config['service_base_name'], notebook_config['endpoint_name'], os.environ['application']).lower()
            notebook_config['expected_secondary_image_name'] = '{}-{}-{}-secondary-image'.format(
                notebook_config['service_base_name'], notebook_config['endpoint_name'], os.environ['application']).lower()
        notebook_config['notebook_primary_image_name'] = (lambda x: '{0}-{1}-{2}-{3}-primary-image-{4}'.format(
            notebook_config['service_base_name'], notebook_config['project_name'], notebook_config['endpoint_name'],
            os.environ['application'], os.environ['notebook_image_name'].replace('_', '-').lower()) if (x != 'None' and x != '')
            else notebook_config['expected_primary_image_name'])(str(os.environ.get('notebook_image_name')))
        print('Searching pre-configured images')
        notebook_config['primary_image_name'] = GCPMeta.get_image_by_name(
            notebook_config['notebook_primary_image_name'])
        if notebook_config['primary_image_name'] == '':
            notebook_config['primary_image_name'] = os.environ['gcp_{}_image_name'.format(os.environ['conf_os_family'])]
        else:
            print('Pre-configured primary image found. Using: {}'.format(
                notebook_config['primary_image_name'].get('name')))
            notebook_config['primary_image_name'] = 'global/images/{}'.format(
                notebook_config['primary_image_name'].get('name'))
        notebook_config['notebook_secondary_image_name'] = (lambda x: '{0}-{1}-{2}-{3}-secondary-image-{4}'.format(
            notebook_config['service_base_name'], notebook_config['project_name'], notebook_config['endpoint_name'],
            os.environ['application'], os.environ['notebook_image_name'].replace('_', '-').lower()) if (x != 'None' and x != '')
            else notebook_config['expected_secondary_image_name'])(str(os.environ.get('notebook_image_name')))
        notebook_config['secondary_image_name'] = GCPMeta.get_image_by_name(
            notebook_config['notebook_secondary_image_name'])
        if notebook_config['secondary_image_name'] == '':
            notebook_config['secondary_image_name'] = 'None'
        else:
            print('Pre-configured secondary image found. Using: {}'.format(
                notebook_config['secondary_image_name'].get('name')))
            notebook_config['secondary_image_name'] = 'global/images/{}'.format(
                notebook_config['secondary_image_name'].get('name'))

        notebook_config['gpu_accelerator_type'] = 'None'

        if os.environ['application'] in ('tensor', 'tensor-rstudio', 'deeplearning'):
            notebook_config['gpu_accelerator_type'] = os.environ['gcp_gpu_accelerator_type']

        notebook_config['network_tag'] = '{0}-{1}-{2}-ps'.format(notebook_config['service_base_name'],
                                                                 notebook_config['project_name'],
                                                                 notebook_config['endpoint_name'])

        with open('/root/result.json', 'w') as f:
            data = {"notebook_name": notebook_config['instance_name'], "error": ""}
            json.dump(data, f)

        print('Additional tags will be added: {}'.format(os.environ['tags']))
        additional_tags = os.environ['tags'].replace("': '", ":").replace("', '", ",").replace("{'", "" ).replace(
            "'}", "").lower()

        print('Additional tags will be added: {}'.format(additional_tags))
        notebook_config['labels'] = {"name": notebook_config['instance_name'],
                                     "sbn": notebook_config['service_base_name'],
                                     "product": "datalab"
                                     }

        for tag in additional_tags.split(','):
            label_key = tag.split(':')[0]
            label_value = tag.split(':')[1].replace('_', '-')
            if '@' in label_value:
                label_value = label_value[:label_value.find('@')]
            if label_value != '':
                notebook_config['labels'].update({label_key: label_value})
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        sys.exit(1)
    # launching instance for notebook server
    try:
        logging.info('[CREATE NOTEBOOK INSTANCE]')
        print('[CREATE NOTEBOOK INSTANCE]')
        params = "--instance_name {0} --region {1} --zone {2} --vpc_name {3} --subnet_name {4} --instance_size {5} " \
                 "--ssh_key_path {6} --initial_user {7} --service_account_name {8} --image_name {9} " \
                 "--secondary_image_name {10} --instance_class {11} --primary_disk_size {12} " \
                 "--secondary_disk_size {13} --gpu_accelerator_type {14} --network_tag {15} --labels '{16}' " \
                 "--service_base_name {17}".\
            format(notebook_config['instance_name'], notebook_config['region'], notebook_config['zone'],
                   notebook_config['vpc_name'], notebook_config['subnet_name'], notebook_config['instance_size'],
                   notebook_config['ssh_key_path'], notebook_config['initial_user'],
                   notebook_config['notebook_service_account_name'], notebook_config['primary_image_name'],
                   notebook_config['secondary_image_name'], 'notebook', notebook_config['primary_disk_size'],
                   notebook_config['secondary_disk_size'], notebook_config['gpu_accelerator_type'],
                   notebook_config['network_tag'], json.dumps(notebook_config['labels']),
                   notebook_config['service_base_name'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_instance', params), shell=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create instance.", str(err))
        GCPActions.remove_disk(notebook_config['instance_name'], notebook_config['zone'])
        GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)
