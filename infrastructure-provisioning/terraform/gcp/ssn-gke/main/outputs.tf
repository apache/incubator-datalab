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

output "service_base_name" {
  value = var.service_base_name
}

output "vpc_id" {
  value = module.gke_cluster.vpc_name
}

output "subnet_id" {
  value = module.gke_cluster.subnet_name
}

output "keycloak_client_secret" {
    value = module.helm_charts.keycloak_client_secret
}

output "keycloak_client_id" {
    value = var.keycloak_client_id
}

output "ssn_ui_host" {
    value = module.helm_charts.ssn_ui_host
}

output "step_root_ca" {
    value = module.helm_charts.step_root_ca
}

output "step_kid" {
    value = module.helm_charts.step_kid
}

output "step_kid_password" {
    value = module.helm_charts.step_kid_password
}

output "step_ca_url" {
    value = module.helm_charts.step_ca_url
}

output "keycloak_auth_server_url" {
    value = "https://${module.helm_charts.ssn_ui_host}/auth"
}

output "keycloak_realm_name" {
    value = var.keycloak_realm_name
}

output "keycloak_user_name" {
    value = var.keycloak_user
}

output "keycloak_user_password" {
    value = module.helm_charts.keycloak_user_password
}