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
  edge_instance_ip = "${var.service_base_name}-${var.project_tag}-edge-ip"
  ps_name          = "${var.service_base_name}-${var.project_tag}-private-subnet"
  ps_tag           = "${var.service_base_name}-${var.project_tag}-ps"
  edge_ingress     = "${var.service_base_name}-${var.project_tag}-edge-ingress"
  edge_egress      = "${var.service_base_name}-${var.project_tag}-edge-egress"
  ps_ingress       = "${var.service_base_name}-${var.project_tag}-ps-ingress"
  ps_egress        = "${var.service_base_name}-${var.project_tag}-ps-egress"
}

#################
### Edge node ###
#################

resource "google_compute_address" "edge_ip" {
  name = local.edge_instance_ip
}

resource "google_compute_firewall" "edge_ingress-public" {
  name    = "${local.edge_ingress}-public"
  network = var.vpc_name
  allow {
    protocol = "tcp"
    ports    = ["22", "8084", "8085"]
  }
  target_tags   = ["${local.edge_instance_name}"]
  source_ranges = ["0.0.0.0/0"]
}

resource "google_compute_firewall" "edge_ingress_internal" {
  name    = "${local.edge_ingress}-internal"
  network = var.vpc_name
  allow {
    protocol = "all"
  }
  target_tags   = ["${local.edge_instance_name}"]
  source_ranges = [var.ps_cidr]
}

resource "google_compute_firewall" "edge_egress_public" {
  name      = "${local.edge_egress}-public"
  network   = var.vpc_name
  direction = "EGRESS"
  allow {
    protocol = "udp"
    ports    = ["53", "123"]
  }
  allow {
    protocol = "tcp"
    ports    = ["22", "80", "443"]
  }
  target_tags        = [local.edge_instance_name]
  destination_ranges = ["0.0.0.0/0"]
}

resource "google_compute_firewall" "edge_egress_internal" {
  name      = "${local.edge_egress}-internal"
  network   = var.vpc_name
  direction = "EGRESS"
  allow {
    protocol = "tcp"
    ports    = ["22", "389", "8888", "8080", "8787", "6006", "20888", "8042", "8088", "18080", "50070",
                          "8085", "8081", "4040-4045"]
  }
  target_tags        = [local.edge_instance_name]
  destination_ranges = [var.ps_cidr]
}

############################################################
### Explotratory environment and computational resources ###
############################################################

resource "google_compute_subnetwork" "private_subnet" {
  name          = local.ps_name
  ip_cidr_range = var.ps_cidr
  region        = var.region
  network       = var.vpc_name
}

resource "google_compute_firewall" "ps-ingress" {
  name    = local.ps_ingress
  network = var.vpc_name
  allow {
    protocol = "all"
  }
  target_tags   = [local.ps_tag]
  source_ranges = [var.ps_cidr, var.ssn_subnet_cidr]

}

resource "google_compute_firewall" "ps-egress-private" {
  name      = "${local.ps_egress}-private"
  network   = var.vpc_name
  direction = "EGRESS"
  allow {
    protocol = "all"
  }
  target_tags        = [local.ps_tag]
  destination_ranges = [var.ps_cidr, var.ssn_subnet_cidr]
}

resource "google_compute_firewall" "ps-egress-public" {
  name      = "${local.ps_egress}-public"
  network   = var.vpc_name
  direction = "EGRESS"
  allow {
    protocol = "tcp"
    ports = ["443"]
  }
  target_tags        = [local.ps_tag]
  destination_ranges = ["0.0.0.0/0"]
}