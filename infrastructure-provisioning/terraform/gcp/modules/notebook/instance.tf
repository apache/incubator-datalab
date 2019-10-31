locals {
  name = "${var.project_tag}-nb-${var.notebook_name}"
}

resource "google_compute_disk" "secondary" {
  name = "${local.name}-secondary"
  zone = "${var.zone_var}"
  labels = {
    name    = "${local.name}"
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