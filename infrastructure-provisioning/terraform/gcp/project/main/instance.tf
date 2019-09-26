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
  edge_instance_name = "${var.service_base_name}-${var.project_tag}-edge"
}

resource "google_compute_instance" "endpoint" {
  name         = local.edge_instance_name
  machine_type = var.edge_shape
  tags         = ["${replace("${local.edge_instance_name}", "_", "-")}"]
  labels       = {
    name        = "${local.edge_instance_name}"
    sbn         = "${var.service_base_name}"
    product     = "${var.product}"
    endpoint_id = "${var.endpoint_tag}"
    project_tag = var.project_tag
  }
  zone         = var.zone

  boot_disk {
    initialize_params {
      image = var.ami
      size  = var.edge_volume_size
    }
  }

  metadata = {
    ssh-keys = "ubuntu:${file(var.path_to_pub_key)}" # Format the file before deploy
  }

  service_account {
    email  = google_service_account.edge_sa.email #"${var.project_name_var}-ssn-sa@${var.project_var}.iam.gserviceaccount.com"
    scopes = ["https://www.googleapis.com/auth/cloud-platform", "https://www.googleapis.com/auth/compute"]
  }

  network_interface {
    network    = var.vpc_name
    subnetwork = var.ssn_subnet_name
    access_config {
      nat_ip = google_compute_address.edge_ip.address
    }
  }
}