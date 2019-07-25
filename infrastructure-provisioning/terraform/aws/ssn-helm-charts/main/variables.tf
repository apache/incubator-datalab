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

variable "ssn_k8s_alb_dns_name" {
    default = ""
}

variable "keycloak_user" {
    default = "dlab-admin"
}

variable "keycloak_password" {
    default = "keycloak123"
}

variable "mysql_root_password" {
    default = "mysqlroot123"
}

variable "mysql_user" {
    default = "keycloak"
}

variable "mysql_user_password" {
    default = "keycloak123"
}

variable "mysql_db_name" {
    default = "keycloak"
}

variable "ldap_usernameAttr" {
    default = "uid"
}

variable "ldap_rdnAttr" {
    default = "uid"
}

variable "ldap_uuidAttr" {
    default = "uid"
}

variable "ldap_users_dn" {
    default = "ou=People,dc=example,dc=com"
}

variable "ldap_bind_dn" {
    default = "cn=admin,dc=example,dc=com"
}

variable "ldap_bind_creds" {
    default = ""
}

variable "ldap_connection_url" {
    default = ""
}

variable "mongo_root_pwd" {
    default = "$tr0ng_r00T-passworI)"
    description = "Password for MongoDB root user"
}
variable "mongo_db_username" {
    default = "admin"
    description = "Password for MongoDB root user"
}

variable "mongo_dbname" {
    default = "dlabdb"
    description = "Password for MongoDB root user"
}

variable "mongo_db_pwd" {
    default = "$tr0ng_N0N=r00T-passworI)"
    description = "Password for MongoDB root user"
}

variable "mongo_image_tag" {
    default = "4.0.10-debian-9-r13"
    description = "MongoDB Image tag"
}

variable "mongo_service_port" {
    default = "27017"
}

variable "mongo_node_port" {
    default = "31017"
}

variable "mongo_service_name" {
    default = "mongo-ha-mongodb"
}

variable "ssn_k8s_workers_count" {
    default = "2"
}

//variable "nginx_http_port" {
//    default = "31080"
//    description = "Sets the nodePort that maps to the Ingress' port 80"
//}
//variable "nginx_https_port" {
//    default = "31443"
//    description = "Sets the nodePort that maps to the Ingress' port 443"
//}