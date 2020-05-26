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
    edge_sg_name   = "${var.service_base_name}-${var.project_tag}-edge-sg"
    edge_ip_name   = "${var.service_base_name}-${var.project_tag}-edge-ip"
    ps_subnet_name = "${var.service_base_name}-${var.project_tag}-ps-subnet"
    ps_sg_name     = "${var.service_base_name}-${var.project_tag}-ps-sg"
}

#################
### Edge node ###
#################

resource "azurerm_public_ip" "edge_ip" {
    location = var.region
    name = local.edge_ip_name
    resource_group_name = var.resource_group
    allocation_method = "Static"
    tags = {
        SBN              = var.service_base_name
        Name             = local.edge_ip_name
        Project_tag      = var.project_tag
        Endpoint_Tag     = var.endpoint_tag
        Product          = var.product
        User_Tag         = var.user_tag
        Custom_Tag       = var.custom_tag
    }
}

resource "azurerm_network_security_group" "edge_sg" {
    name = local.edge_sg_name
    location = var.region
    resource_group_name = var.resource_group

    security_rule {
        name                       = "in-1"
        priority                   = 100
        direction                  = "Inbound"
        access                     = "Allow"
        protocol                   = "*"
        source_port_range          = "*"
        destination_port_range     = "*"
        source_address_prefix      = "${var.ps_cidr}"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "in-2"
        priority                   = 110
        direction                  = "Inbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "22"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "in-3"
        priority                   = 120
        direction                  = "Inbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "3128"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "in-4"
        priority                   = 130
        direction                  = "Inbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "80"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "out-1"
        priority                   = 100
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "22"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-2"
        priority                   = 110
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "8888"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-3"
        priority                   = 120
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "8080"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-4"
        priority                   = 130
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "8787"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-5"
        priority                   = 140
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "6006"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-6"
        priority                   = 150
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "20888"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-7"
        priority                   = 160
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "8088"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-8"
        priority                   = 170
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "18080"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-9"
        priority                   = 180
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "50070"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-10"
        priority                   = 190
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "8085"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-11"
        priority                   = 200
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "8081"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-12"
        priority                   = 210
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "4040-4140"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "out-13"
        priority                   = 220
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "UDP"
        source_port_range          = "*"
        destination_port_range     = "53"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "out-14"
        priority                   = 230
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "80"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "out-15"
        priority                   = 240
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "443"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "out-16"
        priority                   = 250
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "TCP"
        source_port_range          = "*"
        destination_port_range     = "389"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "out-17"
        priority                   = 260
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "*"
        source_port_range          = "*"
        destination_port_range     = "8042"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-18"
        priority                   = 270
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "UDP"
        source_port_range          = "*"
        destination_port_range     = "123"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "out-19"
        priority                   = 280
        direction                  = "Outbound"
        access                     = "Deny"
        protocol                   = "*"
        source_port_range          = "*"
        destination_port_range     = "*"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }
}

############################################################
### Explotratory environment and computational resources ###
############################################################


resource "azurerm_subnet" "ps_subnet" {
    name                 = local.ps_subnet_name
    resource_group_name  = var.resource_group
    virtual_network_name = var.vpc_id
    address_prefix       = var.ps_cidr
}

resource "azurerm_network_security_group" "ps_sg" {
    name                = local.ps_sg_name
    location            = var.region
    resource_group_name = var.resource_group

    security_rule {
        name                       = "in-1"
        priority                   = 100
        direction                  = "Inbound"
        access                     = "Allow"
        protocol                   = "*"
        source_port_range          = "*"
        destination_port_range     = "*"
        source_address_prefix      = "${var.ps_cidr}"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "in-2"
        priority                   = 110
        direction                  = "Inbound"
        access                     = "Allow"
        protocol                   = "*"
        source_port_range          = "*"
        destination_port_range     = "*"
        source_address_prefix      = "${var.edge_cidr}"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "in-3"
        priority                   = 200
        direction                  = "Inbound"
        access                     = "Deny"
        protocol                   = "*"
        source_port_range          = "*"
        destination_port_range     = "*"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "out-1"
        priority                   = 100
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "*"
        source_port_range          = "*"
        destination_port_range     = "*"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.ps_cidr}"
    }

    security_rule {
        name                       = "out-2"
        priority                   = 110
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "*"
        source_port_range          = "*"
        destination_port_range     = "*"
        source_address_prefix      = "*"
        destination_address_prefix = "${var.edge_cidr}"
    }

    security_rule {
        name                       = "out-3"
        priority                   = 120
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "*"
        source_port_range          = "443"
        destination_port_range     = "*"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "out-4"
        priority                   = 200
        direction                  = "Outbound"
        access                     = "Deny"
        protocol                   = "*"
        source_port_range          = "*"
        destination_port_range     = "*"
        source_address_prefix      = "*"
        destination_address_prefix = "*"
    }

    tags = {
        Name             = local.ps_subnet_name
        SBN              = var.service_base_name
        Product          = var.product
        Project_name     = var.project_name
        Project_tag      = var.project_tag
        Endpoint_tag     = var.endpoint_tag
        User_tag         = var.user_tag
        Custom_tag       = var.custom_tag
    }
}