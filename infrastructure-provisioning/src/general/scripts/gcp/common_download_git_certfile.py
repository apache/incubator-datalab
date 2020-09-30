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

import argparse
import os
from datalab.actions_lib import *
from fabric.api import *

parser = argparse.ArgumentParser()
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    env.hosts = "{}".format(args.notebook_ip)
    env['connection_attempts'] = 100
    env.user = args.os_user
    env.key_filename = "{}".format(args.keyfile)
    env.host_string = env.user + "@" + env.hosts

    bucket_name = ('{0}-{1}-{2}-bucket'.format(os.environ['conf_service_base_name'], os.environ['project_name'],
                                               os.environ['endpoint_name'])).lower().replace('_', '-')
    gitlab_certfile = os.environ['conf_gitlab_certfile']
    if GCPActions().get_gitlab_cert(bucket_name, gitlab_certfile):
        put(gitlab_certfile, gitlab_certfile)
        sudo('chown root:root {}'.format(gitlab_certfile))
        print('{} has been downloaded'.format(gitlab_certfile))
    else:
        print('There is no {} to download'.format(gitlab_certfile))
