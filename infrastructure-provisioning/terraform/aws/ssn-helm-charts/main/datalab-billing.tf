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

data "template_file" "datalab_billing_values" {
  template = file("./datalab-billing-chart/values.yaml")
  vars = {
    mongo_db_name = var.mongo_dbname
    mongo_user = var.mongo_db_username
    mongo_port = var.mongo_service_port
    mongo_service_name = var.mongo_service_name
    service_base_name = var.service_base_name
    tag_resource_id = var.tag_resource_id
    billing_bucket = var.billing_bucket
    billing_bucket_path = var.billing_bucket_path
    billing_aws_job_enabled = var.billing_aws_job_enabled
    billing_aws_account_id = var.billing_aws_account_id
    billing_tag = var.billing_tag
    billing_datalab_id = var.billing_datalab_id
    billing_usage_date = var.billing_usage_date
    billing_product = var.billing_product
    billing_usage_type = var.billing_usage_type
    billing_usage = var.billing_usage
    billing_cost = var.billing_cost
    billing_resource_id = var.billing_resource_id
    billing_tags = var.billing_tags
  }
}

resource "helm_release" "datalab-billing" {
  name = "datalab-billing"
  chart = "./datalab-billing-chart"
  namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  depends_on = [
    helm_release.mongodb,
    kubernetes_secret.mongo_db_password_secret]
  wait = true

  values = [
    data.template_file.datalab_billing_values.rendered
  ]
}
