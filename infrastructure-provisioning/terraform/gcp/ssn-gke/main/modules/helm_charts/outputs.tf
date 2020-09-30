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

output "keycloak_client_secret" {
    value = random_uuid.keycloak_client_secret.result
}

output "keycloak_client_id" {
    value = "datalab-ui"
}

output "keycloak_user_password" {
    value = random_string.keycloak_password.result
}

output "ssn_ui_host" {
    value = local.ui_host
}

output "step_root_ca" {
    value = lookup(data.external.step-ca-config-values.result, "rootCa")
}

output "step_kid" {
    value = lookup(data.external.step-ca-config-values.result, "kid")
}

output "step_kid_password" {
    value = random_string.step_ca_provisioner_password.result
}

output "step_ca_url" {
    value = "https://${kubernetes_service.step_service_lb.load_balancer_ingress.0.ip}"
}

