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

data "template_file" "cert_manager_values" {
  template = file("./files/cert_manager_values.yaml")
}

resource "helm_release" "cert_manager_crd" {
    name       = "cert_manager_crd"
    chart      = "./cert-manager-crd-chart"
    wait       = true
}

data "helm_repository" "jetstack" {
  name = "jetstack"
  url  = "https://charts.jetstack.io"
}

resource "helm_release" "cert-manager" {
    name       = "cert-manager"
    repository = data.helm_repository.jetstack.metadata.0.name
    chart      = "jetstack/cert-manager"
    namespace  = kubernetes_namespace.cert-manager-namespace.metadata[0].name
    depends_on = [helm_release.cert_manager_crd]
    wait       = true
    version    = "v0.9.0"
    values     = [
        data.template_file.cert_manager_values.rendered
    ]
}

resource "null_resource" "cert_manager_delay" {
    provisioner "local-exec" {
        command = "sleep 120"
    }
    triggers = {
        "after" = helm_release.cert-manager.name
    }
}
