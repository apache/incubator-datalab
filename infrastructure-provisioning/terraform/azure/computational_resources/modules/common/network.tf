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
  subnet_name = "${var.sbn}-subnet"
  sg_name     = "${var.sbn}-nb-sg" #sg - security group
}

resource "azurerm_subnet" "subnet" {
    name                 = local.subnet_name
    resource_group_name  = var.sbn
    virtual_network_name = var.vpc
    address_prefix       = var.cidr_range
}

resource "azurerm_network_security_group" "nb-sg" {
    name                = local.sg_name
    location            = var.region
    resource_group_name = var.sbn

    security_rule {
        name                       = "in-1"
        priority                   = 100
        direction                  = "Inbound"
        access                     = "Allow"
        protocol                   = "*"
        source_port_range          = "*"
        destination_port_range     = "*"
        source_address_prefix      = "${var.cidr_range}"
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
        source_address_prefix      = "${var.traefik_cidr}"
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
        source_address_prefix      = "${var.cidr_range}"
        destination_address_prefix = "*"
    }

    security_rule {
        name                       = "out-2"
        priority                   = 110
        direction                  = "Outbound"
        access                     = "Allow"
        protocol                   = "*"
        source_port_range          = "*"
        destination_port_range     = "*"
        source_address_prefix      = "${var.traefik_cidr}"
        destination_address_prefix = "*"
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

    tags = {
        Name             = local.subnet_name
        SBN              = var.sbn
        Product          = var.product
        Project_name     = var.project_name
        Project_tag      = var.project_tag
        Endpoint_tag     = var.endpoint_tag
        User_tag         = var.user_tag
        Custom_tag       = var.custom_tag
    }
}

resource "aws_subnet" "subnet" {
  vpc_id     = var.vpc
  cidr_block = var.cidr_range

  tags = {
    Name             = local.subnet_name
    SBN              = var.sbn
    Product          = var.product
    Project_name     = var.project_name
    Project_tag      = var.project_tag
    Endpoint_tag     = var.endpoint_tag
    User_tag         = var.user_tag
    Custom_tag       = var.custom_tag
  }
}

resource "aws_security_group" "nb-sg" {
  name   = local.sg_name
  vpc_id = var.vpc

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["${var.cidr_range}", "${var.traefik_cidr}"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 443
    to_port     = 443
    protocol    = "TCP"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name             = local.sg_name
    SBN              = var.sbn
    Product          = var.product
    Project_name     = var.project_name
    Project_tag      = var.project_tag
    Endpoint_tag     = var.endpoint_tag
    User_tag         = var.user_tag
    Custom_tag       = var.custom_tag
  }
}