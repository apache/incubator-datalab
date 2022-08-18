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
  vpc_id                = "${var.service_base_name}-${var.endpoint_id}-vpc"
  subnet_name           = "${var.service_base_name}-${var.endpoint_id}-subnet"
  firewall_ingress_name = "${var.service_base_name}-${var.endpoint_id}-ingress-sg"
  firewall_egress_name  = "${var.service_base_name}-${var.endpoint_id}-egress-sg"
}

resource "google_compute_network" "endpoint_vpc" {
  count = var.vpc_id == "" ? 1 : 0
  name                    = local.vpc_id
  auto_create_subnetworks = false
}

data "google_compute_network" "endpoint_vpc_data" {
  name = var.vpc_id == "" ? google_compute_network.endpoint_vpc.0.name : var.vpc_id
}

resource "google_compute_subnetwork" "endpoint_subnet" {
  count         = var.subnet_id == "" ? 1 : 0
  name          = local.subnet_name
  ip_cidr_range = var.subnet_cidr
  region        = var.region
  network       = data.google_compute_network.endpoint_vpc_data.id
}

data "google_compute_subnetwork" "endpoint_subnet_data" {
  name = var.subnet_id == "" ? google_compute_subnetwork.endpoint_subnet.0.name : var.subnet_id
}

resource "google_compute_firewall" "firewall-ingress" {
  name    = local.firewall_ingress_name
  network = data.google_compute_network.endpoint_vpc_data.name
  allow {
    protocol = "tcp"
    ports    = ["22", "8084", "8085", "4822", "8088"]
  }
  target_tags   = ["${var.service_base_name}-${var.endpoint_id}-endpoint"]
  source_ranges = var.firewall_ing_cidr_range

}

resource "google_compute_firewall" "firewall-egress" {
  name      = local.firewall_egress_name
  network   = data.google_compute_network.endpoint_vpc_data.name
  direction = "EGRESS"
  allow {
    protocol = "all"
  }
  target_tags        = ["${var.service_base_name}-${var.endpoint_id}-endpoint"]
  destination_ranges = var.firewall_eg_cidr_range
}
