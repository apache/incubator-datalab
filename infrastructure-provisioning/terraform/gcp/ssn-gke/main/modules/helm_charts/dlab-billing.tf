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

data "template_file" "dlab_billing_values" {
  template = file("./modules/helm_charts/dlab-billing-chart/values.yaml")
  vars = {
    mongo_db_name           = var.mongo_dbname
    mongo_user              = var.mongo_db_username
    mongo_port              = var.mongo_service_port
    mongo_service_name      = var.mongo_service_name
    service_base_name       = var.service_base_name
    big_query_dataset       = var.big_query_dataset
  }
}

resource "helm_release" "dlab-billing" {
    name       = "dlab-billing"
    chart      = "./modules/helm_charts/dlab-billing-chart"
    depends_on = [helm_release.mongodb, kubernetes_secret.mongo_db_password_secret, null_resource.cert_manager_delay]
    namespace  = kubernetes_namespace.dlab-namespace.metadata[0].name
    wait       = true

    values     = [
        data.template_file.dlab_billing_values.rendered
    ]
}
