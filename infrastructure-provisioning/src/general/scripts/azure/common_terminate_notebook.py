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
import traceback


def terminate_nb(resource_group_name, notebook_name):
    logging.info("Terminating data engine cluster")
    try:
        for vm in AzureMeta.compute_client.virtual_machines.list(resource_group_name):
            if "notebook_name" in vm.tags:
                if notebook_name == vm.tags['notebook_name']:
                    AzureActions.remove_instance(resource_group_name, vm.name)
                    logging.info("Instance {} has been terminated".format(vm.name))
    except Exception as err:
        datalab.fab.append_result("Failed to terminate clusters", str(err))
        sys.exit(1)

    logging.info("Terminating notebook")
    try:
        for vm in AzureMeta.compute_client.virtual_machines.list(resource_group_name):
            if "Name" in vm.tags:
                if notebook_name == vm.tags["Name"]:
                    AzureActions.remove_instance(resource_group_name, vm.name)
                    logging.info("Instance {} has been terminated".format(vm.name))
    except Exception as err:
        datalab.fab.append_result("Failed to terminate instance", str(err))
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

    try:
        logging.info('[TERMINATE NOTEBOOK]')
        try:
            terminate_nb(notebook_config['resource_group_name'], notebook_config['notebook_name'])
        except Exception as err:
            traceback.print_exc()
            datalab.fab.append_result("Failed to terminate notebook.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Action": "Terminate notebook server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
