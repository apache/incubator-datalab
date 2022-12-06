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
  endpoint_policy_name      = "${var.service_base_name}-${var.endpoint_id}-policy"
  endpoint_role_name        = "${var.service_base_name}-${var.endpoint_id}-role"
  service_account_name      = "${var.service_base_name}-${var.endpoint_id}-sa"
}

resource "google_service_account" "endpoint_sa" {
  account_id   = local.service_account_name
  display_name = local.service_account_name
}

resource "google_project_iam_custom_role" "endpoint_role" {
  permissions = var.endpoint_policies
  role_id     = replace(local.endpoint_role_name, "-", "_")
  title       = local.endpoint_role_name
}

resource "google_project_iam_member" "iam" {
  # try to set perms as file
  project = var.gcp_project_id
  count  = length(var.endpoint_roles)
  member = "serviceAccount:${google_service_account.endpoint_sa.email}"
  role   = element(var.endpoint_roles, count.index)
}

resource "google_project_iam_member" "role_for_member" {
  project = var.gcp_project_id
  member = "serviceAccount:${google_service_account.endpoint_sa.email}"
  role   = google_project_iam_custom_role.endpoint_role.id
}
