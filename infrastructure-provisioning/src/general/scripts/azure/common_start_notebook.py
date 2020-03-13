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
import dlab.fab
import dlab.actions_lib
import dlab.meta_lib
import traceback
import os
import uuid
import argparse
from fabric.api import *


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    # generating variables dictionary
    AzureMeta = dlab.meta_lib.AzureMeta()
    AzureActions = dlab.actions_lib.AzureActions()
    print('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = os.environ['conf_service_base_name']
    notebook_config['resource_group_name'] = os.environ['azure_resource_group_name']
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']

    try:
        logging.info('[START NOTEBOOK]')
        print('[START NOTEBOOK]')
        try:
            print("Starting notebook")
            AzureActions.start_instance(notebook_config['resource_group_name'], notebook_config['notebook_name'])
            print("Instance {} has been started".format(notebook_config['notebook_name']))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed to start notebook.", str(err))
        sys.exit(1)

    try:
        logging.info('[SETUP USER GIT CREDENTIALS]')
        print('[SETUP USER GIT CREDENTIALS]')
        notebook_config['notebook_ip'] = AzureMeta.get_private_ip_address(
            notebook_config['resource_group_name'], notebook_config['notebook_name'])
        notebook_config['keyfile'] = '{}{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        params = '--os_user {} --notebook_ip {} --keyfile "{}"' \
            .format(os.environ['conf_os_user'], notebook_config['notebook_ip'], notebook_config['keyfile'])
        try:
            local("~/scripts/{}.py {}".format('manage_git_creds', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed to setup git credentials.", str(err))
        sys.exit(1)

    if os.environ['azure_datalake_enable'] == 'true':
        try:
            logging.info('[UPDATE STORAGE CREDENTIALS]')
            print('[UPDATE STORAGE CREDENTIALS]')
            notebook_config['notebook_ip'] = AzureMeta.get_private_ip_address(
                notebook_config['resource_group_name'], notebook_config['notebook_name'])
            env.hosts = "{}".format(notebook_config['notebook_ip'])
            env.user = os.environ['conf_os_user']
            env.key_filename = "{}".format(notebook_config['keyfile'])
            env.host_string = env.user + "@" + env.hosts
            params = '--refresh_token {}'.format(os.environ['azure_user_refresh_token'])
            try:
                put('~/scripts/common_notebook_update_refresh_token.py', '/tmp/common_notebook_update_refresh_token.py')
                sudo('mv /tmp/common_notebook_update_refresh_token.py '
                     '/usr/local/bin/common_notebook_update_refresh_token.py')
                sudo("/usr/bin/python /usr/local/bin/{}.py {}".format('common_notebook_update_refresh_token', params))
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            dlab.fab.append_result("Failed to update storage credentials.", str(err))
            sys.exit(1)

    try:
        logging.info('[UPDATE LAST ACTIVITY TIME]')
        print('[UPDATE LAST ACTIVITY TIME]')
        params = '--os_user {} --notebook_ip {} --keyfile "{}"' \
            .format(os.environ['conf_os_user'], notebook_config['notebook_ip'], notebook_config['keyfile'])
        try:
            local("~/scripts/{}.py {}".format('update_inactivity_on_start', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed to update last activity time.", str(err))
        sys.exit(1)

    try:
        ip_address = AzureMeta.get_private_ip_address(notebook_config['resource_group_name'],
                                                      notebook_config['notebook_name'])
        print('[SUMMARY]')
        logging.info('[SUMMARY]')
        print("Instance name: {}".format(notebook_config['notebook_name']))
        print("Private IP: {}".format(ip_address))
        with open("/root/result.json", 'w') as result:
            res = {"ip": ip_address,
                   "notebook_name": notebook_config['notebook_name'],
                   "Action": "Start up notebook server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)


