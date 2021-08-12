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

resource "random_uuid" "keycloak_client_secret" {}

resource "random_string" "ssn_keystore_password" {
  length = 16
  special = false
}

resource "kubernetes_secret" "keycloak_client_secret" {
  metadata {
    name = "keycloak-client-secret"
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  }

  data = {
    client_secret = random_uuid.keycloak_client_secret.result
  }
}

resource "random_string" "keycloak_password" {
  length = 16
  special = false
}


resource "kubernetes_secret" "keycloak_password_secret" {
  metadata {
    name = "keycloak-password"
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  }

  data = {
    password = random_string.keycloak_password.result
  }
}

resource "random_string" "mongo_root_password" {
  length = 16
  special = false
}

resource "kubernetes_secret" "mongo_root_password_secret" {
  metadata {
    name = "mongo-root-password"
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  }

  data = {
    password = random_string.mongo_root_password.result
  }
}

resource "random_string" "mongo_db_password" {
  length = 16
  special = false
}

resource "kubernetes_secret" "mongo_db_password_secret" {
  metadata {
    name = "mongo-db-password"
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  }

  data = {
    password = random_string.mongo_db_password.result
  }
}

resource "random_string" "mysql_root_password" {
  length = 16
  special = false
}

resource "kubernetes_secret" "mysql_root_password_secret" {
  metadata {
    name = "mysql-root-password"
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  }

  data = {
    password = random_string.mysql_root_password.result
  }
}

resource "random_string" "mysql_keycloak_user_password" {
  length = 16
  special = false
}

resource "kubernetes_secret" "mysql_keycloak_user_password_secret" {
  metadata {
    name = "mysql-keycloak-user-password"
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  }

  data = {
    password = random_string.mysql_keycloak_user_password.result
  }
}

resource "kubernetes_secret" "ssn_keystore_password" {
  metadata {
    name = "ssn-keystore-password"
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  }

  data = {
    password = random_string.ssn_keystore_password.result
  }
}

resource "random_string" "step_ca_password" {
  length = 8
  special = false
}

resource "kubernetes_secret" "step_ca_password_secret" {
  metadata {
    name = "step-ca-password"
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  }

  data = {
    password = random_string.step_ca_password.result
  }
}

resource "random_string" "step_ca_provisioner_password" {
  length = 8
  special = false
}

resource "kubernetes_secret" "step_ca_provisioner_password_secret" {
  metadata {
    name = "step-ca-provisioner-password"
    namespace = kubernetes_namespace.datalab-namespace.metadata[0].name
  }

  data = {
    password = random_string.step_ca_provisioner_password.result
  }
}