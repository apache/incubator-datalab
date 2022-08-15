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
  endpoint_subnet_name       = "${var.service_base_name}-${var.endpoint_id}-subnet"
  endpoint_sg_name           = "${var.service_base_name}-${var.endpoint_id}-sg"
  endpoint_vpc_name          = "${var.service_base_name}-${var.endpoint_id}-vpc"
  additional_tag             = split(":", var.additional_tag)
  endpoint_igw_name          = "${var.service_base_name}-${var.endpoint_id}-igw"
  endpoint_ip_name           = "${var.service_base_name}-${var.endpoint_id}-static-ip"
  projects_rt                = "${var.service_base_name}-${var.endpoint_id}-project-rt"
}


resource "aws_vpc" "vpc_create" {
  cidr_block           = var.vpc_cidr
  count                = var.vpc_id == "" ? 1 : 0
  instance_tenancy     = "default"
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags                 = {
    Name                              = local.endpoint_vpc_name
    "${local.additional_tag[0]}"      = local.additional_tag[1]
    "${var.tag_resource_id}"          = "${var.service_base_name}:${local.endpoint_vpc_name}"
    "${var.service_base_name}-tag"    = local.endpoint_vpc_name
  }
}

data "aws_vpc" "data_vpc" {
  id = var.vpc_id == "" ? aws_vpc.vpc_create.0.id : var.vpc_id
}

resource "aws_internet_gateway" "gw" {
  vpc_id = aws_vpc.vpc_create.0.id
  count  = var.vpc_id == "" ? 1 : 0
  tags   = {
    Name                           = local.endpoint_igw_name
    "${local.additional_tag[0]}"   = local.additional_tag[1]
    "${var.tag_resource_id}"       = "${var.service_base_name}:${local.endpoint_igw_name}"
    "${var.service_base_name}-tag" = local.endpoint_igw_name
  }
}

resource "aws_subnet" "endpoint_subnet" {
  vpc_id            = aws_vpc.vpc_create.0.id
  cidr_block        = var.subnet_cidr
  availability_zone = "${var.region}${var.zone}"
  tags              = {
    Name                           = local.endpoint_subnet_name
    "${local.additional_tag[0]}"   = local.additional_tag[1]
    "${var.tag_resource_id}"       = "${var.service_base_name}:${local.endpoint_subnet_name}"
    "${var.service_base_name}-tag" = local.endpoint_subnet_name
  }
  count = var.vpc_id == "" ? 1 : 0
}

data "aws_subnet" "data_subnet" {
  id = var.subnet_id == "" ? aws_subnet.endpoint_subnet.0.id : var.subnet_id
}

resource "aws_route" "route" {
  count                     = var.vpc_id == "" ? 1 : 0
  route_table_id            = aws_vpc.vpc_create.0.main_route_table_id
  destination_cidr_block    = "0.0.0.0/0"
  gateway_id                = aws_internet_gateway.gw.0.id
}

resource "aws_security_group" "endpoint_sec_group" {
  name        = local.endpoint_sg_name
  count       = var.sg_id == "" ? 1 : 0
  vpc_id      = data.aws_vpc.data_vpc.id
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.allowed_ip_cidrs
  }

  ingress {
    from_port   = 8084
    to_port     = 8084
    protocol    = "tcp"
    cidr_blocks = var.allowed_ip_cidrs
  }

  ingress {
    from_port   = 8085
    to_port     = 8085
    protocol    = "tcp"
    cidr_blocks = var.allowed_ip_cidrs
  }

  ingress {
    from_port   = 4822
    to_port     = 4822
    protocol    = "tcp"
    cidr_blocks = var.allowed_ip_cidrs
  }

  ingress {
    from_port   = 8088
    to_port     = 8088
    protocol    = "tcp"
    cidr_blocks = var.allowed_ip_cidrs
  }

    ingress {
    from_port   = 3128
    to_port     = 3128
    protocol    = "tcp"
    cidr_blocks = var.allowed_ip_cidrs
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name                           = local.endpoint_sg_name
    "${local.additional_tag[0]}"   = local.additional_tag[1]
    "${var.tag_resource_id}"       = "${var.service_base_name}:${local.endpoint_sg_name}"
    "${var.service_base_name}-tag" = local.endpoint_sg_name
  }
}

data "aws_security_group" "data_sg" {
  id = var.sg_id == "" ? aws_security_group.endpoint_sec_group[1].id : var.sg_id
}

resource "aws_eip" "endpoint_eip" {
  vpc      = true
  tags = {
    Name                           = local.endpoint_ip_name
    "${local.additional_tag[0]}"   = local.additional_tag[1]
    "${var.tag_resource_id}"       = "${var.service_base_name}:${local.endpoint_ip_name}"
    "${var.service_base_name}-tag" = local.endpoint_ip_name
  }
}

resource "aws_route_table" "projects_route_table" {
  vpc_id = data.aws_vpc.data_vpc.id
  tags   = {
    Name                           = local.projects_rt
    "${local.additional_tag[0]}"   = local.additional_tag[1]
    "${var.tag_resource_id}"       = "${var.service_base_name}:${local.projects_rt}"
    "${var.service_base_name}-tag" = local.projects_rt
    "${var.service_base_name}-tag" = var.service_base_name
  }
}

resource "aws_vpc_endpoint" "s3-endpoint" {
  vpc_id          = data.aws_vpc.data_vpc.id
  service_name    = "com.amazonaws.${var.region}.s3"
  route_table_ids = [aws_route_table.projects_route_table.id]
}
