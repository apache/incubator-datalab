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

resource "helm_release" "keycloak" {
  name = "keycloak"
  chart = "stable/keycloak"
  wait = true

  values = [
    file("files/keycloak_values.yaml")
  ]

//  set {
//    name = "keycloak.username"
//    value = "dlab-admin"
//  }
//
//  set {
//    name = "keycloak.password"
//    value = "12345o"
//  }
//
//  set {
//    name = "keycloak.persistence.dbVendor"
//    value = "mysql"
//  }
//
//  set {
//    name = "keycloak.persistence.dbName"
//    value = "keycloak"
//  }
//
//  set {
//    name = "keycloak.persistence.dbHost"
//    value = "keycloak-mysql"
//  }
//
//  set {
//    name = "keycloak.persistence.dbPort"
//    value = "3306"
//  }
//
//  set {
//    name = "keycloak.persistence.dbUser"
//    value = "keycloak"
//  }
//
// set {
//    name = "keycloak.persistence.dbPassword"
//    value = "1234567890o"
//  }
//
//  set {
//    name = "keycloak.service.type"
//    value = "NodePort"
//  }
//
//  set {
//    name = "keycloak.service.nodePort"
//    value = "31088"
//  }
  depends_on = [helm_release.keycloak-mysql]
}