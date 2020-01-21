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

data "template_file" "step_issuer_values" {
  template = file("./modules/helm_charts/step-issuer-chart/values.yaml")
}

resource "helm_release" "step-issuer" {
    name       = "step-issuer"
    chart      = "./modules/helm_charts/step-issuer-chart"
    wait       = true
    depends_on = [null_resource.step_ca_delay]

    values     = [
        data.template_file.step_issuer_values.rendered
    ]
}

resource "null_resource" "step_issuer_delay" {
  provisioner "local-exec" {
    command = "sleep 120"
  }
  triggers = {
    "before" = helm_release.step-issuer.name
  }
}

data "template_file" "step_ca_issuer_values" {
  template = file("./modules/helm_charts/step-ca-issuer-chart/values.yaml")
  vars     = {
    step_ca_url      = "https://${kubernetes_service.step_service_lb.load_balancer_ingress.0.ip}"
    step_ca_bundle   = lookup(data.external.step-ca-config-values.result, "rootCa")
    namespace        = kubernetes_namespace.dlab-namespace.metadata[0].name
    step_ca_kid_name = lookup(data.external.step-ca-config-values.result, "kidName")
    step_ca_kid      = lookup(data.external.step-ca-config-values.result, "kid")
  }
}

resource "helm_release" "step-ca-issuer" {
    name       = "step-ca-issuer"
    chart      = "./modules/helm_charts/step-ca-issuer-chart"
    wait       = true
    depends_on = [null_resource.step_issuer_delay]

    values     = [
        data.template_file.step_ca_issuer_values.rendered
    ]
}

resource "null_resource" "step_ca_issuer_delay" {
  provisioner "local-exec" {
    command = "sleep 60"
  }
  triggers = {
    "before" = helm_release.step-ca-issuer.name
  }
}

data "external" "step-ca-config-values" {
  program     = ["sh", "./modules/helm_charts/files/get_configmap_values.sh", var.credentials_file_path,
                 var.gke_cluster_name, var.region, var.project_id]
  depends_on  = [null_resource.step_issuer_delay]
}
