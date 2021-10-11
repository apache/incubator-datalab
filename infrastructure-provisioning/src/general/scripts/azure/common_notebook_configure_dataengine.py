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
from fabric import *


def clear_resources():
    for i in range(notebook_config['instance_count'] - 1):
        slave_name = notebook_config['slave_node_name'] + '{}'.format(i + 1)
        AzureActions.remove_instance(notebook_config['resource_group_name'], slave_name)
    AzureActions.remove_instance(notebook_config['resource_group_name'], notebook_config['master_node_name'])


if __name__ == "__main__":
    try:
        # generating variables dictionary
        AzureMeta = datalab.meta_lib.AzureMeta()
        AzureActions = datalab.actions_lib.AzureActions()
        logging.info('Generating infrastructure names and tags')
        notebook_config = dict()
        if 'exploratory_name' in os.environ:
            notebook_config['exploratory_name'] = os.environ['exploratory_name']
        else:
            notebook_config['exploratory_name'] = ''
        if 'computational_name' in os.environ:
            notebook_config['computational_name'] = os.environ['computational_name']
        else:
            notebook_config['computational_name'] = ''
        notebook_config['service_base_name'] = os.environ['conf_service_base_name']
        notebook_config['resource_group_name'] = os.environ['azure_resource_group_name']
        notebook_config['region'] = os.environ['azure_region']
        notebook_config['user_name'] = os.environ['edge_user_name']
        notebook_config['project_name'] = os.environ['project_name']
        notebook_config['project_tag'] = notebook_config['project_name']
        notebook_config['endpoint_name'] = os.environ['endpoint_name']
        notebook_config['endpoint_tag'] = notebook_config['endpoint_name']
        notebook_config['cluster_name'] = '{}-{}-{}-de-{}'.format(notebook_config['service_base_name'],
                                                                  notebook_config['project_name'],
                                                                  notebook_config['endpoint_name'],
                                                                  notebook_config['computational_name'])
        notebook_config['master_node_name'] = notebook_config['cluster_name'] + '-m'
        notebook_config['slave_node_name'] = notebook_config['cluster_name'] + '-s'
        notebook_config['notebook_name'] = os.environ['notebook_instance_name']
        notebook_config['key_path'] = '{}/{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        notebook_config['datalab_ssh_user'] = os.environ['conf_os_user']
        notebook_config['instance_count'] = int(os.environ['dataengine_instance_count'])
        try:
            notebook_config['spark_master_ip'] = AzureMeta.get_private_ip_address(
                notebook_config['resource_group_name'], notebook_config['master_node_name'])
            notebook_config['notebook_ip'] = AzureMeta.get_private_ip_address(
                notebook_config['resource_group_name'], notebook_config['notebook_name'])
        except Exception as err:
            datalab.fab.append_result("Failed to get instance IP address", str(err))
            clear_resources()
            sys.exit(1)
        notebook_config['spark_master_url'] = 'spark://{}:7077'.format(notebook_config['spark_master_ip'])

    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to generate infrastructure names", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]')
        params = "--cluster_name {0} --spark_version {1} --hadoop_version {2} --os_user {3} --spark_master {4}" \
                 " --keyfile {5} --notebook_ip {6} --datalake_enabled {7} --spark_master_ip {8}".\
            format(notebook_config['cluster_name'], os.environ['notebook_spark_version'],
                   os.environ['notebook_hadoop_version'], notebook_config['datalab_ssh_user'],
                   notebook_config['spark_master_url'], notebook_config['key_path'], notebook_config['notebook_ip'],
                   os.environ['azure_datalake_enable'], notebook_config['spark_master_ip'])
        try:
            subprocess.run("~/scripts/{}_{}.py {}".format(os.environ['application'], 'install_dataengine_kernels', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed installing Dataengine kernels.", str(err))
        sys.exit(1)

    try:
        logging.info('[UPDATING SPARK CONFIGURATION FILES ON NOTEBOOK]')
        params = "--hostname {0} " \
                 "--keyfile {1} " \
                 "--os_user {2} " \
                 "--cluster_name {3} " \
            .format(notebook_config['notebook_ip'],
                    notebook_config['key_path'],
                    notebook_config['datalab_ssh_user'],
                    notebook_config['cluster_name'])
        try:
            subprocess.run("~/scripts/{0}.py {1}".format('common_configure_spark', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to configure Spark.", str(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Action": "Configure notebook server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        clear_resources()
        sys.exit(1)
