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

output "endpoint_eip_address" {
  value = azurerm_public_ip.endpoint-static-ip.ip_address
}

output "subnet_id" {
  value = data.azurerm_subnet.data-endpoint-subnet.name
}

output "vpc_id" {
  value = data.azurerm_virtual_network.data-endpoint-network.name
}

output "ssn_k8s_sg_id" {
  value = azurerm_network_security_group.enpoint-sg.name
}

output "endpoint_id" {
  value = var.endpoint_id
}

output "resource_group_name" {
  value = data.azurerm_resource_group.data-endpoint-resource-group.name
}