resource "google_compute_subnetwork" "subnet" {
  name          = "${var.project_tag}-subnet"
  ip_cidr_range = "${var.cidr_range}"
  region        = "${var.region}"
  network       = "${var.vpc_name}"
}

resource "google_compute_firewall" "fw_ingress" {
  name    = "${var.fw_ingress}"
  network = "${var.vpc_name}"
  allow {
    protocol = "all"
  }
  target_tags   = ["${var.network_tag}"]
  source_ranges = ["${var.cidr_range}", "${var.traefik_cidr}"]
}

resource "google_compute_firewall" "fw_egress_public" {
  name      = "${var.fw_egress_public}"
  network   = "${var.vpc_name}"
  direction = "EGRESS"
  allow {
    protocol = "tcp"
    ports    = ["443"]
  }
  target_tags        = ["${var.network_tag}"]
  destination_ranges = ["0.0.0.0/0"]
}

resource "google_compute_firewall" "fw_egress_private" {
  name      = "${var.fw_egress_private}"
  network   = "${var.vpc_name}"
  direction = "EGRESS"
  allow {
    protocol = "all"
  }
  target_tags        = ["${var.network_tag}"]
  destination_ranges = ["${var.cidr_range}", "${var.traefik_cidr}"]
}