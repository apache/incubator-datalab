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
  ssn_vpc_name      = "${var.service_base_name}-ssn-vpc"
  ssn_subnet_name   = "${var.service_base_name}-ssn-subnet"
}

resource "google_compute_network" "ssn_gke_vpc" {
  count                   = var.vpc_name == "" ? 1 : 0
  name                    = local.ssn_vpc_name
  auto_create_subnetworks = false
}

data "google_compute_network" "ssn_gke_vpc_data" {
  name = var.vpc_name == "" ? google_compute_network.ssn_gke_vpc.0.name : var.vpc_name
}

resource "google_compute_subnetwork" "ssn_gke_subnet" {
  count         = var.subnet_name == "" ? 1 : 0
  name          = local.ssn_subnet_name
  ip_cidr_range = var.subnet_cidr
  region        = var.region
  network       = data.google_compute_network.ssn_gke_vpc_data.self_link
}

data "google_compute_subnetwork" "ssn_gke_subnet_data" {
  name   = var.subnet_name == "" ? google_compute_subnetwork.ssn_gke_subnet.0.name : var.subnet_name
  region = var.region
}

