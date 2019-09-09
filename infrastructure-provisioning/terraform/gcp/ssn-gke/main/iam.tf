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
  service_account_name = "${var.service_base_name}-sa"
  role_name            = "${var.service_base_name}-role"
}

resource "google_service_account" "ssn_k8s_sa" {
  account_id   = local.service_account_name
  display_name = local.service_account_name
}

resource "google_project_iam_member" "log_writer" {
  project = var.project_id
  # role    = "projects/${var.project_id}/roles/${local.role_name}"
  role = "roles/logging.logWriter"
  member  = "serviceAccount:${google_service_account.ssn_k8s_sa.email}"
}

resource "google_project_iam_member" "metric_writer" {
  project = var.project_id
  # role    = "projects/${var.project_id}/roles/${local.role_name}"
  role = "roles/monitoring.metricWriter"
  member  = "serviceAccount:${google_service_account.ssn_k8s_sa.email}"
}

resource "google_project_iam_member" "monitoring_viewer" {
  project = var.project_id
  # role    = "projects/${var.project_id}/roles/${local.role_name}"
  role = "monitoring.viewer"
  member  = "serviceAccount:${google_service_account.ssn_k8s_sa.email}"
}

//resource "google_project_iam_custom_role" "ssn_k8s_role" {
//  role_id     = local.role_name
//  title       = local.role_name
//  description = "Role for GKE cluser - ${local.gke_name}"
//  permissions = ["iam.roles.list", "iam.roles.create", "iam.roles.delete"]
//}