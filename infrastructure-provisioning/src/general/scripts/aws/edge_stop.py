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
import sys


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    create_aws_config_files()
    print('Generating infrastructure names and tags')
    edge_conf = dict()
    edge_conf['service_base_name'] = os.environ['conf_service_base_name'] = replace_multi_symbols(
            os.environ['conf_service_base_name'].lower()[:12], '-', True)
    edge_conf['instance_name'] = edge_conf['service_base_name'] + "-" + os.environ['project_name'] + '-edge'
    edge_conf['tag_name'] = edge_conf['service_base_name'] + '-Tag'

    logging.info('[STOP EDGE]')
    print('[STOP EDGE]')
    try:
        stop_ec2(edge_conf['tag_name'], edge_conf['instance_name'])
    except Exception as err:
        append_result("Failed to stop edge.", str(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"instance_name": edge_conf['instance_name'],
                   "Action": "Stop edge server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)

