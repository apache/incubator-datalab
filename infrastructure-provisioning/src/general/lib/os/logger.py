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
import os
import subprocess

#quick fix for spark configuration, when python script is launched on notebook instance without environment variables and log directories
if 'conf_resource' not in os.environ and 'request_id' not in os.environ:
    os.environ['conf_resource'] = 'undefined_conf_resource'
    os.environ['request_id'] = 'undefined_request_id'
    subprocess.run("sudo mkdir -p /logs/undefined_conf_resource/", shell=True, check=True)

local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                    level=logging.DEBUG,
                    filename='{}'.format(local_log_filepath),
                    filemode='a')
console = logging.StreamHandler()
console.setLevel(logging.INFO)
logging.getLogger('').addHandler(console)
logging.getLogger('googleapiclient.discovery_cache').setLevel(logging.ERROR)