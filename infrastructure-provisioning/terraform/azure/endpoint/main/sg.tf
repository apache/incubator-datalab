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

resource "azure_security_group" "enpoint-sg" {
  name     = local.endpoint_sg_name
  location = var.region
}

resource "azure_security_group_rule" "inbound-1" {
  name                       = "inbound-1"
  security_group_names       = [azure_security_group.enpoint-sg.name]
  type                       = "Inbound"
  action                     = "Allow"
  priority                   = 100
  source_address_prefix      = "*"
  source_port_range          = "*"
  destination_address_prefix = "*"
  destination_port_range     = "22"
  protocol                   = "TCP"
}

resource "azure_security_group_rule" "inbound-2" {
  name                       = "inbound-2"
  security_group_names       = [azure_security_group.enpoint-sg.name]
  type                       = "Inbound"
  action                     = "Allow"
  priority                   = 200
  source_address_prefix      = "*"
  source_port_range          = "*"
  destination_address_prefix = "*"
  destination_port_range     = "8084"
  protocol                   = "TCP"
}

resource "azure_security_group_rule" "outbound-1" {
  name                       = "outbound-1"
  security_group_names       = [azure_security_group.enpoint-sg.name]
  type                       = "Outbound"
  action                     = "Allow"
  priority                   = 100
  source_address_prefix      = "*"
  source_port_range          = "*"
  destination_address_prefix = "*"
  destination_port_range     = "*"
  protocol                   = "*"
}
