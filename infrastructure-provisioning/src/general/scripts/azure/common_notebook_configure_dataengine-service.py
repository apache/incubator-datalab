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
    AzureActions.terminate_hdinsight_cluster(notebook_config['resource_group_name'],
                                             notebook_config['full_cluster_name'])
    for storage_account in AzureMeta.list_storage_accounts(notebook_config['resource_group_name']):
        if notebook_config['storage_account_name_tag'] == storage_account.tags["Name"]:
            AzureActions.remove_storage_account(notebook_config['resource_group_name'], storage_account.name)
    AzureActions.remove_dataengine_kernels(notebook_config['resource_group_name'],
                                           notebook_config['notebook_name'], os.environ['conf_os_user'],
                                           notebook_config['key_path'], notebook_config['cluster_name'])


if __name__ == "__main__":
    # generating variables dictionary
    AzureMeta = datalab.meta_lib.AzureMeta()
    AzureActions = datalab.actions_lib.AzureActions()
    logging.info('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['resource_group_name'] = os.environ['azure_resource_group_name']
    notebook_config['service_base_name'] = (os.environ['conf_service_base_name'])
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['edge_user_name'] = (os.environ['edge_user_name'])
    notebook_config['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
    notebook_config['project_tag'] = notebook_config['project_name']
    notebook_config['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
    notebook_config['endpoint_tag'] = notebook_config['endpoint_name']
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-tag'
    notebook_config['computational_name'] = os.environ['computational_name'].replace('_', '-').lower()
    notebook_config['bucket_name'] = '{0}-{1}-{2}-bucket'.format(notebook_config['service_base_name'],
                                                                 notebook_config['project_name'],
                                                                 notebook_config['endpoint_name'])
    notebook_config['cluster_name'] = '{}-{}-{}-des-{}'.format(notebook_config['service_base_name'],
                                                              notebook_config['project_name'],
                                                              notebook_config['endpoint_name'],
                                                              notebook_config['computational_name'])
    notebook_config['storage_account_name_tag'] = ('{}-bucket'.format(notebook_config['cluster_name'])).lower()
    notebook_config['notebook_ip'] = AzureMeta.get_private_ip_address(notebook_config['resource_group_name'],
                                                                      notebook_config['notebook_name'])
    notebook_config['key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    edge_instance_name = '{0}-{1}-{2}-edge'.format(notebook_config['service_base_name'],
                                                   notebook_config['project_name'], notebook_config['endpoint_tag'])
    edge_instance_hostname = AzureMeta.get_private_ip_address(notebook_config['resource_group_name'],
                                                              edge_instance_name)
    for cluster in AzureMeta.list_hdinsight_clusters(notebook_config['resource_group_name']):
        if notebook_config['cluster_name'] == cluster.tags["Name"]:
            notebook_config['full_cluster_name'] = cluster.name
    notebook_config['headnode_ip'] = datalab.fab.get_hdinsight_headnode_private_ip(os.environ['conf_os_user'],
                                                                                   notebook_config['full_cluster_name'],
                                                                                   notebook_config['key_path'])

    if os.environ['application'] == 'deeplearning':
        application = 'jupyter'
    else:
        application = os.environ['application']

    try:
        logging.info('[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]')
        params = "--bucket {} --cluster_name {} --hdinsight_version {} --keyfile {} --notebook_ip {} --region {} " \
                 "--project_name {} --os_user {}  --edge_hostname {} --proxy_port {} " \
                 "--scala_version {} --application {} --headnode_ip {}" \
            .format(notebook_config['storage_account_name_tag'], notebook_config['full_cluster_name'], os.environ['hdinsight_version'],
                    notebook_config['key_path'], notebook_config['notebook_ip'], os.environ['gcp_region'],
                    notebook_config['project_name'], os.environ['conf_os_user'],
                    edge_instance_hostname, '3128', os.environ['notebook_scala_version'], os.environ['application'],
                    notebook_config['headnode_ip'])
        try:
            subprocess.run("~/scripts/{}_{}.py {}".format(application, 'install_dataengine-service_kernels', params), 
                           shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed installing HDinsight kernels.", str(err))
        sys.exit(1)

    try:
        logging.info('[UPDATING SPARK CONFIGURATION FILES ON NOTEBOOK]')
        params = "--hostname {0} " \
                 "--keyfile {1} " \
                 "--os_user {2} " \
            .format(notebook_config['notebook_ip'],
                    notebook_config['key_path'],
                    os.environ['conf_os_user'])
        try:
            subprocess.run("~/scripts/{0}.py {1}".format('common_configure_spark', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure Spark.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Tag_name": notebook_config['tag_name'],
                   "Action": "Configure notebook server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        clear_resources()
        sys.exit(1)
