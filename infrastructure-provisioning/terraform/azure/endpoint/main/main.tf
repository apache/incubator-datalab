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
  resource_group_name = "${var.service_base_name}-${var.endpoint_id}-resource-group"
  json_data           = jsondecode(file(var.auth_file_path))
}

provider "azurerm" {
  features {}
  subscription_id = local.json_data.subscriptionId
  client_id       = local.json_data.clientId
  client_secret   = local.json_data.clientSecret
  tenant_id       = local.json_data.tenantId
}

resource "azurerm_resource_group" "endpoint-resource-group" {
  count    = var.resource_group_name == "" ? 1 : 0
  name     = local.resource_group_name
  location = var.region

  tags = {
    Name = var.service_base_name
  }
}

data "azurerm_resource_group" "data-endpoint-resource-group" {
  name = var.resource_group_name == "" ? azurerm_resource_group.endpoint-resource-group.0.name : var.resource_group_name
}
