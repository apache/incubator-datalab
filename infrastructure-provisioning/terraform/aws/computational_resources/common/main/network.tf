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
  subnet_name = "${var.project_tag}-subnet"
  sg_name     = "${var.project_tag}-nb-sg" #sg - security group
}

resource "aws_subnet" "subnet" {
  vpc_id     = "${var.vpc}"
  cidr_block = "${var.cidr_range}"

  tags = {
    Name    = "${local.subnet_name}"
    Env-Tag = "${local.subnet_name}"
    product = "${var.product}"
  }
}

resource "aws_security_group" "nb-sg" {
  name   = "${local.sg_name}"
  vpc_id = "${var.vpc}"

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
    Name    = "${local.sg_name}"
    product = "${var.product}"
  }
}