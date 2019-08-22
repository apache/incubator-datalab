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
  ssn_nlb_name            = "${var.service_base_name}-ssn-nlb"
  ssn_alb_name            = "${var.service_base_name}-ssn-alb"
  ssn_k8s_nlb_api_tg_name = "${var.service_base_name}-ssn-nlb-api-tg"
  ssn_k8s_nlb_ss_tg_name  = "${var.service_base_name}-ssn-nlb-ss-tg"
  ssn_k8s_alb_tg_name     = "${var.service_base_name}-ssn-alb-tg"
}


resource "google_compute_forwarding_rule" "ssn_k8s_nlb" {
  name   = local.ssn_nlb_name
  backend_service = google_compute_backend_service
  target = google_compute_target_pool.ssn_target_pool.self_link
  ports = ["8443", "6443"]
  load_balancing_scheme = "INTERNAL"
  network = google_compute_network.ssn_k8s_vpc.name
  subnetwork = compact([data.google_compute_subnetwork.k8s-subnet-a-data.name, data.google_compute_subnetwork.k8s-subnet-b-data.name, local.subnet_c_id])
}

resource "google_compute_backend_service" "nlb_service" {
  health_checks = [google_compute_health_check.ssn_health_check.self_link]
  name = "nlb_backend"
}

resource "google_compute_health_check" "ssn_health_check" {
  name  = "${var.service_base_name}-hc"
  check_interval_sec = 1
  timeout_sec        = 1

  tcp_health_check {
    port = "6443"
  }
}

######################################################################

resource "google_compute_global_address" "default" {
  name         = "${local.ssn_alb_name}-ip"
  ip_version   = "IPV4"
  address_type = "EXTERNAL"
}

resource "google_compute_global_forwarding_rule" "ssn_k8s_alb" {
  name       = "global-rule"
  target     = google_compute_target_http_proxy.ssn_target_http.self_link
  port_range = "80"
  depends_on = [google_compute_global_address.default]
}


resource "google_compute_target_http_proxy" "ssn_target_http" {
  name = "target_proxy"
  url_map = google_compute_url_map.url_map.self_link
}

resource "google_compute_url_map" "url_map" {
  default_service = google_compute_backend_service.ssn_http_back.self_link
  name = "url-map"
}

resource "google_compute_backend_service" "ssn_http_back" {
  name        = "backend"
  port_name   = "http"
  protocol    = "HTTP"
  timeout_sec = 10

  health_checks = [google_compute_http_health_check.ssn_http_hc.self_link]
}

resource "google_compute_http_health_check" "ssn_http_hc" {
  name               = "check-backend"
  request_path       = "/"
  check_interval_sec = 1
  timeout_sec        = 1
}

resource "google_compute_global_forwarding_rule" "ssn_k8s_alb" {
  name       = local.ssn_alb_name
  target     = google_compute_target_http_proxy.http[0].self_link
  ip_address = google_compute_global_address.default.address
  port_range = "80"


}

