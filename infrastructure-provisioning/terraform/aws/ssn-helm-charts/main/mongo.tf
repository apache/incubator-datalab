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

data "template_file" "mongo_values" {
  template = file("./files/mongo_values.yaml")
  vars     = {
      mongo_root_pwd      = random_string.mongo_root_password.result
      mongo_db_username   = var.mongo_db_username
      mongo_dbname        = var.mongo_dbname
      mongo_db_pwd        = random_string.mongo_db_password.result
      mongo_image_tag     = var.mongo_image_tag
      mongo_service_port  = var.mongo_service_port
      mongo_node_port     = var.mongo_node_port
  }
}

resource "helm_release" "mongodb" {
  name       = "mongo-ha"
  chart = "stable/mongodb"
  namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  wait = true
  values     = [
      data.template_file.mongo_values.rendered
  ]
  depends_on = [helm_release.nginx, kubernetes_secret.mongo_db_password_secret,
                kubernetes_secret.mongo_root_password_secret]
}
