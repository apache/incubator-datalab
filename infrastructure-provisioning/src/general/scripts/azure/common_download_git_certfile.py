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
from datalab.meta_lib import *
from datalab.logger import logging
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()

resource_group_name = os.environ['azure_resource_group_name']
ssn_storage_account_tag = ('{0}-{1}-{2}-bucket'.format(os.environ['conf_service_base_name'], os.environ['project_name'],
                                                       os.environ['endpoint_name']))
container_name = ('{}-ssn-bucket'.format(os.environ['conf_service_base_name'])).lower().replace('_', '-')
gitlab_certfile = os.environ['conf_gitlab_certfile']

if __name__ == "__main__":
    global conn
    conn = datalab.fab.init_datalab_connection(args.notebook_ip, args.os_user, args.keyfile)

    for storage_account in AzureMeta().list_storage_accounts(resource_group_name):
        if ssn_storage_account_tag == storage_account.tags["Name"]:
            ssn_storage_account_name = storage_account.name
    if AzureActions().download_from_container(resource_group_name, ssn_storage_account_name, container_name, gitlab_certfile):
        conn.put(gitlab_certfile, gitlab_certfile)
        conn.sudo('chown root:root {}'.format(gitlab_certfile))
        logging.info('{} has been downloaded'.format(gitlab_certfile))
    else:
        logging.info('There is no {} to download'.format(gitlab_certfile))

    conn.close()