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

data "template_file" "keycloak-mysql-values" {
  template = file("./files/mysql_keycloak_values.yaml")
  vars = {
    mysql_root_password = random_string.mysql_root_password.result
    mysql_user          = var.mysql_keycloak_user
    mysql_user_password = random_string.mysql_keycloak_user_password.result
    mysql_db_name = var.mysql_keycloak_db_name
    storage_class = kubernetes_storage_class.datalab-storage-class.metadata[0].name
    mysql_disk_size = var.mysql_disk_size
  }
}

resource "helm_release" "keycloak-mysql" {
  name       = "keycloak-mysql"
  chart = "stable/mysql"
  namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  wait = true
  values     = [
    data.template_file.keycloak-mysql-values.rendered
  ]
  depends_on = [kubernetes_secret.mysql_root_password_secret, kubernetes_secret.mysql_keycloak_user_password_secret,
                helm_release.nginx]
}
