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
  dataproc_name = "${var.project_tag}-des-${var.notebook_name}-${var.cluster_name}"
}

resource "google_dataproc_cluster" "dataproc" {
  name = "${local.dataproc_name}"
  region = "${var.region}"
  labels = {
    computational_name = "${var.cluster_name}"
    name               = "${local.dataproc_name}"
    sbn                = "${var.project_tag}"
    user               = "${var.user_tag}"
  }

  cluster_config {

    master_config {
      num_instances     = 1
      machine_type      = "${var.master_shape}"
      disk_config {
        boot_disk_size_gb = 30
      }
    }

    worker_config {
      num_instances     = "${var.total_count - 1}"
      machine_type      = "${var.slave_shape}"
      disk_config {
        boot_disk_size_gb = 30
      }
    }

    gce_cluster_config {
      subnetwork = "${var.subnet_name}"
      tags    = ["${var.network_tag}"]
    }

    preemptible_worker_config {
      num_instances = "${var.preemptible_count}"
    }
  }
}