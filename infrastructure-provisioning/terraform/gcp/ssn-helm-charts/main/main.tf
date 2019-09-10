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
provider "helm" {

  kubernetes {
    host                   = var.k8s_gke_endpoint
    token                  = base64decode(var.k8s_gke_client_access_token)
    client_certificate     = base64decode(var.k8s_gke_clinet_cert)
    client_key             = base64decode(var.k8s_gke_client_key)
    cluster_ca_certificate = base64decode(var.k8s_gke_cluster_ca)
  }
  install_tiller = true
}

output "keycloak_client_secret" {
    value = random_uuid.keycloak_client_secret.result
}