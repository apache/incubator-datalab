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
    ssn_k8s_alb_dns_name = var.ssn_k8s_alb_dns_name
    keycloak_user        = var.keycloak_user
    keycloak_passowrd    = var.keycloak_password
    ldap_usernameAttr    = var.ldap_usernameAttr
    ldap_rdnAttr         = var.ldap_rdnAttr
    ldap_uuidAttr        = var.ldap_uuidAttr
    ldap_connection_url  = var.ldap_connection_url
    ldap_users_dn        = var.ldap_users_dn
    ldap_bind_dn         = var.ldap_bind_dn
    ldap_bind_creds      = var.ldap_bind_creds
  }
}

data "template_file" "keycloak_values" {
  template = file("./files/keycloak_values.yaml")
  vars = {
    keycloak_user           = var.keycloak_user
    keycloak_password       = var.keycloak_password
    ssn_k8s_alb_dns_name    = var.ssn_k8s_alb_dns_name
    configure_keycloak_file = data.template_file.configure_keycloak.rendered
    mysql_db_name           = var.mysql_db_name
    mysql_user              = var.mysql_user
    mysql_user_password     = var.mysql_user_password
  }
}

data "helm_repository" "codecentric" {
    name = "codecentric"
    url  = "https://codecentric.github.io/helm-charts"
}

resource "helm_release" "keycloak" {
  name       = "keycloak"
  repository = data.helm_repository.codecentric.metadata.0.name
  chart      = "codecentric/keycloak"
  wait       = true
  timeout    = 600

  values     = [
    data.template_file.keycloak_values.rendered
  ]
  depends_on = [helm_release.keycloak-mysql]
}