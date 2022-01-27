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

import argparse
import os
from datalab.actions_lib import *
from fabric import *
from datalab.fab import replace_multi_symbols
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    create_aws_config_files()
    global conn
    conn = datalab.fab.init_datalab_connection(args.notebook_ip, args.os_user, args.keyfile)

    service_base_name = os.environ['conf_service_base_name'] = replace_multi_symbols(
        os.environ['conf_service_base_name'][:20], '-', True)
    project_name = os.environ['project_name']
    endpoint_name = os.environ['endpoint_name']
    bucket_name = ('{0}-{1}-{2}-bucket'.format(service_base_name,
                                               project_name, endpoint_name)).lower().replace('_', '-')
    gitlab_certfile = os.environ['conf_gitlab_certfile']
    if datalab.actions_lib.get_gitlab_cert(bucket_name, gitlab_certfile):
        conn.put(gitlab_certfile, gitlab_certfile)
        conn.sudo('chown root:root {}'.format(gitlab_certfile))
        logging.info('{} has been downloaded'.format(gitlab_certfile))
    else:
        logging.info('There is no {} to download'.format(gitlab_certfile))

    conn.close()
