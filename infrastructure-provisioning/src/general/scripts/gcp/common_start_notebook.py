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

if __name__ == "__main__":
    # generating variables dictionary
    GCPMeta = datalab.meta_lib.GCPMeta()
    GCPActions = datalab.actions_lib.GCPActions()
    logging.info('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = (os.environ['conf_service_base_name'])
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['zone'] = os.environ['gcp_zone']
    if "gcp_wrapped_csek" in os.environ:
        notebook_config['gcp_wrapped_csek'] = os.environ['gcp_wrapped_csek']
    else:
        notebook_config['gcp_wrapped_csek'] = ''

    try:
        logging.info('[START NOTEBOOK]')
        try:
            logging.info("Starting notebook")
            GCPActions.start_instance(notebook_config['notebook_name'], notebook_config['zone'],
                                      notebook_config['gcp_wrapped_csek'])
        except Exception as err:
            traceback.print_exc()
            datalab.fab.append_result("Failed to start notebook.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        logging.info('[SETUP USER GIT CREDENTIALS]')
        notebook_config['notebook_ip'] = GCPMeta.get_private_ip_address(notebook_config['notebook_name'])
        notebook_config['keyfile'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        params = '--os_user {} --notebook_ip {} --keyfile "{}"' \
            .format(os.environ['conf_os_user'], notebook_config['notebook_ip'], notebook_config['keyfile'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('manage_git_creds', params), shell=True, check=True)
        except Exception as err:
            traceback.print_exc()
            datalab.fab.append_result("Failed to setup git credentials.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        logging.info('[UPDATE LAST ACTIVITY TIME]')
        params = '--os_user {} --notebook_ip {} --keyfile "{}"' \
            .format(os.environ['conf_os_user'], notebook_config['notebook_ip'], notebook_config['keyfile'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('update_inactivity_on_start', params), shell=True, check=True)
        except Exception as err:
            traceback.print_exc()
            datalab.fab.append_result("Failed to update last activity time.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        logging.info('[SUMMARY]')
        logging.info("Instance name: {}".format(notebook_config['notebook_name']))
        logging.info("Private IP: {}".format(notebook_config['notebook_ip']))
        with open("/root/result.json", 'w') as result:
            res = {"hostname": notebook_config['notebook_ip'],
                   "ip": notebook_config['notebook_ip'],
                   "notebook_name": notebook_config['notebook_name'],
                   "Action": "Start up notebook server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
