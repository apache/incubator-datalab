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

data "helm_repository" "smallstep" {
  name = "smallstep"
  url  = "https://smallstep.github.io/helm-charts/"
}

data "template_file" "step_ca_values" {
  template = file("./files/step_ca_values.yaml")
  vars = {
    storage_class_name = kubernetes_storage_class.dlab-storage-class.metadata[0].name
  }
}

resource "helm_release" "step_ca" {
  name       = "dlab-step-ca"
  repository = data.helm_repository.smallstep.metadata.0.name
  chart      = "smallstep/step-certificates"
  namespace  = kubernetes_namespace.dlab-namespace.metadata[0].name
  wait       = true
  timeout    = 600

  values     = [
    data.template_file.step_ca_values.rendered
  ]
}