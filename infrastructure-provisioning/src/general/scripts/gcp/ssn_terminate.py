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

import dlab.fab
import dlab.actions_lib
import dlab.meta_lib
import dlab.ssn_lib
import sys
import os
import logging
import json
import traceback
from fabric.api import *

if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    # generating variables dictionary
    print('Generating infrastructure names and tags')
    ssn_conf = dict()
    ssn_conf['service_base_name'] = dlab.fab.replace_multi_symbols(
        os.environ['conf_service_base_name'].replace('_', '-').lower()[:20], '-', True)
    ssn_conf['region'] = os.environ['gcp_region']
    ssn_conf['zone'] = os.environ['gcp_zone']
    pre_defined_vpc = False
    try:
        if os.environ['gcp_vpc_name'] == '':
            raise KeyError
        else:
            pre_defined_vpc = True
            ssn_conf['vpc_name'] = os.environ['gcp_vpc_name']
    except KeyError:
        ssn_conf['vpc_name'] = '{}-vpc'.format(ssn_conf['service_base_name'])

    try:
        logging.info('[TERMINATE SSN]')
        print('[TERMINATE SSN]')
        params = "--service_base_name {} --region {} --zone {} --pre_defined_vpc {} --vpc_name {}".format(
            ssn_conf['service_base_name'], ssn_conf['region'], ssn_conf['zone'], pre_defined_vpc, ssn_conf['vpc_name'])
        try:
            local("~/scripts/{}.py {}".format('ssn_terminate_gcp_resources', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed to terminate ssn.", str(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": ssn_conf['service_base_name'],
                   "Action": "Terminate ssn with all service_base_name environment"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
