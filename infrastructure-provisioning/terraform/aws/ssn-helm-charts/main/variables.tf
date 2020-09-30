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

variable "ssn_k8s_nlb_dns_name" {
    default = ""
}

variable "keycloak_user" {
  default = "datalab-admin"
}

variable "mysql_keycloak_user" {
    default = "keycloak"
}

variable "mysql_keycloak_db_name" {
    default = "keycloak"
}

variable "mysql_disk_size" {
    default = "10"
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

//variable "endpoint_eip_address" {}

variable "service_base_name" {
    default = ""
}

variable "tag_resource_id" {
    default = ""
}

variable "billing_bucket" {
    default = ""
}

variable "billing_bucket_path" {
    default = ""
}

variable "billing_aws_job_enabled" {
    default = "false"
}

variable "billing_aws_account_id" {
    default = ""
}

variable "billing_tag" {
  default = "datalab"
}

variable "billing_datalab_id" {
  default = "resource_tags_user_user_tag"
}

variable "billing_usage_date" {
    default = "line_item_usage_start_date"
}

variable "billing_product" {
    default = "product_product_name"
}

variable "billing_usage_type" {
    default = "line_item_usage_type"
}

variable "billing_usage" {
    default = "line_item_usage_amount"
}

variable "billing_cost" {
    default = "line_item_blended_cost"
}

variable "billing_resource_id" {
    default = "line_item_resource_id"
}

variable "billing_tags" {
    default = "line_item_operation,line_item_line_item_description"
}

variable "env_os" {
    default = "debian"
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

variable "keycloak_realm_name" {
  default = "datalab"
}

variable "keycloak_client_id" {
  default = "datalab-ui"
}