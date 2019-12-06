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
  endpoint_instance_name = "${var.service_base_name}-${var.endpoint_id}-endpoint"
  endpoint_instance_ip   = "${var.service_base_name}-${var.endpoint_id}-static-ip"
}

resource "google_compute_instance" "endpoint" {
  name         = local.endpoint_instance_name
  machine_type = var.endpoint_shape
  zone         = var.zone
  tags         = [replace(local.endpoint_instance_name, "_", "-")]
  labels       = {
    name        = local.endpoint_instance_name
    sbn         = var.service_base_name
    product     = var.product
    endpoint_id = var.endpoint_id
  }

  boot_disk {
    initialize_params {
      image = var.ami
      size  = var.endpoint_volume_size
    }
  }

  metadata = {
    ssh-keys = "ubuntu:${file(var.path_to_pub_key)}"
  }

  service_account {
    email  = google_service_account.endpoint_sa.email
    scopes = ["https://www.googleapis.com/auth/cloud-platform", "https://www.googleapis.com/auth/compute"]
  }

  network_interface {
    network    = data.google_compute_network.endpoint_vpc_data.name
    subnetwork = data.google_compute_subnetwork.endpoint_subnet_data.name
    access_config {
      nat_ip = google_compute_address.static.address
    }
  }
}

resource "google_compute_address" "static" {
  name = local.endpoint_instance_ip
}
