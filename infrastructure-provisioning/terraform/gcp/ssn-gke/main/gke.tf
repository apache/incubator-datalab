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
  additional_tag       = split(":", var.additional_tag)
  gke_name = "${var.service_base_name}-cluster"
  gke_node_pool_name = "${var.service_base_name}-node-pool"
}

resource "google_container_cluster" "ssn_k8s_gke_cluster" {
  name     = local.gke_name
  location = var.region
  remove_default_node_pool = true
  initial_node_count = 1
  min_master_version = var.gke_cluster_version
  network = data.google_compute_network.ssn_gke_vpc_data.self_link
  subnetwork = data.google_compute_subnetwork.ssn_gke_subnet_data.self_link
  resource_labels = {
    name                              = local.gke_name
    "${local.additional_tag[0]}"      = local.additional_tag[1]
    # "${var.tag_resource_id}"          = "${var.service_base_name}:${local.gke_name}"
    "${var.service_base_name}-tag"    = local.gke_name
  }

  master_auth {
    username = ""
    password = ""

    client_certificate_config {
      issue_client_certificate = false
    }
  }
}

resource "google_container_node_pool" "ssn_k8s_gke_node_pool" {
  name       = local.gke_node_pool_name
  location   = var.region
  cluster    = google_container_cluster.ssn_k8s_gke_cluster.name
  node_count = var.ssn_k8s_workers_count
  version    = var.gke_cluster_version

  node_config {
    machine_type = var.ssn_k8s_workers_shape
    service_account = google_service_account.ssn_k8s_sa.name

    metadata = {
      disable-legacy-endpoints = "true"
    }

    oauth_scopes = [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
    ]
  }
}