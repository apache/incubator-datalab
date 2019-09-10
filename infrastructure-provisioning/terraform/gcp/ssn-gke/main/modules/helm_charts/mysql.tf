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

data "template_file" "mysql_values" {
  template = file("./modules/helm_charts/files/mysql_values.yaml")
  vars = {
    mysql_root_password = random_string.mysql_root_password.result
    mysql_user          = var.mysql_user
    mysql_user_password = random_string.mysql_user_password.result
    mysql_db_name       = var.mysql_db_name
  }
}

resource "helm_release" "keycloak-mysql" {
  name   = "keycloak-mysql"
  chart  = "stable/mysql"
  wait   = true
  values = [
    data.template_file.mysql_values.rendered
  ]
  depends_on = [kubernetes_secret.mysql_root_password_secret, kubernetes_secret.mysql_user_password_secret]
}

//resource "kubernetes_persistent_volume" "example" {
//  metadata {
//    name = "mysql-keycloak-pv2"
//  }
//  spec {
//    capacity = {
//      storage = "8Gi"
//    }
//    access_modes = ["ReadWriteMany"]
//    persistent_volume_source {
//      host_path {
//        path = "/home/dlab-user/keycloak-pv2"
//      }
//    }
//  }
//}
//
//resource "kubernetes_persistent_volume_claim" "example" {
//  metadata {
//    name = "mysql-keycloak-pvc2"
//  }
//  spec {
//    access_modes = ["ReadWriteMany"]
//    resources {
//      requests = {
//        storage = "5Gi"
//      }
//    }
//    volume_name = kubernetes_persistent_volume.example.metadata.0.name
//  }
//}

