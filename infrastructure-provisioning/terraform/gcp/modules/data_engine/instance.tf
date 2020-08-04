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
  notebook_name = "${var.project_tag}-nb-${var.notebook_name}"
  cluster_name = "${var.project_tag}-de-${var.notebook_name}-${var.cluster_name}"
}

resource "google_compute_instance" "master" {
  name = "${local.cluster_name}-m"
  machine_type = "${var.master_shape}"
  tags = [
    "${var.network_tag}"]
  zone         = "${var.zone_var}"

  boot_disk {
    initialize_params {
      image = "${var.ami}"
      size  = 30
    }
  }

  labels = {
    name          = "${local.cluster_name}-m"
    notebook_name = "${local.notebook_name}"
    project       = "${var.project_tag}"
    product       = "${var.product}"
    type          = "master"
    user          = "${var.user_tag}"
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


resource "google_compute_instance" "slave" {
  count        = "${var.total_count - 1}"
  name         = "${local.cluster_name}-s${count.index + 1}"
  machine_type = "${var.slave_shape}"
  tags         = ["${var.network_tag}"]
  zone         = "${var.zone_var}"

  boot_disk {
    initialize_params {
      image = "${var.ami}"
      size  = 30
    }
  }

  labels = {
    name          = "${local.cluster_name}-s${count.index + 1}"
    notebook_name = "${local.notebook_name}"
    project           = "${var.project_tag}"
    product       = "${var.product}"
    sbn           = "${var.project_tag}"
    type          = "slave"
    user          = "${var.user_tag}"
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