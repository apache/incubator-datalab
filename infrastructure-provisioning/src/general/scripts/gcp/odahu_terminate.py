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
    odahu_conf['allowed_cidr'] = os.environ['odahu_allowed_cidr'].split(',')
    odahu_conf['project_id'] = (os.environ['gcp_project_id'])
    odahu_conf['region'] = (os.environ['gcp_region'])
    odahu_conf['zone'] = (os.environ['gcp_zone'])
    odahu_conf['node_locations'] = GCPMeta().get_available_zones()
    odahu_conf['dns_zone_name'] = os.environ['odahu_dns_zone_name']
    odahu_conf['docker_repo'] = os.environ['odahu_docker_repo']
    odahu_conf['odahu_cidr'] = os.environ['odahu_cidr']
    odahu_conf['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    odahu_conf['project_name'] = (os.environ['project_name']).lower().replace('_', '-')
    odahu_conf['cluster_name'] = (os.environ['odahu_cluster_name']).lower().replace('_', '-')
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
    odahu_conf['grafana_admin'] = os.environ['grafana_admin']
    odahu_conf['grafana_pass'] = os.environ['grafana_pass']
    odahu_conf['initial_node_count'] = os.environ['odahu_initial_node_count']
    odahu_conf['istio_helm_repo'] = os.environ['odahu_istio_helm_repo']
    odahu_conf['helm_repo'] = os.environ['odahu_helm_repo']
    odahu_conf['k8s_version'] = os.environ['odahu_k8s_version']
    odahu_conf['oauth_oidc_issuer_url'] = "{}/realms/{}".format(os.environ['keycloak_auth_server_url'],
                                                                os.environ['keycloak_realm_name'])
    odahu_conf['oauth_oidc_host'] = os.environ['keycloak_auth_server_url'].replace('https://', '').replace('/auth', '')
    odahu_conf['oauth_client_id'] = os.environ['keycloak_client_name']
    odahu_conf['oauth_client_secret'] = os.environ['keycloak_client_secret']
    odahu_conf['oauth_cookie_secret'] = os.environ['oauth_cookie_secret']
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
    odahu_conf['decrypt_token'] = os.environ['odahuflow_connection_decrypt_token']
    odahu_conf['infra_vpc_peering'] = os.environ['odahu_infra_vpc_peering']

    print('Preparing parameters file')
    try:
        local("cp /root/templates/profile.json /tmp/")
        with open("/tmp/profile.json", 'w') as profile:
            prof = {"allowed_ips": odahu_conf['allowed_cidr'],
                    "cloud_type": "gcp",
                    "cluster_name": "{}".format(odahu_conf['cluster_name']),
                    "cluster_type": "gcp/gke",
                    "cluster_context": "",
                    "dns_zone_name": "{}".format(odahu_conf['dns_zone_name']),
                    "docker_password": "",
                    "docker_repo": "{}".format(odahu_conf['docker_repo']),
                    "docker_user": "",
                    "gcp_cidr": "{}".format(odahu_conf['odahu_cidr']),
                    "gke_node_tag": "{}-gke-node".format(odahu_conf['cluster_name']).split(','),
                    "grafana_admin": "{}".format(odahu_conf['grafana_admin']),
                    "grafana_pass": "BMuYxTw6v5",
                    "oauth_oidc_audience": "legion",
                    "oauth_oidc_issuer_url": "{}".format(odahu_conf['oauth_oidc_issuer_url']),
                    "data_bucket": "{}-data-bucket".format(odahu_conf['cluster_name']),
                    "helm_repo": "{}".format(odahu_conf['helm_repo']),
                    "odahu_infra_version": "{}".format(odahu_conf['odahu_infra_version']),
                    "odahuflow_version": "{}".format(odahu_conf['odahuflow_version']),
                    "mlflow_toolchain_version": "{}".format(odahu_conf['mlflow_toolchain_version']),
                    "jupyterlab_version": "{}".format(odahu_conf['jupyterlab_version']),
                    "packager_version": "{}".format(odahu_conf['packager_version']),
                    "vpc_name": "{}".format(odahu_conf['vpc_name']),
                    "subnet_name": "{}".format(odahu_conf['private_subnet_name']),
                    "node_locations": odahu_conf['node_locations'],
                    "oauth_client_id": "{}".format(odahu_conf['oauth_client_id']),
                    "oauth_client_secret": "{}".format(odahu_conf['oauth_client_secret']),
                    "oauth_cookie_secret": "OWl6VnV3SHNIQzVyMThqVw==",
                    "oauth_oidc_scope": "openid profile email offline_access groups",
                    "oauth_oidc_host": "{}".format(odahu_conf['oauth_oidc_host']),
                    "oauth_oidc_jwks_url": "https://idp.demo.dlabanalytics.com/auth/realms/dlab/protocol/openid-connect/certs",
                    "oauth_oidc_port": 443,
                    "pods_cidr": "{}".format(odahu_conf['pods_cidr']),
                    "project_id": "{}".format(odahu_conf['project_id']),
                    "region": "{}".format(odahu_conf['region']),
                    "root_domain": "{}".format(odahu_conf['root_domain']),
                    "service_cidr": "{}".format(odahu_conf['service_cidr']),
                    "ssh_key": "{}".format(odahu_conf['ssh_key'].replace('\n', '')),
                    "tfstate_bucket": "{}-tfstate".format(odahu_conf['cluster_name']),
                    "tls_crt": "{}".format(odahu_conf['tls_crt']),
                    "tls_key": "{}".format(odahu_conf['tls_key']),
                    "zone": "{}".format(odahu_conf['zone']),
                    "dns_project_id": "{}".format(odahu_conf['dns_project_id']),
                    "odahuflow_connection_decrypt_token": "qfY2k5bA2M",
                    "authorization_enabled": "true",
                    "authz_dry_run": "true",
                    "github_org_name": "",
                    "oauth_local_jwks": "",
                    "oauth_mesh_enabled": "false",
                    "k8s_version": "1.14.10-gke.17",
                    "opa_policies": {
                        "policy.rego": "cGFja2FnZSBvZGFodQoKIyByb2xlcy5yZWdvCgphZG1pbiA6PSAiYWRtaW4iCmRhdGFfc2NpZW50aXN0IDo9ICJkYXRhX3NjaWVudGlzdCIKCiMgcmJhYy5yZWdvCgpyYmFjIDo9IHsKCWRhdGFfc2NpZW50aXN0OiBbCiAgICAJWyIqIiwgImFwaS92MS9tb2RlbC9kZXBsb3ltZW50LioiXSwKICAgIAlbIioiLCAiYXBpL3YxL21vZGVsL3BhY2thZ2luZy4qIl0sCiAgICAJWyIqIiwgImFwaS92MS9tb2RlbC90cmFpbmluZy4qIl0sCiAgICAJWyJHRVQiLCAiYXBpL3YxL2Nvbm5lY3Rpb24uKiJdLAogICAgCVsiUE9TVCIsICJhcGkvdjEvY29ubmVjdGlvbi4qIl0sCiAgICAJWyJHRVQiLCAiYXBpL3YxL3BhY2thZ2luZy9pbnRlZ3JhdGlvbi4qIl0sCiAgICAJWyJHRVQiLCAiYXBpL3YxL3Rvb2xjaGFpbi9pbnRlZ3JhdGlvbi4qIl0KICAgIF0sCiAgYWRtaW4gOiBbCiAgICAgIFsiLioiLCAiLioiXQogIF0KfQoKIyBpbnB1dF9tYXBwZXIKCnJvbGVzX21hcCA9IHsKCSJvZGFodV9hZG1pbiI6IGFkbWluLAogICAgIm9kYWh1X2RhdGFfc2NpZW50aXN0IjogZGF0YV9zY2llbnRpc3QKfQoKand0ID0gaW5wdXQuYXR0cmlidXRlcy5tZXRhZGF0YV9jb250ZXh0LmZpbHRlcl9tZXRhZGF0YVsiZW52b3kuZmlsdGVycy5odHRwLmp3dF9hdXRobiJdLmZpZWxkcy5qd3RfcGF5bG9hZAoKa2V5Y2xvYWtfdXNlcl9yb2xlc1tyb2xlXXsKCXJvbGUgPSBqd3QuS2luZC5TdHJ1Y3RWYWx1ZS5maWVsZHMucmVhbG1fYWNjZXNzLktpbmQuU3RydWN0VmFsdWUuZmllbGRzLnJvbGVzLktpbmQuTGlzdFZhbHVlLnZhbHVlc1tfXS5LaW5kLlN0cmluZ1ZhbHVlCn0KCnVzZXJfcm9sZXNbcm9sZV17Cglyb2xlID0gcm9sZXNfbWFwW2tleWNsb2FrX3VzZXJfcm9sZXNbX11dCn0KCgpwYXJzZWRfaW5wdXQgPSB7CgkiYWN0aW9uIjogaW5wdXQuYXR0cmlidXRlcy5yZXF1ZXN0Lmh0dHAubWV0aG9kLAogICJyZXNvdXJjZSI6IGlucHV0LmF0dHJpYnV0ZXMucmVxdWVzdC5odHRwLnBhdGgsCiAgInVzZXIiOiB7CiAgICAicm9sZXMiOiB1c2VyX3JvbGVzCiAgfQp9CgoKIyBjb3JlCgpkZWZhdWx0IGFsbG93ID0gZmFsc2UKYWxsb3cgewoJYW55X3VzZXJfcm9sZSA6PSBwYXJzZWRfaW5wdXQudXNlci5yb2xlc1tfXQogICAgYW55X3Blcm1pc3Npb25fb2ZfdXNlcl9yb2xlIDo9IHJiYWNbYW55X3VzZXJfcm9sZV1bX10KICAgIGFjdGlvbiA6PSBhbnlfcGVybWlzc2lvbl9vZl91c2VyX3JvbGVbMF0KICAgIHJlc291cmNlIDo9IGFueV9wZXJtaXNzaW9uX29mX3VzZXJfcm9sZVsxXQoKICAgIHJlX21hdGNoKGFjdGlvbiwgcGFyc2VkX2lucHV0LmFjdGlvbikKICAgIHJlX21hdGNoKHJlc291cmNlLCBwYXJzZWRfaW5wdXQucmVzb3VyY2UpCn0="},
                    "node_pools": {
                        "packaging": {
                            "disk_size_gb": 64,
                            "max_node_count": 3,
                            "labels": {
                                "mode": "odahu-flow-packaging"
                            },
                            "taints": [
                                {
                                    "value": "packaging",
                                    "effect": "NO_SCHEDULE",
                                    "key": "dedicated"
                                }
                            ],
                            "machine_type": "n1-standard-4",
                            "disk_type": "pd-ssd"
                        },
                        "training": {
                            "labels": {
                                "mode": "odahu-flow-training"
                            },
                            "disk_size_gb": 100,
                            "taints": [
                                {
                                    "value": "training",
                                    "effect": "NO_SCHEDULE",
                                    "key": "dedicated"
                                }
                            ],
                            "machine_type": "n1-highcpu-8"
                        },
                        "main": {
                            "init_node_count": 3,
                            "disk_size_gb": 64,
                            "min_node_count": 1,
                            "max_node_count": 5
                        },
                        "training_gpu": {
                            "labels": {
                                "mode": "odahu-flow-training-gpu"
                            },
                            "gpu": [
                                {
                                    "count": 2,
                                    "type": "nvidia-tesla-p100"
                                }
                            ],
                            "disk_size_gb": 100,
                            "taints": [
                                {
                                    "value": "training-gpu",
                                    "effect": "NO_SCHEDULE",
                                    "key": "dedicated"
                                }
                            ],
                            "machine_type": "n1-standard-8"
                        },
                        "model_deployment": {
                            "labels": {
                                "mode": "odahu-flow-deployment"
                            },
                            "taints": [
                                {
                                    "value": "deployment",
                                    "effect": "NO_SCHEDULE",
                                    "key": "dedicated"
                                }
                            ],
                            "max_node_count": 3
                        }
                    }
                    }
            profile.write(json.dumps(prof))
        local('cat /tmp/profile.json')
        local('cp /tmp/profile.json /')
    except Exception as err:
        traceback.print_exc()
        append_result("Failed to configure parameter file.", str(err))
        sys.exit(1)

    print('Removing Odahu cluster')
    try:
        local('tf_runner destroy')
    except Exception as err:
        traceback.print_exc()
        append_result("Failed to terminate Odahu cluster.", str(err))
        sys.exit(1)

    try:
        buckets = GCPMeta().get_list_buckets(odahu_conf['cluster_name'])
        if 'items' in buckets:
            for i in buckets['items']:
                GCPActions().remove_bucket(i['name'])
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    try:
        static_addresses = GCPMeta().get_list_static_addresses(odahu_conf['region'], odahu_conf['cluster_name'])
        if 'items' in static_addresses:
            for i in static_addresses['items']:
                GCPActions().remove_static_address(i['name'], odahu_conf['region'])
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)
