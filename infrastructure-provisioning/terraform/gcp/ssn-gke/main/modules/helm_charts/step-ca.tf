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
  step_ca_name = "step-certificates"
}

resource "kubernetes_service" "step_service_lb" {

  depends_on = [null_resource.cert_manager_delay]
  metadata {
    name = "step-certs"
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  }
  spec {
    selector = {
      "app" = local.step_ca_name
    }
    session_affinity = "ClientIP"
    port {
      port        = 443
      target_port = 9000
    }

    type = "LoadBalancer"
  }
}

data "template_file" "step_ca_values" {
  template = file("./modules/helm_charts/step-ca-chart/values.yaml")
  vars     = {
    step_ca_password             = random_string.step_ca_password.result
    step_ca_provisioner_password = random_string.step_ca_provisioner_password.result
    step_ca_host                 = kubernetes_service.step_service_lb.load_balancer_ingress.0.ip
    step_chart_name = local.step_ca_name
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  }
}

resource "helm_release" "step_ca" {
  name       = local.step_ca_name
  chart = "./modules/helm_charts/step-ca-chart"
  namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  # depends_on = [kubernetes_service.step_service_lb]
  wait       = false
  timeout    = 600

  values     = [
    data.template_file.step_ca_values.rendered
  ]
}

resource "null_resource" "step_ca_delay" {
  provisioner "local-exec" {
    command = "sleep 120"
  }
  triggers = {
    "before" = helm_release.step_ca.name
  }
}