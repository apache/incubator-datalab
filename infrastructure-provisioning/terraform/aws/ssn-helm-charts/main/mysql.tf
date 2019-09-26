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
    mysql_db_name       = var.mysql_keycloak_db_name
    mysql_volume_claim  = kubernetes_persistent_volume_claim.mysql-keycloak-pvc.metadata.0.name
  }
}

resource "helm_release" "keycloak-mysql" {
  name   = "keycloak-mysql"
  chart  = "stable/mysql"
  wait   = true
  values = [
    data.template_file.keycloak-mysql-values.rendered
  ]
  depends_on = [kubernetes_secret.mysql_root_password_secret, kubernetes_secret.mysql_keycloak_user_password_secret]
}

provider "kubernetes" {}

resource "kubernetes_persistent_volume" "mysql-keycloak-pv" {
  metadata {
    name = "mysql-keycloak-pv"
  }
  spec {
    capacity = {
      storage = "8Gi"
    }
    access_modes = ["ReadWriteMany"]
    persistent_volume_source {
      host_path {
        path = "/home/dlab-user/keycloak-pv"
      }
    }
  }
}

resource "kubernetes_persistent_volume_claim" "mysql-keycloak-pvc" {
  metadata {
    name = "mysql-keycloak-pvc"
  }
  spec {
    access_modes = ["ReadWriteMany"]
    resources {
      requests = {
        storage = "5Gi"
      }
    }
    volume_name = kubernetes_persistent_volume.mysql-keycloak-pv.metadata.0.name
  }
}

data "template_file" "guacamole-mysql-values" {
  template = file("./files/mysql_guacamole_values.yaml")
  vars = {
    mysql_root_password = random_string.mysql_root_password.result
    mysql_user          = var.mysql_guacamole_user
    mysql_user_password = random_string.mysql_guacamole_user_password.result
    mysql_db_name       = var.mysql_guacamole_db_name
    mysql_volume_claim  = kubernetes_persistent_volume_claim.mysql-guacamole-pvc.metadata.0.name
  }
}

resource "helm_release" "guacamole-mysql" {
  name   = "guacamole-mysql"
  chart  = "stable/mysql"
  wait   = true
  values = [
    data.template_file.guacamole-mysql-values.rendered
  ]
  depends_on = [kubernetes_secret.mysql_root_password_secret, kubernetes_secret.mysql_guacamole_user_password_secret]
}

resource "kubernetes_persistent_volume" "mysql-guacamole-pv" {
  metadata {
    name = "mysql-guacamole-pv"
  }
  spec {
    capacity = {
      storage = "8Gi"
    }
    access_modes = ["ReadWriteMany"]
    persistent_volume_source {
      host_path {
        path = "/home/dlab-user/guacamole-pv"
      }
    }
  }
}

resource "kubernetes_persistent_volume_claim" "mysql-guacamole-pvc" {
  metadata {
    name = "mysql-guacamole-pvc"
  }
  spec {
    access_modes = ["ReadWriteMany"]
    resources {
      requests = {
        storage = "5Gi"
      }
    }
    volume_name = kubernetes_persistent_volume.mysql-guacamole-pv.metadata.0.name
  }
}

