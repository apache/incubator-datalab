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
  cluster_name  = "${var.sbn}-de-${var.notebook_name}-${var.cluster_name}"
  notebook_name = "${var.sbn}-nb-${var.notebook_name}"
  nic           = "${var.sbn}-de-${var.notebook_name}-${var.cluster_name}-nic"
}

resource "azurerm_network_interface" "master-nic" {
    name                      = "${local.nic}-m"
    location                  = var.region
    resource_group_name       = var.resource_group
    network_security_group_id = var.nb-sg_id

    ip_configuration {
        name                          = "${local.nic}-m-IPconigurations"
        subnet_id                     = var.subnet_id
        private_ip_address_allocation = "Dynamic"
    }

    tags = {
        Name             = "${local.nic}-m"
        Project_name     = var.project_name
        Project_tag      = var.project_tag
        Endpoint_Tag     = var.endpoint_tag
        Product          = var.product
        SBN              = var.sbn
        User_Tag         = var.user_tag
        Custom_Tag       = var.custom_tag
    }
}

resource "azurerm_virtual_machine" "master" {
    name                  = "${local.cluster_name}-m"
    location              = var.region
    resource_group_name   = var.resource_group
    network_interface_ids = ["${azurerm_network_interface.master-nic.id}"]
    vm_size               = var.master_shape

    storage_os_disk {
        name              = "${local.cluster_name}-m-volume-primary"
        caching           = "ReadWrite"
        create_option     = "FromImage"
        managed_disk_type = "Premium_LRS"
    }

    storage_image_reference {
        id = var.ami
    }

    os_profile {
        computer_name  = "${local.cluster_name}-m"
        admin_username = var.initial_user
    }

    os_profile_linux_config {
        disable_password_authentication = true
        ssh_keys {
            path     = "/home/${var.initial_user}/.ssh/authorized_keys"
            key_data = "${file("${var.ssh_key}")}"
        }
    }

    tags = {
        Name                     = "${local.cluster_name}-m"
        Type                     = "master"
        dataengine_notebook_name = local.notebook_name
        Product                  = var.product
        Project_name             = var.project_name
        Project_tag              = var.project_tag
        User_tag                 = var.user_tag
        Endpoint_Tag             = var.endpoint_tag
        SBN                      = var.sbn
        Custom_Tag               = var.custom_tag
    }
}


resource "azurerm_network_interface" "slave-nic" {
    count                     = var.slave_count
    name                      = "${local.nic}-s-${count.index + 1}"
    location                  = var.region
    resource_group_name       = var.resource_group
    network_security_group_id = var.nb-sg_id

    ip_configuration {
        name                          = "${local.nic}-s-${count.index + 1}-IPconigurations"
        subnet_id                     = var.subnet_id
        private_ip_address_allocation = "Dynamic"
    }

    tags = {
        Name             = "${local.cluster_name}-s-${count.index + 1}"
        Project_name     = var.project_name
        Project_tag      = var.project_tag
        Endpoint_Tag     = var.endpoint_tag
        SBN              = var.sbn
        Product          = var.product
        User_Tag         = var.user_tag
        Custom_Tag       = var.custom_tag
    }
}

resource "azurerm_virtual_machine" "slave" {
    count                 = var.slave_count
    name                  = "${local.cluster_name}-s-${count.index + 1}"
    location              = var.region
    resource_group_name   = var.resource_group
    network_interface_ids = ["${azurerm_network_interface.slave-nic[count.index].id}"]
    vm_size               = var.slave_shape

    storage_os_disk {
        name              = "${local.notebook_name}-s-${count.index + 1}-volume-primary"
        caching           = "ReadWrite"
        create_option     = "FromImage"
        managed_disk_type = "Premium_LRS"
    }

    storage_image_reference {
        id = var.ami
    }

    os_profile {
        computer_name  = "${local.cluster_name}-s-${count.index + 1}"
        admin_username = var.initial_user
    }

    os_profile_linux_config {
        disable_password_authentication = true
        ssh_keys {
            path     = "/home/${var.initial_user}/.ssh/authorized_keys"
            key_data = "${file("${var.ssh_key}")}"
        }
    }

    tags = {
        Name                     = "${local.cluster_name}-s-${count.index + 1}"
        Type                     = "slave"
        dataengine_notebook_name = local.notebook_name
        Product                  = var.product
        Project_name             = var.project_name
        Project_tag              = var.project_tag
        User_tag                 = var.user_tag
        Endpoint_Tag             = var.endpoint_tag
        SBN                      = var.sbn
        Custom_Tag               = var.custom_tag
    }
}
