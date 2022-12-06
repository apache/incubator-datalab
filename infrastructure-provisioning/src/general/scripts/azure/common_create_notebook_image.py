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
from datalab.logger import logging
import json
import os
import sys
import subprocess
from fabric import *

if __name__ == "__main__":
    try:
        AzureMeta = datalab.meta_lib.AzureMeta()
        AzureActions = datalab.actions_lib.AzureActions()
        image_conf = dict()
        image_conf['service_base_name'] = os.environ['conf_service_base_name']
        image_conf['resource_group_name'] = os.environ['azure_resource_group_name']
        image_conf['user_name'] = os.environ['edge_user_name']
        image_conf['project_name'] = os.environ['project_name']
        image_conf['project_tag'] = image_conf['project_name']
        image_conf['endpoint_name'] = os.environ['endpoint_name']
        image_conf['endpoint_tag'] = image_conf['endpoint_name']
        image_conf['instance_name'] = os.environ['notebook_instance_name']
        image_conf['application'] = os.environ['application']
        image_conf['datalab_ssh_user'] = os.environ['conf_os_user']
        image_conf['image_name'] = os.environ['notebook_image_name'].replace('_', '-')
        image_conf['full_image_name'] = '{}-{}-{}-{}-{}'.format(image_conf['service_base_name'],
                                                                image_conf['project_name'],
                                                                image_conf['endpoint_name'],
                                                                image_conf['application'],
                                                                image_conf['image_name'])
        image_conf['tags'] = {"Instance_Name": image_conf['instance_name'],
                              "SBN": image_conf['service_base_name'],
                              "User": image_conf['user_name'],
                              "project_tag": image_conf['project_tag'],
                              "endpoint_tag": image_conf['endpoint_tag'],
                              "Image": image_conf['image_name'],
                              "Name": image_conf['full_image_name'],
                              os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value']}

        instance_hostname = AzureMeta.get_private_ip_address(image_conf['resource_group_name'],
                                                             image_conf['instance_name'])
        edge_instance_name = '{0}-{1}-{2}-edge'.format(image_conf['service_base_name'],
                                                       image_conf['project_name'],
                                                       image_conf['endpoint_name'])
        edge_instance_hostname = AzureMeta.get_private_ip_address(image_conf['resource_group_name'],
                                                                    edge_instance_name)
        keyfile_name = "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name'])

        instance = AzureMeta.get_instance(image_conf['resource_group_name'], image_conf['instance_name'])
        os.environ['azure_notebook_instance_size'] = instance.hardware_profile.vm_size
        os.environ['exploratory_name'] = instance.tags['Exploratory']
        os.environ['notebook_image_name'] = image_conf['image_name']

        image = AzureMeta.get_image(image_conf['resource_group_name'], image_conf['full_image_name'])
        if image == '':
            logging.info('Creating image from existing notebook.')
            datalab.actions_lib.prepare_vm_for_image(True, image_conf['datalab_ssh_user'], instance_hostname,
                                                     keyfile_name)
            AzureActions.create_image_from_instance(image_conf['resource_group_name'],
                                                    image_conf['instance_name'],
                                                    os.environ['azure_region'],
                                                    image_conf['full_image_name'],
                                                    json.dumps(image_conf['tags']))
            logging.info("Image was successfully created.")
            try:
                subprocess.run("~/scripts/{}.py".format('common_prepare_notebook'), shell=True, check=True)
                instance_running = False
                while not instance_running:
                    if AzureMeta.get_instance_status(image_conf['resource_group_name'],
                                                     image_conf['instance_name']) == 'running':
                        instance_running = True
                instance_hostname = AzureMeta.get_private_ip_address(image_conf['resource_group_name'],
                                                                     image_conf['instance_name'])
                datalab.actions_lib.remount_azure_disk(True, image_conf['datalab_ssh_user'], instance_hostname,
                                                       keyfile_name)
                datalab.actions_lib.ensure_right_mount_paths(True, image_conf['datalab_ssh_user'], instance_hostname,
                                                       keyfile_name)
                datalab.fab.set_git_proxy(image_conf['datalab_ssh_user'], instance_hostname, keyfile_name,
                                          'http://{}:3128'.format(edge_instance_hostname))
                additional_config = {"proxy_host": edge_instance_hostname, "proxy_port": "3128"}
                params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}" \
                    .format(instance_hostname, image_conf['instance_name'], keyfile_name,
                            json.dumps(additional_config), image_conf['datalab_ssh_user'])
                subprocess.run("~/scripts/{}.py {}".format('common_configure_proxy', params), shell=True, check=True)
                logging.info("Image was successfully created. It's name is {}".format(image_conf['full_image_name']))
            except Exception as err:
                AzureActions.remove_instance(image_conf['resource_group_name'], image_conf['instance_name'])
                datalab.fab.append_result("Failed to create instance from image.", str(err))
                sys.exit(1)

            with open("/root/result.json", 'w') as result:
                res = {"notebook_image_name": image_conf['image_name'],
                       "ip": instance_hostname,
                       "full_image_name": image_conf['full_image_name'],
                       "user_name": image_conf['user_name'],
                       "project_name": image_conf['project_name'],
                       "application": image_conf['application'],
                       "status": "active",
                       "Action": "Create image from notebook"}
                result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Failed to create image from notebook", str(err))
        sys.exit(1)
