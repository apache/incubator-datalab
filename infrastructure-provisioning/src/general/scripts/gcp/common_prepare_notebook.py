#!/usr/bin/python

# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ******************************************************************************

import logging
import json
import sys
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import os
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--uuid', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    notebook_config['edge_user_name'] = (os.environ['edge_user_name']).lower().replace('_', '-')
    notebook_config['region'] = os.environ['gcp_region']
    notebook_config['zone'] = os.environ['gcp_zone']

    edge_status = GCPMeta().get_instance_status('{0}-{1}-edge'.format(notebook_config['service_base_name'], notebook_config['edge_user_name']))
    if edge_status != 'RUNNING':
        logging.info('ERROR: Edge node is unavailable! Aborting...')
        print('ERROR: Edge node is unavailable! Aborting...')
        ssn_hostname = GCPMeta().get_private_ip_address(notebook_config['service_base_name'] + '-ssn')
        put_resource_status('edge', 'Unavailable', os.environ['ssn_dlab_path'], os.environ['conf_os_user'], ssn_hostname)
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
    notebook_config['subnet_name'] = '{0}-{1}-subnet'.format(notebook_config['service_base_name'], notebook_config['edge_user_name'])
    notebook_config['instance_size'] = os.environ['gcp_notebook_instance_size']
    notebook_config['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    notebook_config['notebook_service_account_name'] = notebook_config['service_base_name'].lower().replace('_',
                                                                                                            '-') + \
                                                       "-" + os.environ['edge_user_name'] + '-nb-sa'

    if os.environ['conf_os_family'] == 'debian':
        initial_user = 'ubuntu'
        sudo_group = 'sudo'
    if os.environ['conf_os_family'] == 'redhat':
        initial_user = 'ec2-user'
        sudo_group = 'wheel'
    notebook_config['instance_name'] = '{0}-{1}-nb-{2}-{3}'.format(notebook_config['service_base_name'], notebook_config['edge_user_name'],
                                                                   notebook_config['exploratory_name'], args.uuid)
    if os.environ['application'] == 'deeplearning':
        notebook_config['primary_disk_size'] = '30'
    else:
        notebook_config['primary_disk_size'] = '12'
    if os.environ['application'] == 'zeppelin':
        if os.environ['notebook_multiple_clusters'] == 'true':
            notebook_config['expected_ami_name'] = '{0}-{1}-{2}-livy-notebook-image'.format(notebook_config['service_base_name'], notebook_config['edge_user_name'], os.environ['application'])
        else:
            notebook_config['expected_ami_name'] = '{0}-{1}-{2}-spark-notebook-image'.format(notebook_config['service_base_name'], notebook_config['edge_user_name'], os.environ['application'])
    else:
        notebook_config['expected_ami_name'] = '{0}-{1}-{2}-notebook-image'.format(notebook_config['service_base_name'], notebook_config['edge_user_name'], os.environ['application'])
    # notebook_config['role_profile_name'] = '{0}-{1}-nb-Profile'.format(notebook_config['service_base_name'], notebook_config['edge_user_name'])
    # notebook_config['security_group_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
    #     'edge_user_name'] + "-nb-SG"
    # notebook_config['tag_name'] = notebook_config['service_base_name'] + '-Tag'
    #
    #print('Searching preconfigured images')
    #preconfigured_ami = GCPMeta().get_ami_by_name(notebook_config['expected_ami_name'])
    #if preconfigured_ami != '':
    #    print('Preconfigured image found. Using: {}'.format(preconfigured_ami))
    #    notebook_config['ami_name'] = preconfigured_ami
    #else:
    #    notebook_config['ami_name'] = os.environ['gcp_' + os.environ['conf_os_family'] + '_ami_name']
    #    print('No preconfigured image found. Using default one: {}'.format(notebook_config['ami_name']))
    notebook_config['secondary_disk_size'] = os.environ['notebook_disk_size']
    notebook_config['ami_name'] = os.environ['gcp_' + os.environ['conf_os_family'] + '_ami_name']
    notebook_config['gpu_accelerator_type'] = 'None'

    if os.environ['application'] in ('tensor', 'deeplearning'):
        notebook_config['gpu_accelerator_type'] = os.environ['gcp_gpu_accelerator_type']

    notebook_config['network_tag'] = '{0}-{1}-nb-de-des'.format(notebook_config['service_base_name'],
                                                                notebook_config['edge_user_name'])

    # launching instance for notebook server
    try:
        logging.info('[CREATE NOTEBOOK INSTANCE]')
        print('[CREATE NOTEBOOK INSTANCE]')
        params = "--instance_name {} --region {} --zone {} --vpc_name {} --subnet_name {} --instance_size {} --ssh_key_path {} --initial_user {} --service_account_name {} --ami_name {} --instance_class {} --primary_disk_size {} --secondary_disk_size {} --gpu_accelerator_type {}".\
            format(notebook_config['instance_name'], notebook_config['region'], notebook_config['zone'], notebook_config['vpc_name'],
                   notebook_config['subnet_name'], notebook_config['instance_size'], notebook_config['ssh_key_path'], initial_user,
                   notebook_config['notebook_service_account_name'], notebook_config['ami_name'], 'notebook',
                   notebook_config['primary_disk_size'], notebook_config['secondary_disk_size'], notebook_config['gpu_accelerator_type'])
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to create instance.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)