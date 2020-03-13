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

import os
import sys
import logging
import traceback
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
from fabric.api import *


if __name__ == "__main__":
    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        logging.info('[INSTALLING ADDITIONAL LIBRARIES ON NOTEBOOK INSTANCE]')
        print('[INSTALLING ADDITIONAL LIBRARIES ON NOTEBOOK INSTANCE]')
        notebook_config = dict()
        try:
            notebook_config['notebook_name'] = os.environ['notebook_instance_name']
            notebook_config['os_user'] = os.environ['conf_os_user']
            notebook_config['service_base_name'] = os.environ['conf_service_base_name'].lower()
            notebook_config['tag_name'] = notebook_config['service_base_name'] + '-tag'
            notebook_config['notebook_ip'] = get_instance_private_ip_address(
                notebook_config['tag_name'], notebook_config['notebook_name'])
            notebook_config['keyfile'] = '{}{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
            notebook_config['libs'] = os.environ['libs']
        except Exception as err:
            print('Error: {0}'.format(err))
            append_result("Failed to get parameter.", str(err))
            sys.exit(1)
        params = '--os_user {} --instance_ip {} --keyfile "{}" --libs "{}"' \
            .format(notebook_config['os_user'], notebook_config['notebook_ip'],
                    notebook_config['keyfile'], notebook_config['libs'])
        try:
            # Run script to install additional libs
            local("~/scripts/{}.py {}".format('install_additional_libs', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to install additional libraries.", str(err))
        sys.exit(1)
