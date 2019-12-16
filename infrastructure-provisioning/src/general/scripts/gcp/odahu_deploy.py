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

import logging
import json
import sys
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import os

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print('Generating infrastructure names and tags')
    odahu_conf = dict()
    odahu_conf['project_id'] = (os.environ['gcp_project_id'])
    odahu_conf['region'] = (os.environ['gcp_region'])
    odahu_conf['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    odahu_conf['project_name'] = (os.environ['project_name']).lower().replace('_', '-')
    odahu_conf['odahu_cluster_name'] = (os.environ['odahu_cluster_name']).lower().replace('_', '-')
    odahu_conf['bucket_name'] = "{}-tfstate".format((os.environ['odahu_cluster_name']).lower().replace('_', '-'))
    odahu_conf['static_address_name'] = "{}-nat-gw".format((os.environ['odahu_cluster_name']).lower().replace('_', '-'))
    try:
        if os.environ['gcp_vpc_name'] == '':
            raise KeyError
        else:
            odahu_conf['vpc_name'] = os.environ['gcp_vpc_name']
    except KeyError:
        odahu_conf['vpc_name'] = odahu_conf['service_base_name'] + '-ssn-vpc'
    odahu_conf['vpc_cidr'] = os.environ['conf_vpc_cidr']
    odahu_conf['private_subnet_name'] = '{0}-{1}-subnet'.format(odahu_conf['service_base_name'],
                                                               odahu_conf['project_name'])
    odahu_conf['keycloak_realm'] = os.environ['keycloak_realm']
    odahu_conf['keycloak_url'] = os.environ['keycloak_url']\
    odahu_conf['oauth_client_id'] = os.environ['oauth_client_id']
    odahu_conf['oauth_client_secret'] = os.environ['oauth_client_secret']
    odahu_conf['oauth_coockie_secret'] = os.environ['oauth_coockie_secret']
    odahu_conf['tls_crt'] = os.environ['tls_crt']
    odahu_conf['tls_key'] = os.environ['tls_key']

    try:
        local("cp /root/templates/profile.json /tmp/")
        local("sudo sed \'s|<PROJECT_ID>|{}|g\'".format(odahu_conf['project_id']))
        local("sudo sed \'s|<CLUSTER_NAME>|{}|g\'".format(odahu_conf['odahu_cluster_name']))
        local("sudo sed \'s|<REGION>|{}|g\'".format(odahu_conf['region']))
        local("sudo sed \'s|<KEYCLOAK_REALM>|{}|g\'".format(odahu_conf['keycloak_realm']))
        local("sudo sed \'s|<KEYCLOAK_URL>|{}|g\'".format(odahu_conf['keycloak_url']))
        local("sudo sed \'s|<VPC_NAME>|{}|g\'".format(odahu_conf['vpc_name']))
        local("sudo sed \'s|<SUBNET_NAME>|{}|g\'".format(odahu_conf['private_subnet_name']))
        local("sudo sed \'s|<OAUTH_CLIENT_ID>|{}|g\'".format(odahu_conf['oauth_client_id']))
        local("sudo sed \'s|<OAUTH_CLIENT_ID>|{}|g\'".format(odahu_conf['oauth_client_id']))
        local("sudo sed \'s|<TLS_CRT>|{}|g\'".format(odahu_conf['oauth_client_id']))
        local('cp /tmp/profile.json /')
    except Exception as err:
        traceback.print_exc()
        append_result("Failed to configure parameter file.", str(err))
        sys.exit(1)

    try:
        local('tf_runner create')
    except Exception as err:
        traceback.print_exc()
        append_result("Failed to deploy Odahu cluster.", str(err))
        sys.exit(1)
