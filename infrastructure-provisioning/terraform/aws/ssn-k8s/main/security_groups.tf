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
  ssn_sg_name = "${var.service_base_name}-ssn-sg"
}

resource "aws_security_group" "ssn_k8s_sg" {
  name        = local.ssn_sg_name
  description = "SG for SSN K8S cluster"
  vpc_id      = data.aws_vpc.ssn_k8s_vpc_data.id

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    cidr_blocks = [data.aws_vpc.ssn_k8s_vpc_data.cidr_block]
  }
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.allowed_cidrs
  }
  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    cidr_blocks = ["0.0.0.0/0"]
    description = "Need to be changed in the future"
  }

  egress {
    from_port   = 0
    protocol    = -1
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name                                          = local.ssn_sg_name
    "${local.additional_tag[0]}"                  = local.additional_tag[1]
    "${var.tag_resource_id}"                      = "${var.service_base_name}:${local.ssn_sg_name}"
    "${var.service_base_name}-tag"                = local.ssn_sg_name
    "kubernetes.io/cluster/${local.cluster_name}" = "owned"
  }
}