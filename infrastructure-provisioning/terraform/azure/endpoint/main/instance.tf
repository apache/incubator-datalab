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
  endpoint_instance_name      = "${var.service_base_name}-${var.endpoint_id}-endpoint"
  endpoint_instance_disk_name = "${var.service_base_name}-${var.endpoint_id}-endpoint-volume"
}

data "tls_public_key" "enpoint_key" {
  private_key_pem = file(var.key_path)
}

resource "azurerm_virtual_machine" "endpoint_instance" {
  name                          = local.endpoint_instance_name
  location                      = data.azurerm_resource_group.data-endpoint-resource-group.location
  resource_group_name           = data.azurerm_resource_group.data-endpoint-resource-group.name
  network_interface_ids         = [azurerm_network_interface.endpoint-nif.id]
  vm_size                       = var.endpoint_shape
  delete_os_disk_on_termination = true

  storage_image_reference {
    publisher = element(split(":", var.ami),0)
    offer     = element(split(":", var.ami),1)
    sku       = element(split(":", var.ami),2)
    version   = "latest"
  }
  storage_os_disk {
    os_type = "Linux"
    name              = local.endpoint_instance_disk_name
    create_option     = "FromImage"
    disk_size_gb      = var.endpoint_volume_size
    managed_disk_type = "Premium_LRS"
  }
  os_profile {
    computer_name  = local.endpoint_instance_name
    admin_username = "ubuntu"
  }
  os_profile_linux_config {
    disable_password_authentication = true
    ssh_keys {
      key_data = data.tls_public_key.enpoint_key.public_key_openssh
      path = "/home/ubuntu/.ssh/authorized_keys"
    }
  }

  tags = {
    Name                              = local.endpoint_instance_name
    "${local.additional_tag[0]}"      = local.additional_tag[1]
    "${var.tag_resource_id}"          = "${var.service_base_name}:${local.endpoint_instance_name}"
    "${var.service_base_name}-tag"    = local.endpoint_instance_name
  }
}