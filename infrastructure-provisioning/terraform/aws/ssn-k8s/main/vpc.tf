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
  additional_tag       = split(":", var.additional_tag)
  ssn_vpc_name      = "${var.service_base_name}-vpc"
  ssn_igw_name      = "${var.service_base_name}-igw"
  ssn_subnet_a_name = "${var.service_base_name}-ssn-subnet-az-a"
  ssn_subnet_b_name = "${var.service_base_name}-ssn-subnet-az-b"
  ssn_subnet_c_name = "${var.service_base_name}-ssn-subnet-az-c"
  endpoint_rt_name  = "${var.service_base_name}-endpoint-rt"
  endpoint_s3_name  = "${var.service_base_name}-endpoint-s3"
}

resource "aws_vpc" "ssn_k8s_vpc" {
  count = var.vpc_id == "" ? 1 : 0
  cidr_block           = var.vpc_cidr
  instance_tenancy     = "default"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name                                          = local.ssn_vpc_name
    "${local.additional_tag[0]}"                  = local.additional_tag[1]
    "${var.tag_resource_id}"                      = "${var.service_base_name}:${local.ssn_vpc_name}"
    "${var.service_base_name}-tag"                = local.ssn_vpc_name
    "kubernetes.io/cluster/${local.cluster_name}" = "owned"
  }
}

resource "aws_internet_gateway" "ssn_k8s_igw" {
  count  = var.vpc_id == "" ? 1 : 0
  vpc_id = aws_vpc.ssn_k8s_vpc.0.id

  tags = {
    Name                                          = local.ssn_igw_name
    "${local.additional_tag[0]}"                  = local.additional_tag[1]
    "${var.tag_resource_id}"                      = "${var.service_base_name}:${local.ssn_igw_name}"
    "${var.service_base_name}-tag"                = local.ssn_igw_name
    "kubernetes.io/cluster/${local.cluster_name}" = "owned"
  }
}

resource "aws_route" "ssn_k8s_route" {
  count                     = var.vpc_id == "" ? 1 : 0
  route_table_id            = aws_vpc.ssn_k8s_vpc.0.main_route_table_id
  destination_cidr_block    = "0.0.0.0/0"
  gateway_id                = aws_internet_gateway.ssn_k8s_igw.0.id
}

data "aws_vpc" "ssn_k8s_vpc_data" {
  id = var.vpc_id == "" ? aws_vpc.ssn_k8s_vpc.0.id : var.vpc_id
}

resource "aws_subnet" "ssn_k8s_subnet_a" {
  count                   = var.subnet_id_a == "" ? 1 : 0
  vpc_id                  = data.aws_vpc.ssn_k8s_vpc_data.id
  availability_zone       = "${var.region}a"
  cidr_block              = var.subnet_cidr_a
  map_public_ip_on_launch = true

  tags = {
    Name                                          = local.ssn_subnet_a_name
    "${local.additional_tag[0]}"                  = local.additional_tag[1]
    "${var.tag_resource_id}"                      = "${var.service_base_name}:${local.ssn_subnet_a_name}"
    "${var.service_base_name}-tag"                = local.ssn_subnet_a_name
    "kubernetes.io/cluster/${local.cluster_name}" = "owned"
  }
}

resource "aws_subnet" "ssn_k8s_subnet_b" {
  count                   = var.subnet_id_b == "" ? 1 : 0
  vpc_id                  = data.aws_vpc.ssn_k8s_vpc_data.id
  availability_zone       = "${var.region}b"
  cidr_block              = var.subnet_cidr_b
  map_public_ip_on_launch = true

  tags = {
    Name                                          = local.ssn_subnet_b_name
    "${local.additional_tag[0]}"                  = local.additional_tag[1]
    "${var.tag_resource_id}"                      = "${var.service_base_name}:${local.ssn_subnet_b_name}"
    "${var.service_base_name}-tag"                = local.ssn_subnet_b_name
    "kubernetes.io/cluster/${local.cluster_name}" = "owned"
  }
}

resource "aws_subnet" "ssn_k8s_subnet_c" {
  count                   = var.ssn_k8s_masters_count > 2 ? 1 : 0
  vpc_id                  = data.aws_vpc.ssn_k8s_vpc_data.id
  availability_zone       = "${var.region}c"
  cidr_block              = var.subnet_cidr_c
  map_public_ip_on_launch = true

  tags = {
    Name                                          = local.ssn_subnet_c_name
    "${local.additional_tag[0]}"                  = local.additional_tag[1]
    "${var.tag_resource_id}"                      = "${var.service_base_name}:${local.ssn_subnet_c_name}"
    "${var.service_base_name}-tag"                = local.ssn_subnet_c_name
    "kubernetes.io/cluster/${local.cluster_name}" = "owned"
  }
}

data "aws_subnet" "k8s-subnet-a-data" {
  id = var.subnet_id_a == "" ? aws_subnet.ssn_k8s_subnet_a.0.id : var.subnet_id_a
}

data "aws_subnet" "k8s-subnet-b-data" {
  id = var.subnet_id_b == "" ? aws_subnet.ssn_k8s_subnet_b.0.id : var.subnet_id_b
}

data "aws_subnet" "k8s-subnet-c-data" {
  count = var.ssn_k8s_masters_count > 2 ? 1 : 0
  id = aws_subnet.ssn_k8s_subnet_c.0.id
}

resource "aws_route_table" "ssn-k8s-users-route-table" {
  vpc_id = data.aws_vpc.ssn_k8s_vpc_data.id
  tags = {
    Name                                          = local.endpoint_rt_name
    "${var.service_base_name}-tag"                = var.service_base_name
    "${local.additional_tag[0]}"                  = local.additional_tag[1]
    "${var.tag_resource_id}"                      = "${var.service_base_name}:${local.endpoint_rt_name}"
    "kubernetes.io/cluster/${local.cluster_name}" = "owned"
  }
}

resource "aws_vpc_endpoint" "ssn-k8s-users-s3-endpoint" {
  vpc_id       = data.aws_vpc.ssn_k8s_vpc_data.id
  service_name = "com.amazonaws.${var.region}.s3"
  tags = {
    Name                                          = local.endpoint_s3_name
    "${local.additional_tag[0]}"                  = local.additional_tag[1]
    "${var.tag_resource_id}"                      = "${var.service_base_name}:${local.endpoint_s3_name}"
    "${var.service_base_name}-tag"                = local.endpoint_s3_name
    "kubernetes.io/cluster/${local.cluster_name}" = "owned"
  }
}

resource "aws_vpc_endpoint_route_table_association" "ssn-k8s-users-s3-route" {
  route_table_id  = aws_route_table.ssn-k8s-users-route-table.id
  vpc_endpoint_id = aws_vpc_endpoint.ssn-k8s-users-s3-endpoint.id
}