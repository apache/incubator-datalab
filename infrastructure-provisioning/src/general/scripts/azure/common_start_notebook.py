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
import argparse


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
    notebook_config['service_base_name'] = os.environ['conf_service_base_name']
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']

    try:
        logging.info('[START NOTEBOOK]')
        print '[START NOTEBOOK]'
        try:
            print "Starting notebook"
            AzureActions().start_instance(notebook_config['service_base_name'], notebook_config['notebook_name'])
        except Exception as err:
            traceback.print_exc()
            append_result("Failed to start notebook.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        logging.info('[SETUP USER GIT CREDENTIALS]')
        print '[SETUP USER GIT CREDENTIALS]'
        notebook_config['notebook_ip'] = AzureMeta().get_instance_private_ip_address(
            notebook_config['service_base_name'], notebook_config['notebook_name'])
        notebook_config['keyfile'] = '{}{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        params = '--os_user {} --notebook_ip {} --keyfile "{}"' \
            .format(os.environ['conf_os_user'], notebook_config['notebook_ip'], notebook_config['keyfile'])
        try:
            local("~/scripts/{}.py {}".format('manage_git_creds', params))
        except Exception as err:
            traceback.print_exc()
            append_result("Failed to setup git credentials.", str(err))
            raise Exception
    except:
        sys.exit(1)


    try:
        ip_address = AzureMeta().get_instance_private_ip_address(notebook_config['service_base_name'],
                                                                 notebook_config['notebook_name'])
        print '[SUMMARY]'
        logging.info('[SUMMARY]')
        print "Instance name: " + notebook_config['notebook_name']
        print "Private IP: " + ip_address
        with open("/root/result.json", 'w') as result:
            res = {"ip": ip_address,
                   "notebook_name": notebook_config['notebook_name'],
                   "Action": "Start up notebook server"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)


