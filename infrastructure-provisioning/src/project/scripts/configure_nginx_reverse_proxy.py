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
import logging
import os
import sys
from datalab.common_lib import ensure_step
from datalab.edge_lib import install_nginx_lua
from fabric import *
from datalab.fab import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--user', type=str, default='')
parser.add_argument('--keycloak_client_id', type=str, default='')
parser.add_argument('--keycloak_client_secret', type=str, default='')
parser.add_argument('--step_cert_sans', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'],
                                               os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print("Configure connections")
    try:
        global conn
        conn = datalab.fab.init_datalab_connection(args.hostname, args.user, args.keyfile)
    except Exception as err:
        print("Failed establish connection. Excpeption: " + str(err))
        sys.exit(1)
    if os.environ['conf_stepcerts_enabled'] == 'true':
        try:
            ensure_step(args.user)
        except Exception as err:
            print("Failed install step: " + str(err))
            sys.exit(1)

    try:
        install_nginx_lua(args.hostname, os.environ['reverse_proxy_nginx_version'],
                          os.environ['keycloak_auth_server_url'], os.environ['keycloak_realm_name'],
                          args.keycloak_client_id, args.keycloak_client_secret, args.user, args.hostname,
                          args.step_cert_sans)
    except Exception as err:
        print("Failed install nginx reverse proxy: " + str(err))
        sys.exit(1)

    conn.close()