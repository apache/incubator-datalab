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
    custom_cert_name     = local.custom_certs_enabled == "true" ? reverse(split("/", var.custom_cert_path))[0] : "None"
    custom_key_name      = local.custom_certs_enabled == "true" ? reverse(split("/", var.custom_key_path))[0] : "None"
    custom_cert          = local.custom_certs_enabled == "true" ? base64encode(file("/tmp/${local.custom_cert_name}")) : "None"
    custom_key           = local.custom_certs_enabled == "true" ? base64encode(file("/tmp/${local.custom_key_name}")) : "None"
    ui_host              = local.custom_certs_enabled == "true" ? var.custom_certs_host : data.kubernetes_service.nginx-service.load_balancer_ingress.0.hostname
}

data "template_file" "dlab_ui_values" {
  template = file("./dlab-ui-chart/values.yaml")
  vars = {
      mongo_db_name          = var.mongo_dbname
      mongo_user             = var.mongo_db_username
      mongo_port             = var.mongo_service_port
      mongo_service_name     = var.mongo_service_name
      ssn_k8s_alb_dns_name   = local.ui_host
      service_base_name      = var.service_base_name
      os                     = var.env_os
      namespace              = kubernetes_namespace.dlab-namespace.metadata[0].name
      custom_certs_enabled   = local.custom_certs_enabled
      custom_certs_crt       = local.custom_cert
      custom_certs_key       = local.custom_key
      step_ca_crt            = lookup(data.external.step-ca-config-values.result, "rootCa")
      keycloak_realm_name    = var.keycloak_realm_name
      keycloak_client_id     = var.keycloak_client_id
  }
}

resource "helm_release" "dlab_ui" {
    name       = "dlab-ui"
    chart      = "./dlab-ui-chart"
    namespace  = kubernetes_namespace.dlab-namespace.metadata[0].name
    depends_on = [helm_release.mongodb, kubernetes_secret.mongo_db_password_secret, null_resource.step_ca_issuer_delay]
    wait       = true

    values     = [
        data.template_file.dlab_ui_values.rendered
    ]
}

data "kubernetes_service" "nginx-service" {
    metadata {
        name      = "${helm_release.nginx.name}-controller"
        namespace = kubernetes_namespace.dlab-namespace.metadata[0].name
    }
}


