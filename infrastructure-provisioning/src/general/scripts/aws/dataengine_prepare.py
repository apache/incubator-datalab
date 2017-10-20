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

import json
import time
from fabric.api import *
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import sys
import os
import uuid
import logging
from Crypto.PublicKey import RSA
import multiprocessing


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.INFO,
                        filename=local_log_filepath)
    try:
        create_aws_config_files()
        edge_status = get_instance_status(os.environ['conf_service_base_name'] + '-Tag',
                                          os.environ['conf_service_base_name'] + '-' + os.environ[
                                              'edge_user_name'] + '-edge')
        if edge_status != 'running':
            logging.info('ERROR: Edge node is unavailable! Aborting...')
            print('ERROR: Edge node is unavailable! Aborting...')
            ssn_hostname = get_instance_hostname(os.environ['conf_service_base_name'] + '-Tag',
                                                 os.environ['conf_service_base_name'] + '-ssn')
            put_resource_status('edge', 'Unavailable', os.environ['ssn_dlab_path'], os.environ['conf_os_user'],
                                ssn_hostname)
            append_result("Edge node is unavailable")
            sys.exit(1)
        print('Generating infrastructure names and tags')
        data_engine = dict()
        try:
            data_engine['exploratory_name'] = os.environ['exploratory_name']
        except:
            data_engine['exploratory_name'] = ''
        try:
            data_engine['computational_name'] = os.environ['computational_name']
        except:
            data_engine['computational_name'] = ''
        data_engine['service_base_name'] = os.environ['conf_service_base_name']
        data_engine['tag_name'] = data_engine['service_base_name'] + '-Tag'
        data_engine['key_name'] = os.environ['conf_key_name']
        data_engine['region'] = os.environ['aws_region']
        data_engine['cluster_name'] = data_engine['service_base_name'] + '-' + os.environ['edge_user_name'] + \
                                      '-de-' + data_engine['exploratory_name'] + '-' + \
                                      data_engine['computational_name']
        data_engine['master_node_name'] = data_engine['cluster_name'] + '-m'
        data_engine['slave_node_name'] = data_engine['cluster_name'] + '-s'
        data_engine['ami_id'] = get_ami_id(os.environ['aws_' + os.environ['conf_os_family'] + '_ami_name'])
        data_engine['master_size'] = os.environ['aws_dataengine_master_shape']
        data_engine['slave_size'] = os.environ['aws_dataengine_slave_shape']
        data_engine['dataengine_master_security_group_name'] = data_engine['service_base_name'] + '-' + \
                                                               os.environ['edge_user_name'] + '-dataengine-master-sg'
        data_engine['dataengine_slave_security_group_name'] = data_engine['service_base_name'] + '-' + \
                                                              os.environ['edge_user_name'] + '-dataengine-slave-sg'
        data_engine['tag_name'] = data_engine['service_base_name'] + '-Tag'
        tag = {"Key": data_engine['tag_name'],
               "Value": "{}-{}-subnet".format(data_engine['service_base_name'], os.environ['edge_user_name'])}
        data_engine['subnet_cidr'] = get_subnet_by_tag(tag)
        data_engine['notebook_dataengine_role_profile_name'] = data_engine['service_base_name'].\
                                                                   lower().replace('-', '_') + "-" + \
                                                               os.environ['edge_user_name'] + '-nb-de-Profile'
        data_engine['instance_count'] = int(os.environ['dataengine_instance_count'])
        data_engine['cluster_nodes_tag'] = {"Key": "dataengine_notebook_name",
                                            "Value": os.environ['notebook_instance_name']}
        data_engine['cluster_nodes_resource_tag'] = {"Key": os.environ['conf_tag_resource_id'],
                                                     "Value": data_engine['service_base_name'] + ':' +
                                                              data_engine['cluster_name']}
        data_engine['primary_disk_size'] = '30'
        data_engine['instance_class'] = 'dataengine'

    except Exception as err:
        print("Failed to generate variables dictionary.")
        append_result("Failed to generate variables dictionary. Exception:" + str(err))
        sys.exit(1)

    try:
        logging.info('[CREATE MASTER NODE]')
        print('[CREATE MASTER NODE]')
        data_engine['cluster_nodes_tag_type'] = {"Key": "Type", "Value": "master"}
        params = "--node_name {} --ami_id {} --instance_type {} --key_name {} --security_group_ids {} --subnet_id {} --iam_profile {} --infra_tag_name {} --infra_tag_value {} --primary_disk_size {} --instance_class {}" \
            .format(data_engine['master_node_name'], data_engine['ami_id'], data_engine['master_size'],
                    data_engine['key_name'],
                    get_security_group_by_name(data_engine['dataengine_master_security_group_name']),
                    get_subnet_by_cidr(data_engine['subnet_cidr']),
                    data_engine['notebook_dataengine_role_profile_name'], data_engine['tag_name'],
                    data_engine['master_node_name'], data_engine['primary_disk_size'], data_engine['instance_class'])
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
            data_engine['master_id'] = get_instance_by_name(data_engine['tag_name'], data_engine['master_node_name'])
            create_tag(data_engine['master_id'], data_engine['cluster_nodes_tag'], False)
            create_tag(data_engine['master_id'], data_engine['cluster_nodes_resource_tag'], False)
            create_tag(data_engine['master_id'], data_engine['cluster_nodes_tag_type'], False)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to create master instance.", str(err))
        sys.exit(1)

    try:
        for i in range(data_engine['instance_count'] - 1):
            logging.info('[CREATE SLAVE NODE {}]'.format(i + 1))
            print('[CREATE SLAVE NODE {}]'.format(i + 1))
            slave_name = data_engine['slave_node_name'] + '{}'.format(i + 1)
            data_engine['cluster_nodes_tag_type'] = {"Key": "Type", "Value": "slave"}
            params = "--node_name {} --ami_id {} --instance_type {} --key_name {} --security_group_ids {} --subnet_id {} --iam_profile {} --infra_tag_name {} --infra_tag_value {} --primary_disk_size {} --instance_class {}" \
                .format(slave_name, data_engine['ami_id'], data_engine['slave_size'],
                        data_engine['key_name'],
                        get_security_group_by_name(data_engine['dataengine_slave_security_group_name']),
                        get_subnet_by_cidr(data_engine['subnet_cidr']),
                        data_engine['notebook_dataengine_role_profile_name'], data_engine['tag_name'],
                        slave_name, data_engine['primary_disk_size'], data_engine['instance_class'])
            try:
                local("~/scripts/{}.py {}".format('common_create_instance', params))
                data_engine['slave_id'] = get_instance_by_name(data_engine['tag_name'], slave_name)
                create_tag(data_engine['slave_id'], data_engine['cluster_nodes_tag'], False)
                create_tag(data_engine['master_id'], data_engine['cluster_nodes_resource_tag'], False)
                create_tag(data_engine['master_id'], data_engine['cluster_nodes_tag_type'], False)
            except:
                traceback.print_exc()
                raise Exception
    except Exception as err:
        remove_ec2(data_engine['tag_name'], data_engine['master_node_name'])
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            try:
                remove_ec2(data_engine['tag_name'], slave_name)
            except:
                print("The slave instance {} hasn't been created.".format(slave_name))
        append_result("Failed to create slave instances.", str(err))
        sys.exit(1)
