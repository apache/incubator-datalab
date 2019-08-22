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
  subnet_c_id                      = data.google_compute_subnetwork.k8s-subnet-c-data == [] ? "" : data.google_compute_subnetwork.k8s-subnet-c-data.0.name
  ssn_k8s_launch_conf_masters_name = "${var.service_base_name}-ssn-launch-conf-masters"
  ssn_k8s_launch_conf_workers_name = "${var.service_base_name}-ssn-launch-conf-workers"
  ssn_k8s_ag_masters_name          = "${var.service_base_name}-ssn-masters"
  ssn_k8s_ag_workers_name          = "${var.service_base_name}-ssn-workers"
  ssn_k8s_masters_igm              = "${var.service_base_name}-ssn-igm-masters"
  ssn_k8s_slaves_igm               = "${var.service_base_name}-ssn-igm-slves"
}

resource "random_string" "ssn_keystore_password" {
  length = 16
  special = false
}

resource "random_string" "endpoint_keystore_password" {
  length = 16
  special = false
}

data "template_file" "ssn_k8s_masters_user_data" {
  template = file("./files/masters-user-data.sh")
  vars = {
    k8s-asg                    = local.ssn_k8s_ag_masters_name
    k8s-region                 = var.region
    k8s-bucket-name            = google_storage_bucket.ssn_k8s_bucket.id
    k8s-nlb-dns-name           = aws_lb.ssn_k8s_nlb.dns_name
    k8s-tg-arn                 = aws_lb_target_group.ssn_k8s_nlb_api_target_group.arn
    k8s_os_user                = var.os_user
    ssn_keystore_password      = random_string.ssn_keystore_password.result
    endpoint_keystore_password = random_string.endpoint_keystore_password.result
    endpoint_elastic_ip        = google_compute_address.k8s-endpoint-eip.address
  }
}

resource "google_compute_autoscaler" "master_group" {
  name = local.ssn_k8s_ag_masters_name
  target = ""
  autoscaling_policy {
    max_replicas = var.ssn_k8s_masters_count
    min_replicas = var.ssn_k8s_masters_count
  }
}

resource "google_compute_instance_template" "masters_template" {
  name = local.ssn_k8s_launch_conf_masters_name
  machine_type = var.ssn_k8s_masters_shape
  disk {
    source_image = var.ami
  }
  network_interface {
    network = var.vpc_id
    subnetwork = compact([data.google_compute_subnetwork.k8s-subnet-a-data.name, data.google_compute_subnetwork.k8s-subnet-b-data.name, local.subnet_c_id])
  }

  service_account {
    email = google_service_account.ssn_k8s_sa.email
    scopes = ["https://www.googleapis.com/auth/cloud-platform", "https://www.googleapis.com/auth/compute"]
  }
}

resource "google_compute_target_pool" "ssn_target_pool" {
  provider = "google-beta"
  name = "${var.service_base_name}-target-pool"
}

resource "google_compute_instance_group_manager" "masters_igm" {
  provider = "google-beta"

  name = local.ssn_k8s_masters_igm
  zone = var.zone

  instance_template = google_compute_instance_template.masters_template.self_link

  target_pools       = ["${google_compute_target_pool.ssn_target_pool.self_link}"]
  base_instance_name = "autoscaler-sample"
}

resource "google_compute_autoscaler" "master_group" {
  name = local.ssn_k8s_ag_masters_name
  target = ""
  autoscaling_policy {
    max_replicas = var.ssn_k8s_masters_count
    min_replicas = var.ssn_k8s_masters_count
  }
}

resource "google_compute_instance_template" "slaves_template" {
  name = local.ssn_k8s_launch_conf_masters_name
  machine_type = var.ssn_k8s_masters_shape
  disk {
    source_image = var.ami
  }
  network_interface {
    network = var.vpc_id
    subnetwork = compact([data.google_compute_subnetwork.k8s-subnet-a-data.name, data.google_compute_subnetwork.k8s-subnet-b-data.name, local.subnet_c_id])
  }

  service_account {
    email = google_service_account.ssn_k8s_sa.email
    scopes = ["https://www.googleapis.com/auth/cloud-platform", "https://www.googleapis.com/auth/compute"]
  }
}

resource "google_compute_instance_group_manager" "slaves_igm" {
  provider = "google-beta"

  name = local.ssn_k8s_slaves_igm
  zone = var.zone

  instance_template = google_compute_instance_template.slaves_template.self_link

  target_pools       = ["${google_compute_target_pool.ssn_target_pool.self_link}"]
  base_instance_name = "autoscaler-sample"
}

provider "google-beta"{
  region = var.region
  zone   = var.zone
}