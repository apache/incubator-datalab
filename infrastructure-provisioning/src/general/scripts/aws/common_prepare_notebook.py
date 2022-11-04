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

import argparse
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

parser = argparse.ArgumentParser()
parser.add_argument('--uuid', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    try:
        # generating variables dictionary
        datalab.actions_lib.create_aws_config_files()
        notebook_config = dict()
        notebook_config['service_base_name'] = os.environ['conf_service_base_name']
        notebook_config['project_name'] = os.environ['project_name']
        notebook_config['endpoint_name'] = os.environ['endpoint_name']
        notebook_config['edge_name'] = '{}-{}-{}-edge'.format(notebook_config['service_base_name'],
                                                              notebook_config['project_name'],
                                                              notebook_config['endpoint_name'])
        edge_status = datalab.meta_lib.get_instance_status(notebook_config['service_base_name'] + '-tag',
                                                           notebook_config['edge_name'])
        if edge_status != 'running':
            logging.info('ERROR: Edge node is unavailable! Aborting...')
            notebook_config['ssn_hostname'] = datalab.meta_lib.get_instance_hostname(
                '{}-tag'.format(notebook_config['service_base_name']),
                '{}-ssn'.format(notebook_config['service_base_name']))
            datalab.fab.put_resource_status('edge', 'Unavailable', os.environ['ssn_datalab_path'],
                                            os.environ['conf_os_user'],
                                            notebook_config['ssn_hostname'])
            datalab.fab.append_result("Edge node is unavailable")
            sys.exit(1)
        logging.info('Generating infrastructure names and tags')
        try:
            notebook_config['exploratory_name'] = os.environ['exploratory_name'].lower()
        except:
            notebook_config['exploratory_name'] = ''
        notebook_config['custom_tag'] = ''
        if 'custom_tag' in os.environ['tags']:
            notebook_config['custom_tag'] = json.loads(os.environ['tags'].replace("'", '"'))['custom_tag']
            if notebook_config['custom_tag']:
                notebook_config['custom_tag'] = ';custom_tag:{}'.format(notebook_config['custom_tag'])
        notebook_config['instance_type'] = os.environ['aws_notebook_instance_type']
        notebook_config['key_name'] = os.environ['conf_key_name']
        notebook_config['instance_name'] = '{}-{}-{}-nb-{}-{}'.format(notebook_config['service_base_name'],
                                                                      notebook_config['project_name'],
                                                                      notebook_config['endpoint_name'],
                                                                      notebook_config['exploratory_name'], args.uuid)

        notebook_config['primary_disk_size'] = (lambda x: '150' if x == 'deeplearning' else
        ('28' if x == 'tensor' or x == 'tensor-rstudio' else '16'))(os.environ['application'])

        notebook_config['role_profile_name'] = '{}-{}-{}-nb-de-profile'.format(
            notebook_config['service_base_name'], notebook_config['project_name'], notebook_config['endpoint_name'])
        notebook_config['security_group_name'] = '{}-{}-{}-nb-sg'.format(notebook_config['service_base_name'],
                                                                         notebook_config['project_name'],
                                                                         notebook_config['endpoint_name'])
        notebook_config['tag_name'] = '{}-tag'.format(notebook_config['service_base_name'])

        if os.environ['conf_shared_image_enabled'] == 'false':
            notebook_config['expected_image_name'] = '{0}-{1}-{2}-{3}-notebook-image'.format(
                notebook_config['service_base_name'], notebook_config['project_name'], notebook_config['endpoint_name'],
                os.environ['application'])
        else:
            notebook_config['expected_image_name'] = '{0}-{1}-{2}-notebook-image'.format(
                notebook_config['service_base_name'], notebook_config['endpoint_name'], os.environ['application'])
        notebook_config['notebook_image_name'] = (lambda x: '{0}-{1}-{2}-{3}-{4}'.format(
            notebook_config['service_base_name'], notebook_config['project_name'], notebook_config['endpoint_name'],
            os.environ['application'], os.environ['notebook_image_name']) if (x != 'None' and x != '')
            else notebook_config['expected_image_name'])(str(os.environ.get('notebook_image_name')))
        logging.info('Searching pre-configured images')
        notebook_config['ami_id'] = datalab.meta_lib.get_ami_id(os.environ['aws_{}_image_name'.format(
            os.environ['conf_os_family'])])
        image_id = datalab.meta_lib.get_ami_id_by_name(notebook_config['notebook_image_name'], 'available')
        if os.environ['conf_deeplearning_cloud_ami'] == 'true' and os.environ['application'] == 'deeplearning' and image_id == '':
            image_id = datalab.meta_lib.get_ami_id(os.environ['notebook_image_name'])
        if image_id != '':
            notebook_config['ami_id'] = image_id
            logging.info('Pre-configured image found. Using: {}'.format(notebook_config['ami_id']))
        else:
            if 'notebook_image_name' in os.environ:
                logging.info('{} is available in the list for notebook creation but image_id '
                             'is not present on the cloud'.format(os.environ['notebook_image_name']))
                sys.exit(1)

            os.environ['notebook_image_name'] = os.environ['aws_{}_image_name'.format(os.environ['conf_os_family'])]
            logging.info('No pre-configured image found. Using default one: {}'.format(notebook_config['ami_id']))

        tag = {"Key": notebook_config['tag_name'],
               "Value": "{}-{}-{}-subnet".format(notebook_config['service_base_name'], notebook_config['project_name'],
                                                 notebook_config['endpoint_name'])}
        notebook_config['subnet_cidr'] = datalab.meta_lib.get_subnet_by_tag(tag)
        keyfile_name = "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name'])

        with open('/root/result.json', 'w') as f:
            data = {"notebook_name": notebook_config['instance_name'], "error": ""}
            json.dump(data, f)

        try:
            os.environ['conf_additional_tags'] = '{2};project_tag:{0};endpoint_tag:{1}{3}'.format(
                notebook_config['project_name'], notebook_config['endpoint_name'], os.environ['conf_additional_tags'],
                notebook_config['custom_tag'])
        except KeyError as ex:
            os.environ['conf_additional_tags'] = 'project_tag:{0};endpoint_tag:{1}{2}'.format(
                notebook_config['project_name'], notebook_config['endpoint_name'], notebook_config['custom_tag'])

        logging.info('Additional tags will be added: {}'.format(os.environ['conf_additional_tags']))
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        sys.exit(1)

    # launching instance for notebook server
    try:
        logging.info('[CREATE NOTEBOOK INSTANCE]')
        params = "--node_name {} --ami_id {} --instance_type {} --key_name {} --security_group_ids {} --subnet_id {} " \
                 "--iam_profile {} --infra_tag_name {} --infra_tag_value {} --instance_class {} " \
                 "--instance_disk_size {} --primary_disk_size {}" .format(
            notebook_config['instance_name'], notebook_config['ami_id'], notebook_config['instance_type'],
            notebook_config['key_name'],
            datalab.meta_lib.get_security_group_by_name(notebook_config['security_group_name']),
            datalab.meta_lib.get_subnet_by_cidr(notebook_config['subnet_cidr'], os.environ['aws_notebook_vpc_id']),
            notebook_config['role_profile_name'],
            notebook_config['tag_name'], notebook_config['instance_name'], instance_class,
            os.environ['notebook_disk_size'], notebook_config['primary_disk_size'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_instance', params), shell=True, check=True)

        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create instance.", str(err))
        sys.exit(1)
