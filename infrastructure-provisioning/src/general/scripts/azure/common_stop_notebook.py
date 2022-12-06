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

import datalab.actions_lib
import datalab.fab
import datalab.meta_lib
import json
from datalab.logger import logging
import os
import sys


def stop_notebook(resource_group_name, notebook_name):
    logging.info("Terminating Dataengine-service clusters")
    try:
        clusters_list = AzureMeta.list_hdinsight_clusters(resource_group_name)
        if clusters_list:
            for cluster in clusters_list:
                if "notebook_name" in cluster.tags and notebook_name == cluster.tags["notebook_name"]:
                    AzureActions.terminate_hdinsight_cluster(resource_group_name, cluster.name)
                    logging.info('The HDinsight cluster {} has been terminated successfully'.format(cluster.name))
        else:
            logging.info("There are no HDinsight clusters to terminate.")
    except Exception as err:
        datalab.fab.append_result("Failed to terminate dataengine-service", str(err))
        sys.exit(1)

    logging.info("Stopping data engine cluster")
    cluster_list = []
    try:
        for vm in AzureMeta.compute_client.virtual_machines.list(resource_group_name):
            if "notebook_name" in vm.tags:
                if notebook_name == vm.tags['notebook_name']:
                    if 'master' == vm.tags["Type"]:
                        cluster_list.append(vm.tags["Name"])
                    AzureActions.stop_instance(resource_group_name, vm.name)
                    logging.info("Instance {} has been stopped".format(vm.name))
    except Exception as err:
        datalab.fab.append_result("Failed to stop clusters", str(err))
        sys.exit(1)

    logging.info("Stopping notebook")
    try:
        for vm in AzureMeta.compute_client.virtual_machines.list(resource_group_name):
            if "Name" in vm.tags:
                if notebook_name == vm.tags["Name"]:
                    AzureActions.stop_instance(resource_group_name, vm.name)
                    logging.info("Instance {} has been stopped".format(vm.name))
    except Exception as err:
        datalab.fab.append_result("Failed to stop instance", str(err))
        sys.exit(1)


if __name__ == "__main__":
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
    notebook_config['resource_group_name'] = os.environ['azure_resource_group_name']
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']

    logging.info('[STOP NOTEBOOK]')
    try:
        stop_notebook(notebook_config['resource_group_name'], notebook_config['notebook_name'])
    except Exception as err:
        datalab.fab.append_result("Failed to stop notebook.", str(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Action": "Stop notebook server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
