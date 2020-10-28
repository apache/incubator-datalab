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

locals {
  custom_certs_enabled = lower(var.custom_certs_enabled)
  custom_cert_name = local.custom_certs_enabled == "true" ? reverse(split("/", var.custom_cert_path))[0] : "None"
  custom_key_name = local.custom_certs_enabled == "true" ? reverse(split("/", var.custom_key_path))[0] : "None"
  custom_cert = local.custom_certs_enabled == "true" ? base64encode(file("/tmp/${local.custom_cert_name}")) : "None"
  custom_key = local.custom_certs_enabled == "true" ? base64encode(file("/tmp/${local.custom_key_name}")) : "None"
  ui_host = local.custom_certs_enabled == "true" ? var.custom_certs_host : "${var.service_base_name}-ssn.${var.domain}"
}

data "template_file" "datalab_ui_values" {
  template = file("./modules/helm_charts/datalab-ui-chart/values.yaml")
  vars = {
    mongo_db_name = var.mongo_dbname
    mongo_user = var.mongo_db_username
    mongo_port = var.mongo_service_port
    mongo_service_name = var.mongo_service_name
    ssn_k8s_alb_dns_name = local.ui_host
    service_base_name = var.service_base_name
    os = var.env_os
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
    custom_certs_enabled = local.custom_certs_enabled
    custom_certs_crt = local.custom_cert
    custom_certs_key = local.custom_key
    step_ca_crt = lookup(data.external.step-ca-config-values.result, "rootCa")
    keycloak_realm_name = var.keycloak_realm_name
    keycloak_client_id = var.keycloak_client_id
  }
}

resource "helm_release" "datalab_ui" {
  name = "datalab-ui"
  chart = "./modules/helm_charts/datalab-ui-chart"
  namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  wait = true
    depends_on = [
    helm_release.mongodb,
    kubernetes_secret.mongo_db_password_secret,
    null_resource.step_ca_issuer_delay,
    helm_release.external_dns]
    values = [data.template_file.datalab_ui_values.rendered]
}

data "kubernetes_service" "ui_service" {
  metadata {
    name = helm_release.datalab_ui.name
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  }
  depends_on = [
    helm_release.datalab_ui]
}
