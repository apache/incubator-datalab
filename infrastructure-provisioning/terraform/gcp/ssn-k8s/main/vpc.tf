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

locals {
  additional_tag        = split(":", var.additional_tag)
  ssn_vpc_name          = "${var.service_base_name}-ssn-vpc"
  ssn_igw_name          = "${var.service_base_name}-ssn-igw"
  ssn_subnet_a_name     = "${var.service_base_name}-ssn-subnet-az-a"
  ssn_subnet_b_name     = "${var.service_base_name}-ssn-subnet-az-b"
  ssn_subnet_c_name     = "${var.service_base_name}-ssn-subnet-az-c"
  endpoint_ip_name      = "${var.service_base_name}-endpoint-eip"
  endpoint_rt_name      = "${var.service_base_name}-endpoint-rt"
  endpoint_bucket_name  = "${var.service_base_name}-bucket-endpoint"
}

resource "google_compute_network" "ssn_k8s_vpc" {
  count = var.vpc_id == "" ? 1 : 0
  name = local.ssn_vpc_name
  auto_create_subnetworks = false
}

resource "google_compute_route" "ssn_k8s_route" {
  count       = var.vpc_id == "" ? 1 : 0
  name        = "${var.service_base_name}-route"
  dest_range  = "0.0.0.0/0"
  network     = data.google_compute_network.ssn_k8s_vpc_data.id
  priority    = 100
}

data "google_compute_network" "ssn_k8s_vpc_data" {
  name = google_compute_network.ssn_k8s_vpc.0.name
}


resource "google_compute_subnetwork" "ssn_k8s_subnet_a" {
  count         = var.subnet_id_a == "" ? 1 : 0
  ip_cidr_range = var.subnet_cidr_a
  name          = local.ssn_subnet_a_name
  network       = data.google_compute_network.ssn_k8s_vpc_data.id
}

resource "google_compute_subnetwork" "ssn_k8s_subnet_b" {
  count         = var.subnet_id_b == "" ? 1 : 0
  ip_cidr_range = var.subnet_cidr_b
  name          = local.ssn_subnet_b_name
  network       = data.google_compute_network.ssn_k8s_vpc_data.id
}

resource "google_compute_subnetwork" "ssn_k8s_subnet_c" {
  count         = var.ssn_k8s_masters_count > 2 ? 1 : 0
  ip_cidr_range = var.subnet_cidr_c
  name          = local.ssn_subnet_c_name
  network       = data.google_compute_network.ssn_k8s_vpc_data.id
}

data "google_compute_subnetwork" "k8s-subnet-a-data" {
  name = google_compute_subnetwork.ssn_k8s_subnet_a.0.name
}

data "google_compute_subnetwork" "k8s-subnet-b-data" {
  name = google_compute_subnetwork.ssn_k8s_subnet_b.0.name
}

data "google_compute_subnetwork" "k8s-subnet-c-data" {
  count = var.ssn_k8s_masters_count > 2 ? 1 : 0
  name = google_compute_subnetwork.ssn_k8s_subnet_c.0.name
}

resource "google_compute_address" "k8s-endpoint-eip" {
  name = local.endpoint_ip_name
}


resource "google_compute_network_endpoint_group" "ssn-k8s-users-bucket-endpoint" {
  name = local.endpoint_bucket_name
  network = data.google_compute_network.ssn_k8s_vpc_data.id
}
