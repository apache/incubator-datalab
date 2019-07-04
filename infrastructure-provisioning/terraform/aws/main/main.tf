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

provider "aws" {
  region     = var.region
  access_key = var.access_key_id
  secret_key = var.secret_access_key
}

module "ssn-k8s" {
  source                = "../modules/ssn-k8s"
  service_base_name     = var.service_base_name
  vpc_id                = var.vpc_id
  vpc_cidr              = var.vpc_cidr
  subnet_id             = var.subnet_id
  env_os                = var.env_os
  ami                   = var.ami
  key_name              = var.key_name
  region                = var.region
  zone                  = var.zone
  ssn_k8s_masters_count = var.ssn_k8s_masters_count
  ssn_k8s_workers_count = var.ssn_k8s_workers_count
  ssn_root_volume_size  = var.ssn_root_volume_size
  allowed_cidrs         = var.allowed_cidrs
  subnet_cidr           = var.subnet_cidr
  ssn_k8s_masters_shape = var.ssn_k8s_masters_shape
  ssn_k8s_workers_shape = var.ssn_k8s_workers_shape
  os-user               = var.os-user
}

module "common" {
  source        = "../modules/common"
  project_tag   = "${var.project_tag}"
  endpoint_tag  = "${var.endpoint_tag}"
  user_tag      = "${var.user_tag}"
  custom_tag    = "${var.custom_tag}"
  notebook_name = "${var.notebook_name}"
  region        = "${var.region}"
  zone          = "${var.zone}"
  product       = "${var.product_name}"
  vpc           = "${var.vpc_id}"
  cidr_range    = "${var.note_cidr_range}"
  traefik_cidr  = "${var.traefik_cidr}"
  instance_type = "${var.instance_type}"
}

module "notebook" {
  source            = "../modules/notebook"
  project_tag       = "${var.project_tag}"
  endpoint_tag      = "${var.endpoint_tag}"
  user_tag          = "${var.user_tag}"
  custom_tag        = "${var.custom_tag}"
  notebook_name     = "${var.notebook_name}"
  subnet_id         = "${var.subnet_id}"
  nb-sg_id          = "${var.nb-sg_id}"
  note_profile_name = "${var.note_profile_name}"
  product           = "${var.product_name}"
  note_ami          = "${var.note_ami}"
  instance_type     = "${var.instance_type}"
  key_name          = "${var.key_name}"
}

module "data_engine" {
  source            = "../modules/data_engine"
  project_tag       = "${var.project_tag}"
  endpoint_tag      = "${var.endpoint_tag}"
  user_tag          = "${var.user_tag}"
  custom_tag        = "${var.custom_tag}"
  notebook_name     = "${var.notebook_name}"
  subnet_id         = "${var.subnet_id}"
  nb-sg_id          = "${var.nb-sg_id}"
  note_profile_name = "${var.note_profile_name}"
  product           = "${var.product_name}"
  note_ami          = "${var.note_ami}"
  instance_type     = "${var.instance_type}"
  key_name          = "${var.key_name}"
  cluster_name      = "${var.cluster_name}"
  slave_count       = "${var.slave_count}"
  ami               = "${var.ami}"
}

module "emr" {
  source            = "../modules/emr"
  project_tag       = "${var.project_tag}"
  endpoint_tag      = "${var.endpoint_tag}"
  user_tag          = "${var.user_tag}"
  custom_tag        = "${var.custom_tag}"
  notebook_name     = "${var.notebook_name}"
  subnet_id         = "${var.subnet_id}"
  nb-sg_id          = "${var.nb-sg_id}"
  note_profile_name = "${var.note_profile_name}"
  product           = "${var.product_name}"
  note_ami          = "${var.note_ami}"
  emr_template      = "${var.emr_template}"
  master_shape      = "${var.master_shape}"
  slave_shape       = "${var.slave_shape}"
  key_name          = "${var.key_name}"
  cluster_name      = "${var.cluster_name}"
  instance_count    = "${var.instance_count}"
  bid_price         = "${var.bid_price}"
}

module "ami" {
  source             = "../modules/ami"
  source_instance_id = "${var.source_instance_id}"
  project_tag        = "${var.project_tag}"
  notebook_name      = "${var.notebook_name}"
}

module "endpoint" {
  source                  = "../modules/endpoint"
  service_base_name       = var.service_base_name
  region                  = var.region
  zone                    = var.zone
  product                 = var.product_name
  subnet_cidr             = var.subnet_cidr
  endpoint_instance_shape = var.endpoint_instance_shape
  key_name                = var.key_name
  ami                     = var.ami
  env_os                  = var.env_os
  vpc_id                  = var.vpc_id
  subnet_id               = var.subnet_id
  network_type            = var.network_type
  vpc_cidr                = var.vpc_cidr
}