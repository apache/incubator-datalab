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

//data "aws_eip" "ssn_k8s_lb_eip_a" {
//  id = aws_eip.k8s-lb-eip-a.id
//  depends_on = [aws_lb_listener.ssn_k8s_nlb_listener]
//}
//
//data "aws_eip" "ssn_k8s_lb_eip_a" {
//  id = aws_eip.k8s-lb-eip-b.id                           # Need to be refactored
//  depends_on = [aws_lb_listener.ssn_k8s_nlb_listener]
//}
//
//data "aws_eip" "ssn_k8s_lb_eip_a" {
//  id = aws_eip.k8s-lb-eip-a.id
//  depends_on = [aws_lb_listener.ssn_k8s_nlb_listener]
//}

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
//  ingress {
//    from_port   = 0
//    to_port     = 0         # Need to be refactored
//    protocol    = -1
//    cidr_blocks = ["${data.aws_eip.ssn_k8s_lb_eip.public_ip}/32", "${data.aws_eip.ssn_k8s_lb_eip.private_ip}/32"]
//  }

  egress {
    from_port   = 0
    protocol    = -1
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name                           = local.ssn_sg_name
    "${local.billing_tag[0]}"      = local.billing_tag[1]
    "${var.tag_resource_id}"       = "${var.service_base_name}:${local.ssn_sg_name}"
    "${var.service_base_name}-Tag" = local.ssn_sg_name
  }
}