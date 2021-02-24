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
import sys
from datalab.actions_lib import *
from datalab.fab import *
from datalab.notebook_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--datalab_path', type=str, default='')
parser.add_argument('--keycloak_auth_server_url', type=str, default='')
parser.add_argument('--keycloak_realm_name', type=str, default='')
parser.add_argument('--keycloak_client_id', type=str, default='')
parser.add_argument('--keycloak_client_secret', type=str, default='')
parser.add_argument('--edge_instance_private_ip', type=str, default='')
parser.add_argument('--edge_instance_public_ip', type=str, default='')
parser.add_argument('--superset_name', type=str, default='')
parser.add_argument('--ip_address', type=str, default='')
args = parser.parse_args()

gitlab_certfile = os.environ['conf_gitlab_certfile']

##############
# Run script #
##############
if __name__ == "__main__":
    print("Configure connections")
    global conn
    conn = datalab.fab.init_datalab_connection(args.hostname, args.os_user, args.keyfile)

    # PREPARE DISK
    print("Prepare .ensure directory")
    try:
        if not exists(conn,'/home/' + args.os_user + '/.ensure_dir'):
            conn.sudo('mkdir /home/' + args.os_user + '/.ensure_dir')
    except:
        sys.exit(1)
    #print("Mount additional volume")
    #prepare_disk(args.os_user)

    # INSTALL DOCKER COMPOSE
    print("Installing docker compose")
    if not ensure_docker_compose(args.os_user):
        sys.exit(1)

    # INSTALL UNGIT
    print("Install nodejs")
    install_nodejs(args.os_user)
    print("Install ungit")
    install_ungit(args.os_user, args.superset_name, args.edge_instance_private_ip)
    if exists(conn, '/home/{0}/{1}'.format(args.os_user, gitlab_certfile)):
        install_gitlab_cert(args.os_user, gitlab_certfile)

        # INSTALL INACTIVITY CHECKER
    print("Install inactivity checker")
    install_inactivity_checker(args.os_user, args.ip_address)

    # PREPARE SUPERSET
    try:
        configure_superset(args.os_user, args.keycloak_auth_server_url, args.keycloak_realm_name,
                           args.keycloak_client_id, args.keycloak_client_secret, args.edge_instance_private_ip, args.edge_instance_public_ip, args.superset_name)
    except:
        sys.exit(1)
    conn.close()


