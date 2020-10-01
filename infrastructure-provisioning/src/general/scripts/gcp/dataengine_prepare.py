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

import datalab.meta_lib
import json
import logging
import os
import sys
import traceback
from fabric.api import *

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
        data_engine = dict()
        data_engine['service_base_name'] = (os.environ['conf_service_base_name'])
        data_engine['edge_user_name'] = (os.environ['edge_user_name'])
        data_engine['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
        data_engine['project_tag'] = data_engine['project_name']
        data_engine['endpoint_name'] = os.environ['endpoint_name'].replace('_', '-').lower()
        data_engine['endpoint_tag'] = data_engine['endpoint_name']
        data_engine['region'] = os.environ['gcp_region']
        data_engine['zone'] = os.environ['gcp_zone']

        edge_status = GCPMeta.get_instance_status('{0}-{1}-{2}-edge'.format(data_engine['service_base_name'],
                                                                            data_engine['project_name'],
                                                                            data_engine['endpoint_name']))
        if edge_status != 'RUNNING':
            logging.info('ERROR: Edge node is unavailable! Aborting...')
            print('ERROR: Edge node is unavailable! Aborting...')
            ssn_hostname = GCPMeta.get_private_ip_address(data_engine['service_base_name'] + '-ssn')
            datalab.fab.put_resource_status('edge', 'Unavailable', os.environ['ssn_datalab_path'],
                                            os.environ['conf_os_user'],
                                            ssn_hostname)
            datalab.fab.append_result("Edge node is unavailable")
            sys.exit(1)

        try:
            if os.environ['gcp_vpc_name'] == '':
                raise KeyError
            else:
                data_engine['vpc_name'] = os.environ['gcp_vpc_name']
        except KeyError:
            data_engine['vpc_name'] = '{}-vpc'.format(data_engine['service_base_name'])
        if 'exploratory_name' in os.environ:
            data_engine['exploratory_name'] = os.environ['exploratory_name'].replace('_', '-').lower()
        else:
            data_engine['exploratory_name'] = ''
        if 'computational_name' in os.environ:
            data_engine['computational_name'] = os.environ['computational_name'].replace('_', '-').lower()
        else:
            data_engine['computational_name'] = ''

        data_engine['subnet_name'] = '{0}-{1}-{2}-subnet'.format(data_engine['service_base_name'],
                                                                 data_engine['project_name'],
                                                                 data_engine['endpoint_name'])
        data_engine['master_size'] = os.environ['gcp_dataengine_master_size']
        data_engine['slave_size'] = os.environ['gcp_dataengine_slave_size']
        data_engine['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        data_engine['dataengine_service_account_name'] = '{}-{}-{}-ps-sa'.format(data_engine['service_base_name'],
                                                                                 data_engine['project_name'],
                                                                                 data_engine['endpoint_name'])

        if os.environ['conf_os_family'] == 'debian':
            initial_user = 'ubuntu'
            sudo_group = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            initial_user = 'ec2-user'
            sudo_group = 'wheel'
        data_engine['cluster_name'] = "{}-{}-{}-de-{}".format(data_engine['service_base_name'],
                                                              data_engine['project_name'],
                                                              data_engine['endpoint_name'],
                                                              data_engine['computational_name'])
        data_engine['master_node_name'] = data_engine['cluster_name'] + '-m'
        data_engine['slave_node_name'] = data_engine['cluster_name'] + '-s'
        data_engine['instance_count'] = int(os.environ['dataengine_instance_count'])
        data_engine['notebook_name'] = os.environ['notebook_instance_name']

        data_engine['primary_disk_size'] = '30'
        data_engine['secondary_disk_size'] = os.environ['notebook_disk_size']

        data_engine['shared_image_enabled'] = os.environ['conf_shared_image_enabled']
        if data_engine['shared_image_enabled'] == 'false':
            data_engine['expected_primary_image_name'] = '{}-{}-{}-{}-primary-image'.format(
                data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_tag'],
                os.environ['application'])
            data_engine['expected_secondary_image_name'] = '{}-{}-{}-{}-secondary-image'.format(
                data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_tag'],
                os.environ['application'])
        else:
            data_engine['expected_primary_image_name'] = '{}-{}-{}-primary-image'.format(
                data_engine['service_base_name'], data_engine['endpoint_tag'], os.environ['application'])
            data_engine['expected_secondary_image_name'] = '{}-{}-{}-secondary-image'.format(
                data_engine['service_base_name'], data_engine['endpoint_tag'], os.environ['application'])
        data_engine['notebook_primary_image_name'] = (lambda x: os.environ['notebook_primary_image_name'] if x != 'None'
        else data_engine['expected_primary_image_name'])(str(os.environ.get('notebook_primary_image_name')))
        print('Searching pre-configured images')
        data_engine['primary_image_name'] = GCPMeta.get_image_by_name(data_engine['notebook_primary_image_name'])
        if data_engine['primary_image_name'] == '':
            data_engine['primary_image_name'] = os.environ['gcp_{}_image_name'.format(os.environ['conf_os_family'])]
        else:
            print('Pre-configured primary image found. Using: {}'.format(data_engine['primary_image_name'].get('name')))
            data_engine['primary_image_name'] = 'global/images/{}'.format(
                data_engine['primary_image_name'].get('name'))

        data_engine['secondary_image_name'] = GCPMeta.get_image_by_name(data_engine['expected_secondary_image_name'])
        if data_engine['secondary_image_name'] == '':
            data_engine['secondary_image_name'] = 'None'
        else:
            print('Pre-configured secondary image found. Using: {}'.format(
                data_engine['secondary_image_name'].get('name')))
            data_engine['secondary_image_name'] = 'global/images/{}'.format(
                data_engine['secondary_image_name'].get('name'))

        with open('/root/result.json', 'w') as f:
            data = {"hostname": data_engine['cluster_name'], "error": ""}
            json.dump(data, f)

        data_engine['gpu_accelerator_type'] = 'None'
        if os.environ['application'] in ('tensor', 'tensor-rstudio', 'deeplearning'):
            data_engine['gpu_accelerator_type'] = os.environ['gcp_gpu_accelerator_type']
        data_engine['network_tag'] = '{0}-{1}-{2}-ps'.format(data_engine['service_base_name'],
                                                             data_engine['project_name'], data_engine['endpoint_name'])
        additional_tags = os.environ['tags'].replace("': u'", ":").replace("', u'", ",").replace("{u'", "").replace(
            "'}", "").lower()

        data_engine['slave_labels'] = {"name": data_engine['cluster_name'],
                                       "sbn": data_engine['service_base_name'],
                                       "type": "slave",
                                       "notebook_name": data_engine['notebook_name'],
                                       "product": "datalab"}
        data_engine['master_labels'] = {"name": data_engine['cluster_name'],
                                        "sbn": data_engine['service_base_name'],
                                        "type": "master",
                                        "notebook_name": data_engine['notebook_name'],
                                        "product": "datalab"}

        for tag in additional_tags.split(','):
            label_key = tag.split(':')[0]
            label_value = tag.split(':')[1].replace('_', '-')
            if '@' in label_value:
                label_value = label_value[:label_value.find('@')]
            if label_value != '':
                data_engine['slave_labels'].update({label_key: label_value})
                data_engine['master_labels'].update({label_key: label_value})
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary. Exception:" + str(err))
        sys.exit(1)

    try:
        logging.info('[CREATE MASTER NODE]')
        print('[CREATE MASTER NODE]')
        params = "--instance_name {0} --region {1} --zone {2} --vpc_name {3} --subnet_name {4} --instance_size {5} " \
                 "--ssh_key_path {6} --initial_user {7} --service_account_name {8} --image_name {9} " \
                 "--secondary_image_name {10} --instance_class {11} --primary_disk_size {12} " \
                 "--secondary_disk_size {13} --gpu_accelerator_type {14} --network_tag {15} --cluster_name {16} " \
                 "--labels '{17}' --service_base_name {18}". \
            format(data_engine['master_node_name'], data_engine['region'], data_engine['zone'], data_engine['vpc_name'],
                   data_engine['subnet_name'], data_engine['master_size'], data_engine['ssh_key_path'], initial_user,
                   data_engine['dataengine_service_account_name'], data_engine['primary_image_name'],
                   data_engine['secondary_image_name'], 'dataengine', data_engine['primary_disk_size'],
                   data_engine['secondary_disk_size'], data_engine['gpu_accelerator_type'],
                   data_engine['network_tag'], data_engine['cluster_name'],
                   json.dumps(data_engine['master_labels']), data_engine['service_base_name'])
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create instance.", str(err))
        GCPActions.remove_instance(data_engine['master_node_name'], data_engine['zone'])
        sys.exit(1)

    try:
        for i in range(data_engine['instance_count'] - 1):
            logging.info('[CREATE SLAVE NODE {}]'.format(i + 1))
            print('[CREATE SLAVE NODE {}]'.format(i + 1))
            slave_name = data_engine['slave_node_name'] + '{}'.format(i + 1)
            params = "--instance_name {0} --region {1} --zone {2} --vpc_name {3} --subnet_name {4} " \
                     "--instance_size {5} --ssh_key_path {6} --initial_user {7} --service_account_name {8} " \
                     "--image_name {9} --secondary_image_name {10} --instance_class {11} --primary_disk_size {12} " \
                     "--secondary_disk_size {13} --gpu_accelerator_type {14} --network_tag {15} --cluster_name {16} " \
                     "--labels '{17}' --service_base_name {18}". \
                format(slave_name, data_engine['region'], data_engine['zone'],
                       data_engine['vpc_name'], data_engine['subnet_name'], data_engine['slave_size'],
                       data_engine['ssh_key_path'], initial_user, data_engine['dataengine_service_account_name'],
                       data_engine['primary_image_name'], data_engine['secondary_image_name'], 'dataengine',
                       data_engine['primary_disk_size'],
                       data_engine['secondary_disk_size'], data_engine['gpu_accelerator_type'],
                       data_engine['network_tag'], data_engine['cluster_name'],
                       json.dumps(data_engine['slave_labels']), data_engine['service_base_name'])
            try:
                local("~/scripts/{}.py {}".format('common_create_instance', params))
            except:
                traceback.print_exc()
                raise Exception
    except Exception as err:
        for i in range(data_engine['instance_count'] - 1):
            slave_name = data_engine['slave_node_name'] + '{}'.format(i+1)
            try:
                GCPActions.remove_instance(slave_name, data_engine['zone'])
            except:
                print("The slave instance {} hasn't been created.".format(slave_name))
        GCPActions.remove_instance(data_engine['master_node_name'], data_engine['zone'])
        datalab.fab.append_result("Failed to create slave instances.", str(err))
        sys.exit(1)
