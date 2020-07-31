# *****************************************************************************
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#
# ******************************************************************************

provider "google" {
  credentials = "${var.credentials}"
  project = "${var.project_name}"
  region = "${var.region_var}"
  zone = "${var.zone_var}"
}

module "common" {
  source = "../modules/common"
  project_tag       = "${var.project_tag}"
  endpoint_tag      = "${var.endpoint_tag}"
  user_tag          = "${var.user_tag}"
  custom_tag        = "${var.custom_tag}"
  product           = "${var.product_name}"
  region            = "${var.region_var}"
  vpc_name          = "${var.vpc_name}"
  fw_ingress        = "${var.fw_ingress}"
  fw_egress_public  = "${var.fw_egress_public}"
  fw_egress_private = "${var.fw_egress_private}"
  network_tag       = "${var.network_tag}"
  cidr_range        = "${var.cidr_range}"
  traefik_cidr      = "${var.traefik_cidr}"
}

module "notebook" {
  source          = "../modules/notebook"
  project_tag     = "${var.project_tag}"
  endpoint_tag    = "${var.endpoint_tag}"
  user_tag        = "${var.user_tag}"
  custom_tag      = "${var.custom_tag}"
  product         = "${var.product_name}"
  notebook_name   = "${var.notebook_name}"
  zone_var        = "${var.zone_var}"
  vpc_name        = "${var.vpc_name}"
  subnet_name     = "${var.subnet_name}"
  network_tag     = "${var.network_tag}"
  sa_email        = "${var.sa_email}"
  ami             = "${var.ami}"
  machine_type    = "${var.machine_type}"
  ssh_key         = "${var.ssh_key}"
  gpu_accelerator = "${var.gpu_accelerator}"
}

module "data_engine" {
  source          = "../modules/data_engine"
  project_tag     = "${var.project_tag}"
  endpoint_tag    = "${var.endpoint_tag}"
  user_tag        = "${var.user_tag}"
  custom_tag      = "${var.custom_tag}"
  product         = "${var.product_name}"
  notebook_name   = "${var.notebook_name}"
  zone_var        = "${var.zone_var}"
  vpc_name        = "${var.vpc_name}"
  subnet_name     = "${var.subnet_name}"
  network_tag     = "${var.network_tag}"
  sa_email        = "${var.sa_email}"
  ami             = "${var.ami}"
  ssh_key         = "${var.ssh_key}"
  gpu_accelerator = "${var.gpu_accelerator}"
  cluster_name    = "${var.cluster_name}"
  total_count     = "${var.total_count}"
  master_shape    = "${var.master_shape}"
  slave_shape     = "${var.slave_shape}"
}

module "dataproc" {
  source            = "../modules/dataproc"
  region            = "${var.region_var}"
  project_tag       = "${var.project_tag}"
  endpoint_tag      = "${var.endpoint_tag}"
  user_tag          = "${var.user_tag}"
  custom_tag        = "${var.custom_tag}"
  product           = "${var.product_name}"
  notebook_name     = "${var.notebook_name}"
  zone_var          = "${var.zone_var}"
  vpc_name          = "${var.vpc_name}"
  subnet_name       = "${var.subnet_name}"
  network_tag       = "${var.network_tag}"
  sa_email          = "${var.sa_email}"
  ami               = "${var.ami}"
  ssh_key           = "${var.ssh_key}"
  gpu_accelerator   = "${var.gpu_accelerator}"
  cluster_name      = "${var.cluster_name}"
  total_count       = "${var.total_count}"
  master_shape      = "${var.master_shape}"
  slave_shape       = "${var.slave_shape}"
  preemptible_count = "${var.preemptible_count}"
}