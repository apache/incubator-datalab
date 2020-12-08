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

data "template_file" "configure_keycloak" {
  template = file("./files/configure_keycloak.sh")
  vars     = {
    ssn_k8s_alb_dns_name   = local.ui_host
    keycloak_user          = var.keycloak_user
    keycloak_password      = random_string.keycloak_password.result
    keycloak_client_secret = random_uuid.keycloak_client_secret.result
    ldap_usernameAttr      = var.ldap_usernameAttr
    ldap_rdnAttr           = var.ldap_rdnAttr
    ldap_uuidAttr          = var.ldap_uuidAttr
    ldap_host              = var.ldap_host
    ldap_users_group       = var.ldap_users_group
    ldap_dn                = var.ldap_dn
    ldap_user              = var.ldap_user
    ldap_bind_creds        = var.ldap_bind_creds
    keycloak_realm_name    = var.keycloak_realm_name
    keycloak_client_id     = var.keycloak_client_id
  }
}

data "template_file" "keycloak_values" {
  template = file("./files/keycloak_values.yaml")
  vars     = {
    keycloak_user           = var.keycloak_user
    keycloak_password       = random_string.keycloak_password.result
    ssn_k8s_alb_dns_name    = local.ui_host
    configure_keycloak_file = data.template_file.configure_keycloak.rendered
    mysql_db_name           = var.mysql_keycloak_db_name
    mysql_user              = var.mysql_keycloak_user
    mysql_user_password     = random_string.mysql_keycloak_user_password.result
    # replicas_count          = var.ssn_k8s_workers_count > 3 ? 3 : var.ssn_k8s_workers_count
  }
}

data "helm_repository" "codecentric" {
  name = "codecentric"
  url  = "https://codecentric.github.io/helm-charts"
}

resource "helm_release" "keycloak" {
  name       = "keycloak"
  repository = data.helm_repository.codecentric.metadata.0.name
  chart = "codecentric/keycloak"
  namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  wait = true
  timeout    = 600

  values     = [
    data.template_file.keycloak_values.rendered
  ]
  depends_on = [
    helm_release.keycloak-mysql,
    kubernetes_secret.keycloak_password_secret,
    helm_release.nginx,
    helm_release.datalab_ui]
}