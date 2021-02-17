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


import os
import sys
import subprocess
from datalab.actions_lib import *
from datalab.fab import *
from datalab.meta_lib import *

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        logging.info('[ASK INACTIVITY STATUS]')
        print('[ASK INACTIVITY STATUS]')
        notebook_config = dict()
        try:
            notebook_config['notebook_name'] = os.environ['notebook_instance_name']
            notebook_config['os_user'] = os.environ['conf_os_user']
            notebook_config['resource_type'] = os.environ['conf_resource']
            notebook_config['service_base_name'] = os.environ['conf_service_base_name'].lower()
            notebook_config['tag_name'] = notebook_config['service_base_name'] + '-tag'
            notebook_config['notebook_ip'] = get_instance_private_ip_address(
                notebook_config['tag_name'], notebook_config['notebook_name'])
            notebook_config['keyfile'] = '{}{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
            if notebook_config['resource_type'] == 'dataengine':
                notebook_config['dataengine_name'] = '{}-m'.format(os.environ['computational_id'])
                notebook_config['dataengine_ip'] = get_instance_private_ip_address(
                    notebook_config['tag_name'], notebook_config['dataengine_name'])
            else:
                notebook_config['dataengine_ip'] = '0.0.0.0'
        except Exception as err:
            print('Error: {0}'.format(err))
            append_result("Failed to get parameter.", str(err))
            sys.exit(1)
        params = "--os_user {0} --instance_ip {1} --keyfile '{2}' --resource_type {3} --dataengine_ip {4}" \
            .format(notebook_config['os_user'], notebook_config['notebook_ip'], notebook_config['keyfile'], notebook_config['resource_type'], notebook_config['dataengine_ip'])
        try:
            # Run script to get available libs
            subprocess.run("~/scripts/{}.py {}".format('check_inactivity', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to ask inactivity status.", str(err))
        sys.exit(1)