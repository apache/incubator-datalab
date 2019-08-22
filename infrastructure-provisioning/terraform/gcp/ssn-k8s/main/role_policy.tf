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
  ssn_policy_name      = "${var.service_base_name}-ssn-policy"
  ssn_role_name        = "${var.service_base_name}-ssn-role"
  service_account_name = "${var.service_base_name}-storage-sa"
}

resource "google_service_account" "ssn_k8s_sa" {
  account_id   = local.service_account_name
  display_name = local.service_account_name
}

resource "google_project_iam_custom_role" "ssn_k8s_role" {
  permissions = var.ssn_policies
  role_id     = local.ssn_role_name
  title       = local.ssn_role_name
}

resource "google_project_iam_member" "role_for_member" {
  #Grant the custom role for the ps_sa
  member = "serviceAccount:${google_service_account.ssn_k8s_sa.email}"
  role   = "${google_project_iam_custom_role.ssn_k8s_role.id}"
}

resource "google_project_iam_member" "iam" {
  #Grant other roles for the ps_sa
  count  = "${length(var.ssn_roles)}"
  member = "serviceAccount:${google_service_account.ssn_k8s_sa.email}"
  role   = "${element(var.ssn_roles, count.index)}"
}