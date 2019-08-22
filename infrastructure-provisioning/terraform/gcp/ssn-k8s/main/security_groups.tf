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
  ssn_ingress_name = "${var.service_base_name}-ssn-ingress"
  ssn_egress_name = "${var.service_base_name}-ssn-egress"

}

resource "google_compute_firewall" "ssn_k8s_ingress" {
  name    = local.ssn_ingress_name
  network = data.google_compute_network.ssn_k8s_vpc_data.name
  allow {
    protocol = "all"
  }
  target_tags = ["${var.ssn_net_tag}"]
  source_ranges = ["${var.vpc_cidr}"]
}

resource "google_compute_firewall" "ssn_k8s_ssh" {
  name = "${local.ssn_ingress_name}-ssh"
  network = data.google_compute_network.ssn_k8s_vpc_data.name
  allow {
    protocol = "tcp"
    ports = ["22"]
  }
}

resource "google_compute_firewall" "ssn_k8s_ingress_all" {
  name = "${local.ssn_ingress_name}-all"
  network = data.google_compute_network.ssn_k8s_vpc_data.name
  allow {
    protocol = "all"
  }
  target_tags        = ["${var.ssn_net_tag}"]
  destination_ranges = ["0.0.0.0/0"]
}

resource "google_compute_firewall" "ssn_k8s_egress" {
  name = local.ssn_egress_name
  network = data.google_compute_network.ssn_k8s_vpc_data.name
  direction = "EGRESS"
  allow {
    protocol = "all"
  }
  target_tags        = ["${var.ssn_net_tag}"]
  destination_ranges = ["0.0.0.0/0"]
}