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

resource "aws_vpc" "ssn_k8s_vpc" {
  count = var.vpc_id == "" ? 1 : 0
  cidr_block           = var.vpc_cidr
  instance_tenancy     = "default"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${var.service_base_name}-ssn-vpc"
  }
}

resource "aws_internet_gateway" "ssn_k8s_igw" {
  count  = var.vpc_id == "" ? 1 : 0
  vpc_id = aws_vpc.ssn_k8s_vpc.0.id

  tags = {
    Name = "${var.service_base_name}-ssn-igw"
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

resource "aws_subnet" "ssn_k8s_subnet" {
  count                   = var.subnet_id == "" ? 1 : 0
  vpc_id                  = data.aws_vpc.ssn_k8s_vpc_data.id
  availability_zone       = "${var.region}${var.zone}"
  cidr_block              = var.subnet_cidr
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.service_base_name}-ssn-subnet"
  }
}

data "aws_subnet" "k8s-subnet-data" {
  id = var.subnet_id == "" ? aws_subnet.ssn_k8s_subnet.0.id : var.subnet_id
}

resource "aws_eip" "k8s-lb-eip" {
  vpc      = true
  tags = {
    Name = "${var.service_base_name}-ssn-eip"
  }
}