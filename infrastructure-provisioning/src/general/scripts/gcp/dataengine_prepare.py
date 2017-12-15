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

if __name__ == "__main__":
    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print('Generating infrastructure names and tags')
    data_engine = dict()
    data_engine['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    data_engine['edge_user_name'] = (os.environ['edge_user_name']).lower().replace('_', '-')
    data_engine['region'] = os.environ['gcp_region']
    data_engine['zone'] = os.environ['gcp_zone']

    edge_status = GCPMeta().get_instance_status('{0}-{1}-edge'.format(data_engine['service_base_name'],
                                                                      data_engine['edge_user_name']))
    if edge_status != 'RUNNING':
        logging.info('ERROR: Edge node is unavailable! Aborting...')
        print('ERROR: Edge node is unavailable! Aborting...')
        ssn_hostname = GCPMeta().get_private_ip_address(data_engine['service_base_name'] + '-ssn')
        put_resource_status('edge', 'Unavailable', os.environ['ssn_dlab_path'], os.environ['conf_os_user'],
                            ssn_hostname)
        append_result("Edge node is unavailable")
        sys.exit(1)

    try:
        if os.environ['gcp_vpc_name'] == '':
            raise KeyError
        else:
            data_engine['vpc_name'] = os.environ['gcp_vpc_name']
    except KeyError:
        data_engine['vpc_name'] = '{}-ssn-vpc'.format(data_engine['service_base_name'])
    try:
        data_engine['exploratory_name'] = (os.environ['exploratory_name']).lower().replace('_', '-')
    except:
        data_engine['exploratory_name'] = ''
    try:
        data_engine['computational_name'] = os.environ['computational_name'].replace('_', '-')
    except:
        data_engine['computational_name'] = ''

    data_engine['subnet_name'] = '{0}-{1}-subnet'.format(data_engine['service_base_name'],
                                                         data_engine['edge_user_name'])
    data_engine['master_size'] = os.environ['gcp_dataengine_master_size']
    data_engine['slave_size'] = os.environ['gcp_dataengine_slave_size']
    data_engine['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    data_engine['dataengine_service_account_name'] = '{}-{}-nb-de-des-sa'.format(data_engine['service_base_name'],
                                                                                 data_engine['edge_user_name']
                                                                                 ).replace('_', '-')

    if os.environ['conf_os_family'] == 'debian':
        initial_user = 'ubuntu'
        sudo_group = 'sudo'
    if os.environ['conf_os_family'] == 'redhat':
        initial_user = 'ec2-user'
        sudo_group = 'wheel'
    data_engine['cluster_name'] = data_engine['service_base_name'] + '-' + data_engine['edge_user_name'] + \
                                  '-de-' + data_engine['exploratory_name'] + '-' + \
                                  data_engine['computational_name']
    data_engine['master_node_name'] = data_engine['cluster_name'] + '-m'
    data_engine['slave_node_name'] = data_engine['cluster_name'] + '-s'
    data_engine['instance_count'] = int(os.environ['dataengine_instance_count'])
    data_engine['notebook_name'] = os.environ['notebook_instance_name']
    data_engine['ami_name'] = os.environ['gcp_' + os.environ['conf_os_family'] + '_ami_name']
    data_engine['gpu_accelerator_type'] = 'None'
    if os.environ['application'] in ('tensor', 'deeplearning'):
        data_engine['gpu_accelerator_type'] = os.environ['gcp_gpu_accelerator_type']
    data_engine['network_tag'] = '{0}-{1}-nb-de-des'.format(data_engine['service_base_name'],
                                                            data_engine['edge_user_name'])
    data_engine['slave_labels'] = {"Name": data_engine['cluster_name'],
                                   "SBN": data_engine['service_base_name'],
                                   "User": data_engine['user_name'],
                                   "Type": "slave",
                                   "notebook_name": data_engine['notebook_name']}
    data_engine['master_labels'] = {"Name": data_engine['cluster_name'],
                                    "SBN": data_engine['service_base_name'],
                                    "User": data_engine['user_name'],
                                    "Type": "master",
                                    "notebook_name": data_engine['notebook_name']}

    try:
        logging.info('[CREATE MASTER NODE]')
        print('[CREATE MASTER NODE]')
        params = "--instance_name {} --region {} --zone {} --vpc_name {} --subnet_name {} --instance_size {} --ssh_key_path {} --initial_user {} --service_account_name {} --ami_name {} --instance_class {} --primary_disk_size {} --gpu_accelerator_type {} --network_tag {} --labels '{}'".\
            format(data_engine['master_node_name'], data_engine['region'], data_engine['zone'], data_engine['vpc_name'],
                   data_engine['subnet_name'], data_engine['master_size'], data_engine['ssh_key_path'], initial_user,
                   data_engine['dataengine_service_account_name'], data_engine['ami_name'], 'dataengine', '30',
                   data_engine['gpu_accelerator_type'], data_engine['network_tag'], data_engine['master_labels'])
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to create instance.", str(err))
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        sys.exit(1)

    try:
        for i in range(data_engine['instance_count'] - 1):
            logging.info('[CREATE SLAVE NODE {}]'.format(i + 1))
            print('[CREATE SLAVE NODE {}]'.format(i + 1))
            slave_name = data_engine['slave_node_name'] + '{}'.format(i + 1)
            params = "--instance_name {} --region {} --zone {} --vpc_name {} --subnet_name {} --instance_size {} --ssh_key_path {} --initial_user {} --service_account_name {} --ami_name {} --instance_class {} --primary_disk_size {} --gpu_accelerator_type {} --network_tag {} --labels '{}'". \
                format(slave_name, data_engine['region'], data_engine['zone'],
                       data_engine['vpc_name'], data_engine['subnet_name'], data_engine['slave_size'],
                       data_engine['ssh_key_path'], initial_user, data_engine['dataengine_service_account_name'],
                       data_engine['ami_name'], 'dataengine', '30', data_engine['gpu_accelerator_type'],
                       data_engine['network_tag'], data_engine['slave_labels'])
            try:
                local("~/scripts/{}.py {}".format('common_create_instance', params))
            except:
                traceback.print_exc()
                raise Exception
    except Exception as err:
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            try:
                GCPActions().remove_instance(slave_name, data_engine['zone'])
            except:
                print("The slave instance {} hasn't been created.".format(slave_name))
        GCPActions().remove_instance(data_engine['master_node_name'], data_engine['zone'])
        append_result("Failed to create slave instances.", str(err))
        sys.exit(1)
