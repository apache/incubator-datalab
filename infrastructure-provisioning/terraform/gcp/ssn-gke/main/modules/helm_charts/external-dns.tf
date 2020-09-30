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

data "template_file" "external_dns_values" {
    template = file("./modules/helm_charts/external-dns/values.yaml")
    vars     = {
        namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
      project_id = var.project_id
        domain     = var.domain
    }
}

resource "helm_release" "external_dns" {
    name       = "external-dns"
  chart = "./modules/helm_charts/external-dns"
  namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  wait = true
    depends_on = [helm_release.nginx]
    values     = [
        data.template_file.external_dns_values.rendered
    ]
}