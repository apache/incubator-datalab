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
  edge_sg_name     = "${var.service_base_name}-${var.project_name}-edge-sg"
  edge_ip_name     = "${var.service_base_name}-${var.project_name}-edge-EIP"
  additional_tag   = split(":", var.additional_tag)
  nb_subnet_name   = "${var.service_base_name}-${var.project_name}-nb-subnet"
  sg_name          = "${var.service_base_name}-${var.project_name}-nb-sg" #sg - security group
  sbn              = var.service_base_name
}

#################
### Edge node ###
#################

resource "aws_eip" "edge_ip" {
  vpc  = true
  tags = {
    Name                           = local.edge_ip_name
    "${local.additional_tag[0]}"   = local.additional_tag[1]
    "${var.tag_resource_id}"       = "${var.service_base_name}:${local.edge_ip_name}"
    "${var.service_base_name}-tag" = local.edge_ip_name
  }
}

resource "aws_security_group" "edge_sg" {
  name        = local.edge_sg_name
  vpc_id      = var.vpc_id

  ingress {
    from_port = 0
    protocol = "-1"
    to_port = 0
    cidr_blocks = [var.nb_cidr, var.edge_cidr]
  }

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 3128
    to_port     = 3128
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 8080
    protocol = "tcp"
    to_port = 8080
    cidr_blocks = [var.nb_cidr]
  }

  egress {
    from_port = 6006
    protocol = "tcp"
    to_port = 6006
    cidr_blocks = [var.nb_cidr]
  }

  egress {
    from_port = 8085
    protocol = "tcp"
    to_port = 8085
    cidr_blocks = [var.nb_cidr]
  }

  egress {
    from_port = 18080
    protocol = "tcp"
    to_port = 18080
    cidr_blocks = [var.nb_cidr]
  }

  egress {
    from_port = 8088
    protocol = "tcp"
    to_port = 8088
    cidr_blocks = [var.nb_cidr]
  }

  egress {
    from_port = 4040
    protocol = "tcp"
    to_port = 4140
    cidr_blocks = [var.nb_cidr]
  }

  egress {
    from_port = 50070
    protocol = "tcp"
    to_port = 50070
    cidr_blocks = [var.nb_cidr]
  }

  egress {
    from_port = 8888
    protocol = "tcp"
    to_port = 8888
    cidr_blocks = [var.nb_cidr]
  }

  egress {
    from_port = 8042
    protocol = "tcp"
    to_port = 8042
    cidr_blocks = [var.nb_cidr]
  }

  egress {
    from_port = 20888
    protocol = "tcp"
    to_port = 20888
    cidr_blocks = [var.nb_cidr]
  }

  egress {
    from_port = 8787
    protocol = "tcp"
    to_port = 8787
    cidr_blocks = [var.nb_cidr]
  }

  egress {
    from_port = 8081
    protocol = "tcp"
    to_port = 8081
    cidr_blocks = [var.nb_cidr]
  }

  egress {
    from_port   = 53
    to_port     = 53
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 389
    to_port     = 389
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 123
    to_port     = 123
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name                           = local.edge_sg_name
    "${local.additional_tag[0]}"   = local.additional_tag[1]
    "${var.tag_resource_id}"       = "${var.service_base_name}:${local.edge_sg_name}"
    "${var.service_base_name}-tag" = local.edge_sg_name
  }
}

############################################################
### Explotratory environment and computational resources ###
############################################################

resource "aws_subnet" "private_subnet" {
  vpc_id     = var.vpc_id
  cidr_block = var.nb_cidr

  tags = {
    Name                         = local.nb_subnet_name
    "${local.sbn}-tag"           = local.nb_subnet_name
    "${local.additional_tag[0]}" = local.additional_tag[1]
    Project_name                 = var.project_name
    Project_tag                  = var.project_tag
    Endpoint_tag                 = var.endpoint_tag
    "user:tag"                   = "${local.sbn}:${local.nb_subnet_name}"
    User_tag                     = var.user_tag
    Custom_tag                   = var.custom_tag
  }
}

resource "aws_security_group" "nb-sg" {
  name   = local.sg_name
  vpc_id = var.vpc_id

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [var.nb_cidr, var.edge_cidr]
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
    Name                         = local.sg_name
    "${local.sbn}-tag"           = local.sg_name
    "${local.additional_tag[0]}" = local.additional_tag[1]
    Project_name                 = var.project_name
    Project_tag                  = var.project_tag
    Endpoint_tag                 = var.endpoint_tag
    "user:tag"                   = "${local.sbn}:${local.sg_name}"
    User_tag                     = var.user_tag
    Custom_tag                   = var.custom_tag
  }
}