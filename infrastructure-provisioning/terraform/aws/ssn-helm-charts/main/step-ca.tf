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

data "template_file" "step_ca_values" {
  template = file("./step-ca-chart/values.yaml")
  vars = {
    storage_class_name = kubernetes_storage_class.datalab-storage-class.metadata[0].name
    ssn_k8s_nlb_dns_name = var.ssn_k8s_nlb_dns_name
    step_ca_password             = random_string.step_ca_password.result
    step_ca_provisioner_password = random_string.step_ca_provisioner_password.result
  }
}

resource "helm_release" "step_ca" {
  name       = "step-certificates"
  chart = "./step-ca-chart"
  namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  depends_on = [
    null_resource.cert_manager_delay]
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