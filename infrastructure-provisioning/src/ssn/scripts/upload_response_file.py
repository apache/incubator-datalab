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

import sys
import argparse
import logging
from dlab.ssn_lib import *


parser = argparse.ArgumentParser()
parser.add_argument('--instance_name', type=str, default='')
parser.add_argument('--instance_hostname', type=str, default='')
parser.add_argument('--local_log_filepath', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()


def upload_response_file(instance_name, local_log_filepath, os_user):
    print('Connect to SSN instance with hostname: {0} and name: {1}'.format(args.instance_hostname, instance_name))
    env['connection_attempts'] = 100
    env.key_filename = "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    env.host_string = '{}@{}'.format(os_user, args.instance_hostname)
    try:
        put('/root/result.json', '/home/{}/{}.json'.format(os_user, os.environ['request_id']))
        sudo('mv /home/{}/{}.json {}tmp/result/'.format(os_user, os.environ['request_id'], os.environ['ssn_dlab_path']))
        put(local_log_filepath, '/home/{}/ssn.log'.format(os_user))
        sudo('mv /home/{}/ssn.log /var/opt/dlab/log/ssn/'.format(os_user))
        return True
    except:
        print('Failed to upload response file')
        return False


if __name__ == "__main__":
    print("Uploading response file")
    if not upload_response_file(args.instance_name, args.local_log_filepath, args.os_user):
        logging.error('Failed to upload response file')
        sys.exit(1)