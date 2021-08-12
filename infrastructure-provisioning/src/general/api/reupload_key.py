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

import json
import os
import sys
import subprocess

if __name__ == "__main__":
    success = True
    try:
        subprocess.run('cd /root; fab reupload_key', shell=True, check=True)
    except:
        success = False

    reply = dict()
    reply['request_id'] = os.environ['request_id']
    if success:
        reply['status'] = 'ok'
        reply['error_message'] = ''
    else:
        reply['status'] = 'failed'
        try:
            with open("/root/result.json") as f:
                result = json.loads(f.read())
                reply['error_message'] = result['error']
        except:
            result = {"error": "Failed to open result.json"}
            reply['error_message'] = 'Failed to open result.json'

    log = "/var/log/datalab/edge/{0}_{1}_{2}.log".format(os.environ['conf_resource'],
                                                         os.environ['project_name'],
                                                         os.environ['request_id'])
    try:
        with open("/response/{}_{}_{}.json".format(os.environ['conf_resource'], os.environ['project_name'],
                                                   os.environ['request_id']), 'w') as response_file:
            response_file.write(json.dumps(reply))
            print(json.dumps(reply))
    except:
        print('Can not write to responce')
    try:
        subprocess.run('chmod 666 /response/*', shell=True, check=True)
    except:
        success = False

    if not success:
        sys.exit(1)