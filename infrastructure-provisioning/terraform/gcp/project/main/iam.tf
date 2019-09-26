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
  edge_policy_name = "${var.service_base_name}-${var.project_tag}-edge-policy"
  edge_role_name   = "${var.service_base_name}-${var.project_tag}-edge_role"
  edge_sa_name     = "${var.service_base_name}-${var.project_tag}-edge-sa"
  nb_policy_name   = "${var.service_base_name}-${var.project_tag}-nb-policy"
  nb_role_name     = "${var.service_base_name}-${var.project_tag}-nb_role"
  nb_sa_name       = "${var.service_base_name}-${var.project_tag}-nb-sa"
}

#################
### Edge node ###
#################

resource "google_service_account" "edge_sa" {
  account_id   = local.edge_sa_name
  display_name = local.edge_sa_name
}
/*
resource "google_project_iam_custom_role" "edge_role" {
  permissions = var.edge_policies
  role_id     = "${replace("${local.edge_role_name}", "-", "_")}"
  title       = local.edge_role_name
}

resource "google_project_iam_member" "edge_iam" {
  # try to set perms as file
  count  = length(var.edge_roles)
  member = "serviceAccount:${google_service_account.edge_sa.email}"
  role   = element(var.edge_roles, count.index)
}

resource "google_project_iam_member" "role_for_edge" {
  member = "serviceAccount:${google_service_account.edge_sa.email}"
  role   = google_project_iam_custom_role.edge_role.id
}
*/
############################################################
### Explotratory environment and computational resources ###
############################################################

resource "google_service_account" "nb_sa" {
  account_id   = local.nb_sa_name
  display_name = local.nb_sa_name
}
/*
resource "google_project_iam_custom_role" "nb_role" {
  permissions = var.nb_policies
  role_id     = "${replace("${local.nb_role_name}", "-", "_")}"
  title       = local.nb_role_name
}

resource "google_project_iam_member" "nb_iam" {
  # try to set perms as file
  count  = length(var.nb_roles)
  member = "serviceAccount:${google_service_account.nb_sa.email}"
  role   = element(var.nb_roles, count.index)
}

resource "google_project_iam_member" "role_for_nb" {
  member = "serviceAccount:${google_service_account.nb_sa.email}"
  role   = google_project_iam_custom_role.nb_role.id
}
*/