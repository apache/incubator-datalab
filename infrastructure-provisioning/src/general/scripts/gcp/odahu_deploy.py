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
import base64

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)



    print('Generating infrastructure names and tags')
    odahu_conf = dict()
    odahu_conf['allowed_cidr'] = json.dumps(os.environ['odahu_allowed_cidr'].split(','))
    odahu_conf['bastion_tag'] = os.environ['odahu_bastion_tag']
    odahu_conf['project_id'] = (os.environ['gcp_project_id'])
    odahu_conf['region'] = (os.environ['gcp_region'])
    odahu_conf['zone'] = (os.environ['gcp_zone'])
    odahu_conf['node_locations'] = json.dumps(GCPMeta().get_available_zones())
    odahu_conf['dns_zone_name'] = os.environ['odahu_dns_zone_name']
    odahu_conf['docker_repo'] = os.environ['odahu_docker_repo']
    odahu_conf['odahu_cidr'] = os.environ['odahu_cidr']
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
    odahu_conf['grafana_admin'] = os.environ['odahu_grafana_admin']
    odahu_conf['grafana_pass'] = id_generator()
    odahu_conf['initial_node_count'] = os.environ['odahu_initial_node_count']
    odahu_conf['istio_helm_repo'] = os.environ['odahu_istio_helm_repo']
    odahu_conf['helm_repo'] = os.environ['odahu_helm_repo']
    odahu_conf['k8s_version'] = os.environ['odahu_k8s_version']
    odahu_conf['oauth_oidc_issuer_url'] = "{}/realms/{}".format(os.environ['keycloak_auth_server_url'], os.environ['keycloak_realm_name'])
    odahu_conf['oauth_client_id'] = os.environ['keycloak_client_name']
    odahu_conf['oauth_client_secret'] = os.environ['keycloak_client_secret']
    odahu_conf['oauth_cookie_secret'] = id_generator()
    odahu_conf['odahu_infra_version'] = os.environ['odahu_infra_version']
    odahu_conf['odahuflow_version'] = os.environ['odahu_odahuflow_version']
    odahu_conf['mlflow_toolchain_version'] = os.environ['odahu_mlflow_toolchain_version']
    odahu_conf['jupyterlab_version'] = os.environ['odahu_jupyterlab_version']
    odahu_conf['packager_version'] = os.environ['odahu_packager_version']
    odahu_conf['node_version'] = os.environ['odahu_node_version']
    odahu_conf['pods_cidr'] = os.environ['odahu_pods_cidr']
    odahu_conf['root_domain'] = os.environ['odahu_root_domain']
    odahu_conf['service_cidr'] = os.environ['odahu_service_cidr']
    odahu_conf['tls_crt'] = base64.b64decode(os.environ['odahu_tls_crt'] + "==")
    odahu_conf['tls_key'] = base64.b64decode(os.environ['odahu_tls_key'] + "==")
    odahu_conf['ssh_key'] = os.environ['ssh_key']
    odahu_conf['dns_project_id'] = os.environ['odahu_dns_project_id']
    odahu_conf['decrypt_token'] = id_generator()
    odahu_conf['infra_vpc_peering'] = os.environ['odahu_infra_vpc_peering']
    response_file ="/response/odahu_{}_{}.json".format(odahu_conf['odahu_cluster_name'], os.environ['request_id'])

    print('Preparing parameters file')
    try:
        local("cp /root/templates/profile.json /tmp/")
        local("sed -i \'s|<ALLOWED_IP_CIDR>|{}|g\' /tmp/profile.json".format(odahu_conf['allowed_cidr']))
        local("sed -i \'s|<BASTION_TAG>|{}|g\' /tmp/profile.json".format(odahu_conf['bastion_tag']))
        local("sed -i \'s|<PROJECT_ID>|{}|g\' /tmp/profile.json".format(odahu_conf['project_id']))
        local("sed -i \'s|<CLUSTER_NAME>|{}|g\' /tmp/profile.json".format(odahu_conf['odahu_cluster_name']))
        local("sed -i \'s|<REGION>|{}|g\' /tmp/profile.json".format(odahu_conf['region']))
        local("sed -i \'s|<ZONE>|{}|g\' /tmp/profile.json".format(odahu_conf['zone']))
        local("sed -i \'s|<DNS_ZONE_NAME>|{}|g\' /tmp/profile.json".format(odahu_conf['dns_zone_name']))
        local("sed -i \'s|<DOCKER_REPO>|{}|g\' /tmp/profile.json".format(odahu_conf['docker_repo']))
        local("sed -i \'s|<ODAHU_CIDR>|{}|g\' /tmp/profile.json".format(odahu_conf['odahu_cidr']))
        local("sed -i \'s|<GRAFANA_ADMIN>|{}|g\' /tmp/profile.json".format(odahu_conf['grafana_admin']))
        local("sed -i \'s|<GRAFANA_PASS>|{}|g\' /tmp/profile.json".format(odahu_conf['grafana_pass']))
        local("sed -i \'s|<INITIAL_NODE_COUNT>|{}|g\' /tmp/profile.json".format(odahu_conf['initial_node_count']))
        local("sed -i \'s|<ISTIO_HELM_REPO>|{}|g\' /tmp/profile.json".format(odahu_conf['istio_helm_repo']))
        local("sed -i \'s|<HELM_REPO>|{}|g\' /tmp/profile.json".format(odahu_conf['helm_repo']))
        local("sed -i \'s|<K8S_VERSION>|{}|g\' /tmp/profile.json".format(odahu_conf['k8s_version']))
        local("sed -i \'s|<ODAHU_INFRA_VERSION>|{}|g\' /tmp/profile.json".format(odahu_conf['odahu_infra_version']))
        local("sed -i \'s|<ODAHUFLOW_VERSION>|{}|g\' /tmp/profile.json".format(odahu_conf['odahuflow_version']))
        local("sed -i \'s|<MLFLOW_TOOLCHAIN_VERSION>|{}|g\' /tmp/profile.json".format(odahu_conf['mlflow_toolchain_version']))
        local("sed -i \'s|<JUPYTERLAB_VERSION>|{}|g\' /tmp/profile.json".format(odahu_conf['jupyterlab_version']))
        local("sed -i \'s|<PACKAGER_VERSION>|{}|g\' /tmp/profile.json".format(odahu_conf['packager_version']))
        local("sed -i \'s|<NODE_LOCATIONS>|{}|g\' /tmp/profile.json".format(odahu_conf['node_locations']))
        local("sed -i \'s|<NODE_VERSION>|{}|g\' /tmp/profile.json".format(odahu_conf['node_version']))
        local("sed -i \'s|<OAUTH_OIDC_ISSUER_URL>|{}|g\' /tmp/profile.json".format(odahu_conf['oauth_oidc_issuer_url']))
        local("sed -i \'s|<VPC_NAME>|{}|g\' /tmp/profile.json".format(odahu_conf['vpc_name']))
        local("sed -i \'s|<SUBNET_NAME>|{}|g\' /tmp/profile.json".format(odahu_conf['private_subnet_name']))
        local("sed -i \'s|<OAUTH_CLIENT_ID>|{}|g\' /tmp/profile.json".format(odahu_conf['oauth_client_id']))
        local("sed -i \'s|<OAUTH_CLIENT_SECRET>|{}|g\' /tmp/profile.json".format(odahu_conf['oauth_client_secret']))
        local("sed -i \'s|<OAUTH_COOCKIE_SECRET>|{}|g\' /tmp/profile.json".format(odahu_conf['oauth_cookie_secret']))
        local("sed -i \'s|<PODS_CIDR>|{}|g\' /tmp/profile.json".format(odahu_conf['pods_cidr']))
        local("sed -i \'s|<ROOT_DOMAIN>|{}|g\' /tmp/profile.json".format(odahu_conf['root_domain']))
        local("sed -i \'s|<SERVICE_CIDR>|{}|g\' /tmp/profile.json".format(odahu_conf['service_cidr']))
        local("sed -i \'s|<TLS_CRT>|{}|g\' /tmp/profile.json".format(odahu_conf['tls_crt'].replace('\n', '')))
        local("sed -i \'s|<TLS_KEY>|{}|g\' /tmp/profile.json".format(odahu_conf['tls_key'].replace('\n', '')))
        local("sed -i \'s|<SSH_KEY>|{}|g\' /tmp/profile.json".format(odahu_conf['ssh_key'].replace('\n', '')))
        local("sed -i \'s|<DNS_PROJECT_ID>|{}|g\' /tmp/profile.json".format(odahu_conf['dns_project_id']))
        local("sed -i \'s|<DECRYPT_TOKEN>|{}|g\' /tmp/profile.json".format(odahu_conf['decrypt_token']))
        local("sed -i \'s|<INFRA_VPC_PEERING>|{}|g\' /tmp/profile.json".format(odahu_conf['infra_vpc_peering']))
        local("sed -i \'s|\r||g\' /tmp/profile.json")
        local('cp /tmp/profile.json /')
        local('cat /profile.json')
    except Exception as err:
        traceback.print_exc()
        append_result("Failed to configure parameter file.", str(err))
        sys.exit(1)

    try:
        local('tf_runner create -o /response/odahu_{}_{}.json'.format(odahu_conf['odahu_cluster_name'], os.environ['request_id']))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed to deploy Odahu cluster.", str(err))
        sys.exit(1)

    # generating output information
    try:
        local("sed -e 's|name = |"description": |g' result")
        local("sed -e 's|url = |"url": |g' result")
        odahu_urls = local("cat result")
    except Exception as err:
        traceback.print_exc()
        append_result("Failed to generate output information.", str(err))
        sys.exit(1)

    print('[SUMMARY]')
    logging.info('[SUMMARY]')
    print('Cluster name: {}'.format(odahu_conf['odahu_cluster_name']))

    with open("/root/result.json", 'w') as result:
        res = {"odahu_urls": [odahu_urls]}
        result.write(json.dumps(res))