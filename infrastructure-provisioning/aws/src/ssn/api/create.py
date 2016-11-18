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

import os
import json
import sys
from fabric.api import local


if __name__ == "__main__":
    success = True
    try:
        local('cd /root; fab run')
    except:
        success = False

    reply = dict()
    reply['request_id'] = os.environ['request_id']
    if success:
        reply['status'] = 'ok'
    else:
        reply['status'] = 'err'

    reply['response'] = dict()

    try:
        with open("/root/result.json") as f:
            reply['response']['result'] = json.loads(f.read())
    except:
        reply['response']['result'] = {"error": "Failed to open result.json"}

    reply['response']['log'] = "/response/%s.log" % os.environ['request_id']

    with open("/response/%s.json" % os.environ['request_id'], 'w') as response_file:
        response_file.write(json.dumps(reply))

    if not success:
        sys.exit(1)