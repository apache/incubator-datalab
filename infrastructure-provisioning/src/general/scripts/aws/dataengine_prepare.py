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
import os
import sys
import traceback
import subprocess
from fabric import *
from datalab.logger import logging

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.INFO,
                        filename=local_log_filepath)
    try:
        datalab.actions_lib.create_aws_config_files()
        data_engine = dict()
        data_engine['service_base_name'] = (os.environ['conf_service_base_name'])
        data_engine['project_name'] = os.environ['project_name']
        data_engine['endpoint_name'] = os.environ['endpoint_name']
        edge_status = datalab.meta_lib.get_instance_status(
            data_engine['service_base_name'] + '-tag', '{0}-{1}-{2}-edge'.format(
                data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_name']))
        if edge_status != 'running':
            logging.info('ERROR: Edge node is unavailable! Aborting...')
            ssn_hostname = datalab.meta_lib.get_instance_hostname(data_engine['service_base_name'] + '-tag',
                                                                  data_engine['service_base_name'] + '-ssn')
            datalab.fab.put_resource_status('edge', 'Unavailable', os.environ['ssn_datalab_path'],
                                            os.environ['conf_os_user'],
                                            ssn_hostname)
            datalab.fab.append_result("Edge node is unavailable")
            sys.exit(1)
        logging.info('Generating infrastructure names and tags')
        if 'exploratory_name' in os.environ:
            data_engine['exploratory_name'] = os.environ['exploratory_name'].lower()
        else:
            data_engine['exploratory_name'] = ''
        if 'computational_name' in os.environ:
            data_engine['computational_name'] = os.environ['computational_name'].lower()
        else:
            data_engine['computational_name'] = ''
        data_engine['custom_tag'] = ''
        if 'custom_tag' in os.environ['tags']:
            data_engine['custom_tag'] = json.loads(os.environ['tags'].replace("'", '"'))['custom_tag']
        if data_engine['custom_tag']:
            data_engine['custom_tag'] = ';custom_tag:{}'.format(data_engine['custom_tag'])
        data_engine['tag_name'] = data_engine['service_base_name'] + '-tag'
        data_engine['key_name'] = os.environ['conf_key_name']
        data_engine['region'] = os.environ['aws_region']
        data_engine['cluster_name'] = "{}-{}-{}-de-{}".format(data_engine['service_base_name'],
                                                              data_engine['project_name'],
                                                              data_engine['endpoint_name'],
                                                              data_engine['computational_name'])
        data_engine['master_node_name'] = '{}-m'.format(data_engine['cluster_name'])
        data_engine['slave_node_name'] = '{}-s'.format(data_engine['cluster_name'])
        data_engine['master_size'] = os.environ['aws_dataengine_master_shape']
        data_engine['slave_size'] = os.environ['aws_dataengine_slave_shape']
        data_engine['dataengine_master_security_group_name'] = '{}-{}-{}-de-master-sg' \
            .format(data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_name'])
        data_engine['dataengine_slave_security_group_name'] = '{}-{}-{}-de-slave-sg' \
            .format(data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_name'])
        data_engine['tag_name'] = '{}-tag'.format(data_engine['service_base_name'])
        tag = {"Key": data_engine['tag_name'],
               "Value": "{}-{}-{}-subnet".format(data_engine['service_base_name'], data_engine['project_name'],
                                                 data_engine['endpoint_name'])}
        data_engine['subnet_cidr'] = datalab.meta_lib.get_subnet_by_tag(tag)
        data_engine['notebook_dataengine_role_profile_name'] = '{}-{}-{}-nb-de-profile' \
            .format(data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_name'])
        data_engine['instance_count'] = int(os.environ['dataengine_instance_count'])
        data_engine['cluster_nodes_tag'] = {"Key": "dataengine_notebook_name",
                                            "Value": os.environ['notebook_instance_name']}
        data_engine['cluster_nodes_resource_tag'] = {"Key": os.environ['conf_tag_resource_id'],
                                                     "Value": "{}:{}".format(data_engine['service_base_name'],
                                                                             data_engine['cluster_name'])}
        data_engine['cluster_nodes_billing_tag'] = {"Key": os.environ['conf_billing_tag_key'],
                                                    "Value": os.environ['conf_billing_tag_value']}
        if os.environ['conf_deeplearning_cloud_ami'] == 'true' and os.environ['application'] == 'deeplearning':
            data_engine['primary_disk_size'] = '150'
        else:
            data_engine['primary_disk_size'] = '30'
        data_engine['instance_class'] = 'dataengine'

        if os.environ['conf_shared_image_enabled'] == 'false':
            data_engine['expected_image_name'] = '{0}-{1}-{2}-{3}-notebook-image'.format(
                data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_name'],
                os.environ['application'])
        else:
            data_engine['expected_image_name'] = '{0}-{1}-{2}-notebook-image'.format(data_engine['service_base_name'],
                                                                                     data_engine['endpoint_name'],
                                                                                     os.environ['application'])
        data_engine['notebook_image_name'] = (
            lambda x: '{0}-{1}-{4}-{2}-{3}'.format(data_engine['service_base_name'],
                                                   data_engine['project_name'],
                                                   os.environ['application'],
                                                   os.environ['notebook_image_name'],
                                                   data_engine['endpoint_name']) if (
                    x != 'None' and x != '')
            else data_engine['expected_image_name'])(str(os.environ.get('notebook_image_name')))
        logging.info('Searching pre-configured images')
        data_engine['ami_id'] = datalab.meta_lib.get_ami_id(os.environ['aws_{}_image_name'.format(
            os.environ['conf_os_family'])])
        image_id = datalab.meta_lib.get_ami_id_by_name(data_engine['notebook_image_name'], 'available')
        if image_id != '' and os.environ['application'] in os.environ['dataengine_image_notebooks'].split(','):
            data_engine['ami_id'] = image_id
            logging.info('Pre-configured image found. Using: {}'.format(data_engine['ami_id']))
        else:
            os.environ['notebook_image_name'] = os.environ['aws_{}_image_name'.format(os.environ['conf_os_family'])]
            logging.info('No pre-configured image found. Using default one: {}'.format(data_engine['ami_id']))

    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        sys.exit(1)

    with open('/root/result.json', 'w') as f:
        data = {"hostname": data_engine['cluster_name'], "error": ""}
        json.dump(data, f)

    try:
        os.environ['conf_additional_tags'] = '{2};project_tag:{0};endpoint_tag:{1}{3}'.format(
            data_engine['project_name'], data_engine['endpoint_name'], os.environ['conf_additional_tags'],
            data_engine['custom_tag'])
    except KeyError:
        os.environ['conf_additional_tags'] = 'project_tag:{0};endpoint_tag:{1}{2}'.format(
            data_engine['project_name'], data_engine['endpoint_name'], data_engine['custom_tag'])

    logging.info('Additional tags will be added: {}'.format(os.environ['conf_additional_tags']))

    try:
        logging.info('[CREATE MASTER NODE]')
        data_engine['cluster_nodes_tag_type'] = {"Key": "Type", "Value": "master"}
        params = "--node_name {} --ami_id {} --instance_type {} --key_name {} --security_group_ids {} " \
                 "--subnet_id {} --iam_profile {} --infra_tag_name {} --infra_tag_value {} --primary_disk_size {} " \
                 "--instance_class {}" \
            .format(data_engine['master_node_name'], data_engine['ami_id'], data_engine['master_size'],
                    data_engine['key_name'],
                    datalab.meta_lib.get_security_group_by_name(data_engine['dataengine_master_security_group_name']),
                    datalab.meta_lib.get_subnet_by_cidr(data_engine['subnet_cidr'], os.environ['aws_notebook_vpc_id']),
                    data_engine['notebook_dataengine_role_profile_name'], data_engine['tag_name'],
                    data_engine['master_node_name'], data_engine['primary_disk_size'], data_engine['instance_class'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_instance', params), shell=True, check=True)
            data_engine['master_id'] = datalab.meta_lib.get_instance_by_name(data_engine['tag_name'],
                                                                             data_engine['master_node_name'])
            datalab.actions_lib.create_tag(data_engine['master_id'], data_engine['cluster_nodes_tag'], False)
            datalab.actions_lib.create_tag(data_engine['master_id'], data_engine['cluster_nodes_resource_tag'], False)
            datalab.actions_lib.create_tag(data_engine['master_id'], data_engine['cluster_nodes_billing_tag'], False)
            datalab.actions_lib.create_tag(data_engine['master_id'], data_engine['cluster_nodes_tag_type'], False)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create master instance.", str(err))
        sys.exit(1)

    try:
        for i in range(data_engine['instance_count'] - 1):
            logging.info('[CREATE SLAVE NODE {}]'.format(i + 1))
            slave_name = data_engine['slave_node_name'] + '{}'.format(i + 1)
            data_engine['cluster_nodes_tag_type'] = {"Key": "Type", "Value": "slave"}
            params = "--node_name {} --ami_id {} --instance_type {} --key_name {} --security_group_ids {} " \
                     "--subnet_id {} --iam_profile {} --infra_tag_name {} --infra_tag_value {} " \
                     "--primary_disk_size {} --instance_class {}" \
                .format(slave_name, data_engine['ami_id'], data_engine['slave_size'],
                        data_engine['key_name'],
                        datalab.meta_lib.get_security_group_by_name(
                            data_engine['dataengine_slave_security_group_name']),
                        datalab.meta_lib.get_subnet_by_cidr(data_engine['subnet_cidr'],
                                                            os.environ['aws_notebook_vpc_id']),
                        data_engine['notebook_dataengine_role_profile_name'], data_engine['tag_name'],
                        slave_name, data_engine['primary_disk_size'], data_engine['instance_class'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('common_create_instance', params), shell=True, check=True)
                data_engine['slave_id'] = datalab.meta_lib.get_instance_by_name(data_engine['tag_name'], slave_name)
                datalab.actions_lib.create_tag(data_engine['slave_id'], data_engine['cluster_nodes_tag'], False)
                datalab.actions_lib.create_tag(data_engine['slave_id'], data_engine['cluster_nodes_resource_tag'],
                                               False)
                datalab.actions_lib.create_tag(data_engine['slave_id'], data_engine['cluster_nodes_billing_tag'], False)
                datalab.actions_lib.create_tag(data_engine['slave_id'], data_engine['cluster_nodes_tag_type'], False)
            except:
                traceback.print_exc()
                raise Exception
    except Exception as err:
        datalab.actions_lib.remove_ec2(data_engine['tag_name'], data_engine['master_node_name'])
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            try:
                datalab.actions_lib.remove_ec2(data_engine['tag_name'], slave_name)
            except:
                logging.error("The slave instance {} hasn't been created.".format(slave_name))
        datalab.fab.append_result("Failed to create slave instances.", str(err))
        sys.exit(1)
