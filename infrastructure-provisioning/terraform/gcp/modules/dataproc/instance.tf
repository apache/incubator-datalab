locals {
  dataproc_name = "${var.project_tag}-des-${var.notebook_name}-${var.cluster_name}"
}

resource "google_dataproc_cluster" "dataproc" {
    name       = "${local.dataproc_name}"
    region     = "${var.region}"
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