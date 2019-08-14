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

import logging
import json
import sys
from dlab.fab import *
from dlab.actions_lib import *
import os


def terminate_data_engine(tag_name, notebook_name,
                          os_user, key_path,
                          cluster_name, remote_kernel_name):
    print("Terminating data engine cluster")
    try:
        remove_ec2(os.environ['conf_tag_resource_id'], cluster_name)
    except:
        sys.exit(1)

    print("Removing Data Engine kernels from notebook")
    try:
        remove_dataengine_kernels(tag_name, notebook_name,
                                  os_user, key_path, remote_kernel_name)
    except:
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'],
                                               os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + \
                         os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    # generating variables dictionary
    print('Generating infrastructure names and tags')
    create_aws_config_files()
    data_engine = dict()
    
    try:
        data_engine['exploratory_name'] = os.environ['exploratory_name']
    except:
        data_engine['exploratory_name'] = ''
    try:
        data_engine['computational_name'] = os.environ['computational_name']
    except:
        data_engine['computational_name'] = ''
    data_engine['service_base_name'] = os.environ['conf_service_base_name'] = replace_multi_symbols(
            os.environ['conf_service_base_name'].lower()[:12], '-', True)
    data_engine['tag_name'] = data_engine['service_base_name'] + '-Tag'
    data_engine['project_name'] = os.environ['project_name']
    data_engine['cluster_name'] = \
        data_engine['service_base_name'] + '-' + \
        data_engine['project_name'] + '-de-' + \
        data_engine['exploratory_name'] + '-' +\
        data_engine['computational_name']
    data_engine['notebook_name'] = os.environ['notebook_instance_name']
    data_engine['key_path'] = os.environ['conf_key_dir'] + '/' + \
                              os.environ['conf_key_name'] + '.pem'

    try:
        logging.info('[TERMINATE DATA ENGINE]')
        print('[TERMINATE DATA ENGINE]')
        try:
            terminate_data_engine(data_engine['tag_name'],
                                  data_engine['notebook_name'],
                                  os.environ['conf_os_user'],
                                  data_engine['key_path'], "{}:{}".format(
                    data_engine['service_base_name'],
                    data_engine['cluster_name']), data_engine['cluster_name'])
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
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)