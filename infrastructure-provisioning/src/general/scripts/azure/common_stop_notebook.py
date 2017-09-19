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
import uuid
from dlab.meta_lib import *
from dlab.actions_lib import *
import argparse
import sys


def stop_notebook(resource_group_name, notebook_name, os_user, key_path):
    print "Terminating data engine cluster"
    cluster_list = []
    try:
        for vm in AzureMeta().compute_client.virtual_machines.list(resource_group_name):
            if notebook_name == vm.tags['notebook_name']:
                if '-master' in vm.name:
                    cluster_list.append(vm.name.replace('-master', ''))
                AzureActions().remove_instance(resource_group_name, vm.name)
                print "Instance {} has been terminated".format(vm.name)
    except:
        sys.exit(1)

    print "Removing Data Engine kernels from notebook"
    try:
        for cluster_name in cluster_list:
            AzureActions().remove_dataengine_kernels(resource_group_name, notebook_name, os_user, key_path,
                                                     cluster_name)
    except:
        sys.exit(1)

    print "Stopping notebook"
    try:
        for vm in AzureMeta().compute_client.virtual_machines.list(resource_group_name):
            if notebook_name in vm.name:
                AzureActions().stop_instance(resource_group_name, vm.name)
                print "Instance {} has been terminated".format(vm.name)
    except:
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    print 'Generating infrastructure names and tags'
    notebook_config = dict()
    try:
        notebook_config['exploratory_name'] = os.environ['exploratory_name'].replace('_', '-')
    except:
        notebook_config['exploratory_name'] = ''
    try:
        notebook_config['computational_name'] = os.environ['computational_name'].replace('_', '-')
    except:
        notebook_config['computational_name'] = ''
    notebook_config['service_base_name'] = os.environ['conf_service_base_name']
    notebook_config['resource_group_name'] = os.environ['azure_resource_group_name']
    notebook_config['user_name'] = os.environ['edge_user_name'].replace('_', '-')
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['key_path'] = os.environ['conf_key_dir'] + '/' + os.environ['conf_key_name'] + '.pem'

    logging.info('[STOP NOTEBOOK]')
    print '[STOP NOTEBOOK]'
    try:
        stop_notebook(notebook_config['resource_group_name'], notebook_config['notebook_name'],
                     os.environ['conf_os_user'], notebook_config['key_path'])
    except Exception as err:
        append_result("Failed to stop notebook.", str(err))
        sys.exit(1)


    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Action": "Stop notebook server"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)

