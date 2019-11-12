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

variable "namespace_name" {}

variable "mongo_dbname" {}

variable "mongo_db_username" {}

variable "mongo_service_port" {}

variable "mongo_service_name" {}

variable "ssn_k8s_alb_dns_name" {}

variable "service_base_name" {}

variable "ldap_host" {}

variable "ldap_dn" {}

variable "ldap_users_group" {}

variable "ldap_user" {}

variable "ldap_bind_creds" {}

variable "keycloak_user" {}

variable "ldap_usernameAttr" {}

variable "ldap_rdnAttr" {}

variable "ldap_uuidAttr" {}

variable "mysql_db_name" {}

variable "mysql_user" {}

variable "region" {}

variable "mongo_image_tag" {}

variable "mongo_node_port" {}

variable "ssn_keystore_password" {}

variable "endpoint_keystore_password" {}

variable "gke_cluster_name" {}

variable "big_query_dataset" {}

variable "env_os" {}
//variable "nginx_http_port" {
//    default = "31080"
//    description = "Sets the nodePort that maps to the Ingress' port 80"
//}
//variable "nginx_https_port" {
//    default = "31443"
//    description = "Sets the nodePort that maps to the Ingress' port 443"
//}