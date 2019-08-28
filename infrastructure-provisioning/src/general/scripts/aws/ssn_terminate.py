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

from dlab.fab import *
from dlab.actions_lib import *
import sys, os
from fabric.api import *
from dlab.ssn_lib import *

if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    # generating variables dictionary
    if 'aws_access_key' in os.environ and 'aws_secret_access_key' in os.environ:
        create_aws_config_files(generate_full_config=True)
    else:
        create_aws_config_files()
    print('Generating infrastructure names and tags')
    ssn_conf = dict()
    ssn_conf['service_base_name'] = os.environ['conf_service_base_name'] = replace_multi_symbols(
            os.environ['conf_service_base_name'].lower()[:12], '-', True)
    ssn_conf['tag_name'] = ssn_conf['service_base_name'] + '-Tag'
    ssn_conf['edge_sg'] = ssn_conf['service_base_name'] + "*" + '-edge'
    ssn_conf['nb_sg'] = ssn_conf['service_base_name'] + "*" + '-nb'
    ssn_conf['de_sg'] = ssn_conf['service_base_name'] + "*" + '-dataengine*'
    ssn_conf['de-service_sg'] = ssn_conf['service_base_name'] + "*" + '-des-*'

    try:
        logging.info('[TERMINATE SSN]')
        print('[TERMINATE SSN]')
        params = "--tag_name {} --edge_sg {} --nb_sg {} --de_sg {} --service_base_name {} --de_se_sg {}". \
                 format(ssn_conf['tag_name'], ssn_conf['edge_sg'], ssn_conf['nb_sg'], ssn_conf['de_sg'],
                        ssn_conf['service_base_name'], ssn_conf['de-service_sg'])
        try:
            local("~/scripts/{}.py {}".format('ssn_terminate_aws_resources', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to terminate ssn.", str(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": ssn_conf['service_base_name'],
                   "Action": "Terminate ssn with all service_base_name environment"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)