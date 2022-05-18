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
  service_name = "${var.project_tag}-ps-sa"
  role_name = "${var.project_tag}-ps-role"
}

resource "google_service_account" "ps_sa" {
  #Create service account for notebooks and computational resources
  account_id = "${var.project_tag}-ps-sa"
  display_name = "${var.project_tag}-ps-sa"
}

resource "google_service_account_key" "ps_sa_key" {
  #Create service account key
  depends_on         = ["google_project_iam_member.iam"]
  service_account_id = google_service_account.ps_sa.name
}

resource "google_project_iam_custom_role" "ps-custom-role" {
  #Create custom role for ps_sa
  role_id     = "${replace("${var.project_tag}-ps-role", "-", "_")}"
  title       = "${var.project_tag}-ps-role"
  permissions = "${var.ps_policy}"
}

resource "google_project_iam_member" "role_for_member" {
  #Grant the custom role for the ps_sa
  project = var.gcp_project_id
  member = "serviceAccount:${google_service_account.ps_sa.email}"
  role   = "${google_project_iam_custom_role.ps-custom-role.id}"
}

resource "google_project_iam_member" "iam" {
  #Grant other roles for the ps_sa
  count  = "${length(var.ps_roles)}"
  project = var.gcp_project_id
  member = "serviceAccount:${google_service_account.ps_sa.email}"
  role   = "${element(var.ps_roles, count.index)}"
}