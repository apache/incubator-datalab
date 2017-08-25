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


def terminate_data_engine(resource_group_name, master_instance_name, slave_instance_name):
    print "Terminating Master node"
    try:
        AzureActions().remove_instance(resource_group_name, master_instance_name)
    except:
        sys.exit(1)
    print "Terminating slave nodes"
    try:
        for vm in AzureMeta().compute_client.virtual_machines.list(resource_group_name):
            if slave_instance_name in vm.name:
                AzureActions().remove_instance(resource_group_name, vm.name)
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
    data_engine = dict()
    data_engine['service_base_name'] = os.environ['conf_service_base_name']
    data_engine['resource_group_name'] = os.environ['azure_resource_group_name']
    data_engine['master_node_name'] = data_engine['service_base_name'] + '-' + os.environ['edge_user_name'] + \
                                      '-dataengine-' + data_engine['exploratory_name'] + '-' + \
                                      data_engine['computational_name'] + '-master'
    data_engine['slave_node_name'] = data_engine['service_base_name'] + '-' + os.environ['edge_user_name'] + \
                                      '-dataengine-' + data_engine['exploratory_name'] + '-' + \
                                      data_engine['computational_name'] + '-slave'

    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['user_name'] = os.environ['edge_user_name']

    try:
        logging.info('[TERMINATE DATA ENGINE]')
        print '[TERMINATE DATA ENGINE]'
        try:
            terminate_data_engine(data_engine['resource_group_name'], data_engine['master_node_name'],
                                  data_engine['slave_node_name'])
        except Exception as err:
            traceback.print_exc()
            append_result("Failed to terminate Data Engine.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": data_engine['service_base_name'],
                   "Action": "Terminate Data Engine"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)