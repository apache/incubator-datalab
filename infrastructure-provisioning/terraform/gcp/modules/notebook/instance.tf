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

locals {
  name = "${var.project_tag}-nb-${var.notebook_name}"
}

resource "google_compute_disk" "secondary" {
  name = "${local.name}-secondary"
  zone = "${var.zone_var}"
  labels = {
    name = "${local.name}"
    product = "${var.product}"
    project = "${var.project_tag}"
    user    = "${var.user_tag}"
  }
  physical_block_size_bytes = 4096
  size                      = 30
}

resource "google_compute_instance" "notebook" {
  name         = "${local.name}"
  machine_type = "${var.machine_type}"
  tags         = ["${var.network_tag}"]
  zone         = "${var.zone_var}"

  boot_disk {
    initialize_params {
      image = "${var.ami}"
      size  = 12
    }
  }

  attached_disk {
    source = "${google_compute_disk.secondary.self_link}"
  }

  labels = {
    name    = "${local.name}"
    product = "${var.product}"
    project = "${var.project_tag}"
    user    = "${var.user_tag}"
  }

  metadata = {
    ssh-keys = "ubuntu:${file("${var.ssh_key}")}"
  }

  service_account {
    email  = "${var.sa_email}"
    scopes = ["https://www.googleapis.com/auth/cloud-platform", "https://www.googleapis.com/auth/compute"]
  }

  network_interface {
    network    = "${var.vpc_name}"
    subnetwork = "${var.subnet_name}"
  }

  guest_accelerator {
    count = "${var.gpu_accelerator != "false" ? 1 : 0}"
    type  = "nvidia-tesla-k80"
  }

  scheduling {
    on_host_maintenance = "${var.gpu_accelerator != "false" ? "TERMINATE" : "MIGRATE"}"
  }

}