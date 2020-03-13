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
    dlab.actions_lib.create_aws_config_files()
    print('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = (os.environ['conf_service_base_name'])
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-tag'

    try:
        logging.info('[START NOTEBOOK]')
        print('[START NOTEBOOK]')
        params = "--tag_name {} --nb_tag_value {}".format(notebook_config['tag_name'], notebook_config['notebook_name'])
        try:
            print("Starting notebook")
            dlab.actions_lib.start_ec2(notebook_config['tag_name'], notebook_config['notebook_name'])
        except Exception as err:
            traceback.print_exc()
            dlab.fab.append_result("Failed to start notebook.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        logging.info('[SETUP USER GIT CREDENTIALS]')
        print('[SETUP USER GIT CREDENTIALS]')
        notebook_config['notebook_ip'] = dlab.meta_lib.get_instance_ip_address(
            notebook_config['tag_name'], notebook_config['notebook_name']).get('Private')
        notebook_config['keyfile'] = '{}{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        params = '--os_user {} --notebook_ip {} --keyfile "{}"' \
            .format(os.environ['conf_os_user'], notebook_config['notebook_ip'], notebook_config['keyfile'])
        try:
            local("~/scripts/{}.py {}".format('manage_git_creds', params))
        except Exception as err:
            traceback.print_exc()
            dlab.fab.append_result("Failed to setup git credentials.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        logging.info('[UPDATE LAST ACTIVITY TIME]')
        print('[UPDATE LAST ACTIVITY TIME]')
        params = '--os_user {} --notebook_ip {} --keyfile "{}"' \
            .format(os.environ['conf_os_user'], notebook_config['notebook_ip'], notebook_config['keyfile'])
        try:
            local("~/scripts/{}.py {}".format('update_inactivity_on_start', params))
        except Exception as err:
            traceback.print_exc()
            dlab.fab.append_result("Failed to update last activity time.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        ip_address = dlab.meta_lib.get_instance_ip_address(notebook_config['tag_name'],
                                                           notebook_config['notebook_name']).get('Private')
        dns_name = dlab.meta_lib.get_instance_hostname(notebook_config['tag_name'], notebook_config['notebook_name'])
        print('[SUMMARY]')
        logging.info('[SUMMARY]')
        print("Instance name: {}".format(notebook_config['notebook_name']))
        print("Private DNS: {}".format(dns_name))
        print("Private IP: {}".format(ip_address))
        with open("/root/result.json", 'w') as result:
            res = {"hostname": dns_name,
                   "ip": ip_address,
                   "notebook_name": notebook_config['notebook_name'],
                   "Tag_name": notebook_config['tag_name'],
                   "Action": "Start up notebook server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)


