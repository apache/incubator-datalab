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
 shared_bucket_name = lower("${var.service_base_name}-${var.endpoint_id}-shared-bucket")
}

resource "random_string" "shared_bucket_service_name" {
  length  = 10
  special = false
  lower   = true
  upper   = false
}

resource "azurerm_storage_account" "shared-endpoint-storage-account" {
  name                     = random_string.shared_bucket_service_name.result
  resource_group_name      = data.azurerm_resource_group.data-endpoint-resource-group.name
  location                 = data.azurerm_resource_group.data-endpoint-resource-group.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "BlobStorage"

  tags = {
    Name                              = local.shared_bucket_name
    "${local.additional_tag[0]}"      = local.additional_tag[1]
    "${var.service_base_name}-tag"    = local.shared_bucket_name
    "endpoint_tag"                    = var.endpoint_id
  }
}

resource "azurerm_storage_container" "shared-endpoint-storage-container" {
  name                  = local.shared_bucket_name
  storage_account_name  = azurerm_storage_account.shared-endpoint-storage-account.name
  container_access_type = "private"
}