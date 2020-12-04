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

provider "google" {
  version     = "3.3.0"
  credentials = file(var.credentials_file_path)
  project     = var.project_id
  region      = var.region
  zone        = var.zone
}

module "gke_cluster" {
  source                    = "./modules/gke"
  additional_tag            = var.additional_tag
  service_base_name         = var.service_base_name
  region                    = var.region
  gke_cluster_version       = var.gke_cluster_version
  ssn_k8s_workers_count     = var.ssn_k8s_workers_count
  ssn_k8s_workers_shape     = var.ssn_k8s_workers_shape
  project_id                = var.project_id
  service_account_iam_roles = var.service_account_iam_roles
  vpc_name                  = var.vpc_name
  subnet_name               = var.subnet_name
  subnet_cidr               = var.subnet_cidr
}

module "helm_charts" {
  source = "./modules/helm_charts"
  mongo_dbname               = var.mongo_dbname
  mongo_db_username          = var.mongo_db_username
  mongo_service_port         = var.mongo_service_port
  mongo_service_name         = var.mongo_service_name
  ssn_k8s_alb_dns_name       = var.ssn_k8s_alb_dns_name
  service_base_name          = var.service_base_name
  ldap_host                  = var.ldap_host
  ldap_dn                    = var.ldap_dn
  ldap_users_group           = var.ldap_users_group
  ldap_user                  = var.ldap_user
  ldap_bind_creds            = var.ldap_bind_creds
  keycloak_user              = var.keycloak_user
  ldap_usernameAttr          = var.ldap_usernameAttr
  ldap_rdnAttr               = var.ldap_rdnAttr
  ldap_uuidAttr              = var.ldap_uuidAttr
  mysql_db_name              = var.mysql_db_name
  mysql_user                 = var.mysql_user
  region                     = var.region
  mongo_image_tag            = var.mongo_image_tag
  mongo_node_port            = var.mongo_node_port
  gke_cluster_name           = module.gke_cluster.gke_cluster_name
  big_query_dataset          = var.big_query_dataset
  env_os                     = var.env_os
  namespace_name             = var.namespace_name
  credentials_file_path      = var.credentials_file_path
  project_id                 = var.project_id
  custom_certs_enabled       = var.custom_certs_enabled
  custom_cert_path           = var.custom_cert_path
  custom_certs_host          = var.custom_certs_host
  custom_key_path            = var.custom_key_path
  mysql_disk_size            = var.mysql_disk_size
  domain                     = var.domain
  keycloak_realm_name        = var.keycloak_realm_name
  keycloak_client_id         = var.keycloak_client_id
}