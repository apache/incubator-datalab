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

resource "helm_release" "keycloak-mysql" {
  name = "keycloak-mysql"
  chart = "stable/mysql"
  wait = true

  values = [
    file("files/mysql_values.yaml")
  ]

//  set {
//    name = "mysqlRootPassword"
//    value = "1234567890o"
//  }
//
//  set {
//    name = "mysqlUser"
//    value = "keycloak"
//  }
//
//  set {
//    name = "mysqlPassword"
//    value = "1234567890o"
//  }
//
//  set {
//    name = "mysqlDatabase"
//    value = "keycloak"
//  }

  set {
    name = "persistence.existingClaim"
    value = "${kubernetes_persistent_volume_claim.example.metadata.0.name}"
  }
}


provider "kubernetes" {
  }

resource "kubernetes_persistent_volume" "example" {
  metadata {
    name = "mysql-keycloak-pv2"
  }
  spec {
    capacity = {
      storage = "8Gi"
    }
    access_modes = ["ReadWriteMany"]
    persistent_volume_source {
      host_path {
        path = "/home/dlab-user/keycloak-pv2"
      }
    }
  }
}


resource "kubernetes_persistent_volume_claim" "example" {
  metadata {
    name = "mysql-keycloak-pvc2"
  }
  spec {
    access_modes = ["ReadWriteMany"]
    resources {
      requests = {
        storage = "5Gi"
      }
    }
    volume_name = "${kubernetes_persistent_volume.example.metadata.0.name}"
  }
}

