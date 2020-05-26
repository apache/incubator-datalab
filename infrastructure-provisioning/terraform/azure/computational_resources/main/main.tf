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

provider "azurerm" {
  subscription_id = var.subscription_id
  client_id       = var.client_id
  client_secret   = var.client_secret
  tenant_id       = var.tenant_id
}

module "notebook" {
  source           = "../modules/notebook"
  sbn              = var.service_base_name
  project_name     = var.project_name
  project_tag      = var.project_tag
  endpoint_tag     = var.endpoint_tag
  user_tag         = var.user_tag
  custom_tag       = var.custom_tag
  os_env           = var.os_env
  notebook_name    = var.notebook_name
  region           = var.region
  subnet_id        = var.subnet_id
  nb-sg_id         = var.nb-sg_id
  product          = var.product_name
  ami              = var.ami
  custom_ami       = var.custom_ami
  instance_type    = var.instance_type
  ssh_key          = var.ssh_key
  initial_user     = var.initial_user
  resource_group   = var.resource_group
}

module "data_engine" {
  source           = "../modules/data_engine"
  sbn              = var.service_base_name
  project_name     = var.project_name
  project_tag      = var.project_tag
  endpoint_tag     = var.endpoint_tag
  user_tag         = var.user_tag
  custom_tag       = var.custom_tag
  notebook_name    = var.notebook_name
  region           = var.region
  subnet_id        = var.subnet_id
  nb-sg_id         = var.nb-sg_id
  product          = var.product_name
  ami              = var.ami
  master_shape     = var.master_shape
  slave_shape      = var.slave_shape
  ssh_key          = var.ssh_key
  initial_user     = var.initial_user
  cluster_name     = var.cluster_name
  slave_count      = var.slave_count
  resource_group   = var.resource_group
}