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

output "ssn_keystore_password" {
  value = random_string.ssn_keystore_password.result
}

output "endpoint_keystore_password" {
  value = random_string.endpoint_keystore_password.result
}

output "k8s_gke_endpoint" {
  value = data.google_container_cluster.ssn_k8s_gke_cluster.endpoint
}

output "k8s_gke_client_access_token" {
  value = data.google_client_config.current.access_token
}

output "k8s_gke_clinet_cert" {
  value = google_container_cluster.ssn_k8s_gke_cluster.master_auth.0.client_certificate
}

output "k8s_gke_client_key" {
  value = google_container_cluster.ssn_k8s_gke_cluster.master_auth.0.client_key
}

output "k8s_gke_cluster_ca" {
  value = google_container_cluster.ssn_k8s_gke_cluster.master_auth.0.cluster_ca_certificate
}