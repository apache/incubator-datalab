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

variable "namespace_name" {
    default = "datalab"
}

variable "credentials_file_path" {
  default = ""
}

variable "project_id" {
  default = ""
}

variable "region" {
  default = "us-west1"
}

variable "zone" {
  default = "us-west1-a"
}

variable "vpc_name" {
  default = ""
}

variable "subnet_name" {
  default = ""
}

variable "service_base_name" {
  default = "datalab-k8s"
}

variable "subnet_cidr" {
  default = "172.31.0.0/24"
}

variable "additional_tag" {
  default = "product:datalab"
}

variable "ssn_k8s_workers_count" {
  default = 1
}

variable "gke_cluster_version" {
  default = "1.14.10-gke.50"
}

// Couldn't assign in GCP
//variable "tag_resource_id" {
//  default = "user:tag"
//}

variable "ssn_k8s_workers_shape" {
  default = "n1-standard-2"
}

variable "service_account_iam_roles" {
  default = [
    "roles/logging.logWriter",
    "roles/monitoring.metricWriter",
    "roles/monitoring.viewer",
    "roles/storage.objectViewer",
    "roles/iam.serviceAccountTokenCreator",
    "roles/iam.serviceAccountKeyAdmin",
    "roles/dns.admin"
  ]
}

variable "ssn_k8s_alb_dns_name" {
    default = ""
}

variable "keycloak_user" {
  default = "datalab-admin"
}

variable "mysql_user" {
    default = "keycloak"
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

variable "ldap_users_group" {
    default = "ou=People"
}

variable "ldap_dn" {
    default = "dc=example,dc=com"
}

variable "ldap_user" {
    default = "cn=admin"
}

variable "ldap_bind_creds" {
    default = ""
}

variable "ldap_host" {
    default = ""
}

variable "mongo_db_username" {
    default = "admin"
}

variable "mongo_dbname" {
  default = "datalabdb"
}

variable "mongo_image_tag" {
    default = "4.2.4-debian-10-r0"
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

# variable "endpoint_eip_address" {}

variable "env_os" {
  default = "debian"
}

variable "big_query_dataset" {
  default = ""
}

variable "custom_certs_enabled" {
    default = "False"
}

variable "custom_cert_path" {
    default = ""
}

variable "custom_key_path" {
    default = ""
}

variable "custom_certs_host" {
    default = ""
}

variable "mysql_disk_size" {
    default = "10"
}

variable "domain" {
  default = ""
}

variable "keycloak_realm_name" {
  default = "datalab"
}

variable "keycloak_client_id" {
  default = "datalab-ui"
}
