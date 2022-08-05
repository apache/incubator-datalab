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
   endpoint_sg_name = "${var.service_base_name}-${var.endpoint_id}-sg"
}

resource "azurerm_network_security_group" "enpoint-sg" {
  location            = data.azurerm_resource_group.data-endpoint-resource-group.location
  resource_group_name = data.azurerm_resource_group.data-endpoint-resource-group.name
  name                = local.endpoint_sg_name

  security_rule {
    name                       = "inbound-1"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "22"
    destination_port_range     = "22"
    source_address_prefix      = var.allowed_ip_cidrs
  }

  security_rule {
    name                       = "inbound-2"
    priority                   = 200
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "8084"
    destination_port_range     = "8084"
    source_address_prefix      = var.allowed_ip_cidrs
  }

  security_rule {
    name                       = "inbound-3"
    priority                   = 300
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "8088"
    destination_port_range     = "8088"
    source_address_prefix      = var.allowed_ip_cidrs
  }

  security_rule {
    name                       = "outbound-1"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "*"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
}
