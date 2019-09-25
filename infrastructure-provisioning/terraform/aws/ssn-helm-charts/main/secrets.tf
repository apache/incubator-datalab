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

resource "kubernetes_secret" "keycloak_client_secret" {
  metadata {
    name = "keycloak-client-secret"
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
  }

  data = {
    password = random_string.mysql_keycloak_user_password.result
  }
}

resource "random_string" "mysql_guacamole_user_password" {
  length = 16
  special = false
}

resource "kubernetes_secret" "mysql_guacamole_user_password_secret" {
  metadata {
    name = "mysql-guacamole-user-password"
  }

  data = {
    password = random_string.mysql_guacamole_user_password.result
  }
}

resource "kubernetes_secret" "ssn_keystore_password" {
  metadata {
    name = "ssn-keystore-password"
  }

  data = {
    password = var.ssn_keystore_password
  }
}

resource "kubernetes_secret" "endpoint_keystore_password" {
  metadata {
    name = "endpoint-keystore-password"
  }

  data = {
    password = var.endpoint_keystore_password
  }
}