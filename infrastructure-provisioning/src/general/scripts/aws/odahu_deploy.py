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

import json
import sys
from datalab.fab import *
from datalab.meta_lib import *
from datalab.actions_lib import *
from datalab.logger import logging
import os
import base64
import subprocess

if __name__ == "__main__":
    logging.info('Generating infrastructure names and tags')
    odahu_conf = dict()
    odahu_conf['allowed_cidr'] = os.environ['odahu_allowed_cidr'].split(',')
    odahu_conf['project_id'] = (os.environ['gcp_project_id'])
    odahu_conf['region'] = (os.environ['aws_region'])
    odahu_conf['zone'] = (os.environ['aws_zone'])
    odahu_conf['edge_user_name'] = os.environ['edge_user_name']
    odahu_conf['dns_zone_name'] = os.environ['odahu_dns_zone_name']
    odahu_conf['docker_repo'] = os.environ['odahu_docker_repo']
    odahu_conf['cidr'] = os.environ['odahu_cidr']
    odahu_conf['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    odahu_conf['project_name'] = (os.environ['project_name']).lower().replace('_', '-')
    odahu_conf['cluster_name'] = "{}-{}".format((os.environ['conf_service_base_name']).lower().replace('_', '-'),
                                                (os.environ['odahu_cluster_name']).lower().replace('_', '-'))
    odahu_conf['bucket_name'] = "{}-tfstate".format(odahu_conf['cluster_name'])
    try:
        if os.environ['gcp_vpc_name'] == '':
            raise KeyError
        else:
            odahu_conf['vpc_name'] = os.environ['gcp_vpc_name']
    except KeyError:
        odahu_conf['vpc_name'] = odahu_conf['service_base_name'] + '-ssn-vpc'
    odahu_conf['vpc_cidr'] = os.environ['conf_vpc_cidr']
    tag = {"Key": '{}-Tag'.format(odahu_conf['service_base_name']),
           "Value": "{}-{}-subnet".format(odahu_conf['service_base_name'], odahu_conf['project_name'])}
    odahu_conf['private_subnet_cidr'] = get_subnet_by_tag(tag)
    odahu_conf['grafana_admin'] = os.environ['odahu_grafana_admin']
    odahu_conf['grafana_pass'] = id_generator()
    odahu_conf['docker_password'] = base64.b64decode(os.environ['odahu_docker_password'] + "==")
    odahu_conf['initial_node_count'] = os.environ['odahu_initial_node_count']
    odahu_conf['istio_helm_repo'] = os.environ['odahu_istio_helm_repo']
    odahu_conf['helm_repo'] = os.environ['odahu_helm_repo']
    odahu_conf['k8s_version'] = os.environ['odahu_k8s_version']
    odahu_conf['oauth_oidc_issuer_url'] = "{}/realms/{}".format(os.environ['keycloak_auth_server_url'],
                                                                os.environ['keycloak_realm_name'])
    odahu_conf['oauth_oidc_host'] = os.environ['keycloak_auth_server_url'].replace('https://', '').replace('/auth', '')
    odahu_conf['oauth_client_id'] = os.environ['keycloak_client_name']
    odahu_conf['oauth_client_secret'] = os.environ['keycloak_client_secret']
    odahu_conf['oauth_cookie_secret'] = base64.b64encode(id_generator(16))
    odahu_conf['oauth_local_jwks'] = os.environ['odahu_oauth_local_jwks']
    odahu_conf['infra_version'] = os.environ['odahu_infra_version']
    odahu_conf['odahuflow_version'] = os.environ['odahu_odahuflow_version']
    odahu_conf['mlflow_toolchain_version'] = os.environ['odahu_mlflow_toolchain_version']
    odahu_conf['jupyterlab_version'] = os.environ['odahu_jupyterlab_version']
    odahu_conf['packager_version'] = os.environ['odahu_packager_version']
    odahu_conf['infra_cidr'] = os.environ['odahu_pods_cidr']
    odahu_conf['root_domain'] = os.environ['odahu_root_domain']
    odahu_conf['service_cidr'] = os.environ['odahu_service_cidr']
    odahu_conf['tls_crt'] = base64.b64decode(os.environ['odahu_tls_crt'] + "==")
    odahu_conf['tls_key'] = base64.b64decode(os.environ['odahu_tls_key'] + "==")
    odahu_conf['ssh_key'] = os.environ['ssh_key']
    odahu_conf['dns_project_id'] = os.environ['odahu_dns_project_id']
    odahu_conf['decrypt_token'] = id_generator()
    odahu_conf['infra_vpc_peering'] = os.environ['odahu_infra_vpc_peering']
    odahu_conf['automation_version'] = os.environ['odahu_automation_version']
    odahu_conf['ui_version'] = os.environ['odahu_ui_version']
    odahu_conf['examples_version'] = os.environ['odahu_examples_version']
    odahu_conf['jupyterhub_enabled'] = os.environ['odahu_jupyterhub_enabled']
    odahu_conf['oauth_mesh_enabled'] = os.environ['odahu_oauth_mesh_enabled']
    odahu_conf['keysecret'] = os.environ['odahu_keysecret']
    odahu_conf['airflow_secret'] = os.environ['odahu_airflow_secret']
    odahu_conf['operator_secret'] =os.environ['odahu_operator_secret']
    odahu_conf['resource-uploader_secret'] = os.environ['odahu_resource_uploader_secret']
    odahu_conf['tester_secret'] = os.environ['odahu_tester_secret']
    odahu_conf['tester-data-scientist_secret'] = os.environ['odahu_tester_data_scientist_secret']

    logging.info('Preparing parameters file')
    try:
        subprocess.run("cp /root/templates/profile.json /tmp/", shell=True, check=True)
        with open("/tmp/profile.json", 'w') as profile:
            prof ={
                    "allowed_ips": odahu_conf['allowed_cidr'],
                    "authorization_enabled": "true",
                    "authz_dry_run": "false",
                    "cloud": {
                        "aws": {
                            "az_list": ["{}".format(odahu_conf['zone'])],
                            "zone": "{}".format(odahu_conf['zone']),
                        },
                        "region": "{}".format(odahu_conf['region']),
                        "type": "aws"
                    },
                    "cluster_name": "{}".format(odahu_conf['cluster_name']),
                    "cluster_type": "aws/eks",
                    "data_bucket": "{}-data-bucket".format(odahu_conf['cluster_name']),
                    "dns": {
                        "domain": "odahu.{}.{}".format(odahu_conf['cluster_name'], odahu_conf['root_domain']),
                        "gcp_credentials": "{}",
                        "gcp_project_id": "{}".format(odahu_conf['project_id']),
                        "provider": "gcp",
                        "zone_name": "{}".format(odahu_conf['dns_zone_name']),
                    },
                    "docker_password": "{}".format(odahu_conf['docker_password']),
                    "docker_repo": "{}".format(odahu_conf['docker_repo']),
                    "docker_username": "_json_key",
                    "infra_cidr": "{}".format(odahu_conf['infra_cidr']),
                    "examples_version": "{}".format(odahu_conf['examples_version']),
                    "grafana_pass": "{}".format(odahu_conf['grafana_pass']),
                    "helm_repo": "{}".format(odahu_conf['helm_repo']),
                    "jupyterhub_enabled": odahu_conf['jupyterhub_enabled'],
                    "jupyterlab_version": "{}".format(odahu_conf['jupyterlab_version']),
                    "k8s_version": "{}".format(odahu_conf['k8s_version']),
                    "mlflow_toolchain_version": "{}".format(odahu_conf['mlflow_toolchain_version']),
                    "nat_subnet_cidr": "{}".format(odahu_conf['cidr']),
                    "node_pools": {
                        "main": {
                            "init_node_count": 1,
                            "max_node_count": 7,
                            "min_node_count": 1
                        },
                        "model_deployment": {
                            "labels": {
                                "mode": "odahu-flow-deployment"
                            },
                            "max_node_count": 3,
                            "taints": [
                                {
                                    "effect": "NO_SCHEDULE",
                                    "key": "dedicated",
                                    "value": "deployment"
                                }
                            ]
                        },
                        "packaging": {
                            "disk_size_gb": 100,
                            "labels": {
                                "mode": "odahu-flow-packaging"
                            },
                            "max_node_count": 3,
                            "taints": [
                                {
                                    "effect": "NO_SCHEDULE",
                                    "key": "dedicated",
                                    "value": "packaging"
                                }
                            ]
                        },
                        "training": {
                            "disk_size_gb": 50,
                            "labels": {
                                "mode": "odahu-flow-training"
                            },
                            "machine_type": "c5.2xlarge",
                            "taints": [
                                {
                                    "effect": "NO_SCHEDULE",
                                    "key": "dedicated",
                                    "value": "training"
                                }
                            ]
                        }
                    },
                    "oauth_client_id": "{}".format(odahu_conf['oauth_client_id']),
                    "oauth_client_secret": "{}".format(odahu_conf['oauth_client_secret']),
                    "oauth_cookie_secret": "{}".format(odahu_conf['oauth_cookie_secret']),
                    "oauth_local_jwks": "{}".format(odahu_conf['oauth_local_jwks']),
                    "oauth_mesh_enabled": odahu_conf['oauth_mesh_enabled'],
                    "oauth_oidc_audience": "legion",
                    "oauth_oidc_host": "{}".format(odahu_conf['oauth_oidc_host']),
                    "oauth_oidc_issuer_url": "{}".format(odahu_conf['oauth_oidc_issuer_url']),
                    "oauth_oidc_jwks_url": "{}/protocol/openid-connect/certs".format(odahu_conf['oauth_oidc_issuer_url']),
                    "oauth_oidc_port": 443,
                    "oauth_oidc_scope": "openid profile email offline_access groups",
                    "oauth_oidc_token_endpoint": "{}/protocol/openid-connect/token".format(odahu_conf['oauth_oidc_issuer_url']),
                    "odahu_automation_version": "{}".format(odahu_conf['automation_version']),
                    "odahu_infra_version": "{}".format(odahu_conf['infra_version']),
                    "odahu_ui_version": "{}".format(odahu_conf['ui_version']),
                    "odahuflow_connection_decrypt_token": "{}".format(odahu_conf['decrypt_token']),
                    "odahuflow_connections": [
                        {
                            "id": "odahu-flow-examples",
                            "spec": {
                                "description": "Git repository with the Odahu-Flow examples",
                                "keySecret": "{}".format(odahu_conf['keysecret']),
                                "reference": "{}".format(odahu_conf['examples_version']),
                                "type": "git",
                                "uri": "git@github.com:odahu/odahu-examples.git",
                                "webUILink": "https://github.com/odahu/odahu-examples"
                            }
                        }
                    ],
                    "odahuflow_version": "{}".format(odahu_conf['odahuflow_version']),
                    "opa_policies": {},
                    "packager_version": "{}".format(odahu_conf['packager_version']),
                    "service_accounts": {
                        "airflow": {
                            "client_id": "sa-airflow",
                            "client_secret": "{}".format(odahu_conf['airflow_secret'])
                        },
                        "operator": {
                            "client_id": "sa-operator",
                            "client_secret": "{}".format(odahu_conf['operator_secret'])
                        },
                        "resource_uploader": {
                            "client_id": "sa-resource-uploader",
                            "client_secret": "{}".format(odahu_conf['resource-uploader_secret'])
                        },
                        "test": {
                            "client_id": "sa-tester",
                            "client_secret": "{}".format(odahu_conf['tester_secret'])
                        },
                        "test_data_scientist": {
                            "client_id": "sa-tester-data-scientist",
                            "client_secret": "{}".format(odahu_conf['tester-data-scientist_secret'])
                        }
                    },
                    "service_cidr": "{}".format(odahu_conf['service_cidr']),
                    "ssh_key": "{}".format(odahu_conf['ssh_key'].replace('\n', '')),
                    "subnet_name": "{}".format(odahu_conf['private_subnet_name']),
                    "tfstate_bucket": "{}-tfstate".format(odahu_conf['cluster_name']),
                    "tls_crt": "{}".format(odahu_conf['tls_crt']),
                    "tls_key": "{}".format(odahu_conf['tls_key']),
                    "vpc_name": "{}".format(odahu_conf['vpc_name']),
                    "vault": {
                        "enabled": "true"
                    }
                    }
            profile.write(json.dumps(prof))
        subprocess.run('cat /tmp/profile.json', shell=True, check=True)
        subprocess.run('cp /tmp/profile.json /', shell=True, check=True)
    except Exception as err:
        traceback.print_exc()
        append_result("Failed to configure parameter file.", str(err))
        GCPActions().remove_bucket(odahu_conf['bucket_name'])
        GCPActions().remove_static_address(odahu_conf['static_address_name'], odahu_conf['region'])
        sys.exit(1)

    try:
        subprocess.run('tf_runner create -o /tmp/result.json', shell=True, check=True)
        subprocess.run("sed -i 's|name|description|g' /tmp/result.json", shell=True, check=True)
    except Exception as err:
        traceback.print_exc()
        append_result("Failed to deploy Odahu cluster.", str(err))
        GCPActions().remove_bucket(odahu_conf['bucket_name'])
        GCPActions().remove_static_address(odahu_conf['static_address_name'], odahu_conf['region'])
        sys.exit(1)

    # generating output information
    logging.info('[SUMMARY]')
    logging.info('[SUMMARY]')
    logging.info('Cluster name: {}'.format(odahu_conf['cluster_name']))
    with open('/tmp/result.json', 'r') as f:
        output = json.load(f)
        odahu_urls = json.dumps(output['odahu_urls']['value'], sort_keys=True, indent=4)
    logging.info('Odahu urls: {}'.format(odahu_urls))
    res = dict()
    res['odahu_urls'] = output['odahu_urls']['value']
    res['oauth_cookie_secret'] = odahu_conf['oauth_cookie_secret']
    res['odahuflow_connection_decrypt_token'] = odahu_conf['decrypt_token']
    res['grafana_pass'] = odahu_conf['grafana_pass']
    res['grafana_admin'] = odahu_conf['grafana_admin']
    with open("/root/result.json", 'w') as result:
        result.write(json.dumps(res))