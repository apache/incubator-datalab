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
  ssn_nlb_name                 = "${var.service_base_name}-ssn-nlb"
  ssn_alb_name                 = "${var.service_base_name}-ssn-alb"
  ssn_k8s_nlb_api_tg_name      = "${var.service_base_name}-ssn-nlb-api-tg"
  ssn_k8s_nlb_step_ca_tg_name  = "${var.service_base_name}-ssn-nlb-step-ca-tg"
  ssn_k8s_alb_tg_name          = "${var.service_base_name}-ssn-alb-tg"
}

resource "aws_lb" "ssn_k8s_nlb" {
  name               = local.ssn_nlb_name
  load_balancer_type = "network"
  subnets            = compact([data.aws_subnet.k8s-subnet-a-data.id, data.aws_subnet.k8s-subnet-b-data.id,
                                local.subnet_c_id])
  tags               = {
    Name                                          = local.ssn_nlb_name
    "${local.additional_tag[0]}"                  = local.additional_tag[1]
    "${var.tag_resource_id}"                      = "${var.service_base_name}:${local.ssn_nlb_name}"
    "${var.service_base_name}-tag"                = local.ssn_nlb_name
    "kubernetes.io/cluster/${local.cluster_name}" = "owned"
  }
}

resource "aws_lb_target_group" "ssn_k8s_nlb_api_target_group" {
  name     = local.ssn_k8s_nlb_api_tg_name
  port     = 6443
  protocol = "TCP"
  vpc_id   = data.aws_vpc.ssn_k8s_vpc_data.id
  tags     = {
    Name                                          = local.ssn_k8s_nlb_api_tg_name
    "${local.additional_tag[0]}"                  = local.additional_tag[1]
    "${var.tag_resource_id}"                      = "${var.service_base_name}:${local.ssn_k8s_nlb_api_tg_name}"
    "${var.service_base_name}-tag"                = local.ssn_k8s_nlb_api_tg_name
    "kubernetes.io/cluster/${local.cluster_name}" = "owned"
  }
}

resource "aws_lb_target_group" "ssn_k8s_nlb_step_ca_target_group" {
  name     = local.ssn_k8s_nlb_step_ca_tg_name
  port     = 32433
  protocol = "TCP"
  vpc_id   = data.aws_vpc.ssn_k8s_vpc_data.id
  tags     = {
    Name                                          = local.ssn_k8s_nlb_step_ca_tg_name
    "${local.additional_tag[0]}"                  = local.additional_tag[1]
    "${var.tag_resource_id}"                      = "${var.service_base_name}:${local.ssn_k8s_nlb_step_ca_tg_name}"
    "${var.service_base_name}-tag"                = local.ssn_k8s_nlb_step_ca_tg_name
    "kubernetes.io/cluster/${local.cluster_name}" = "owned"
  }
}

resource "aws_lb_listener" "ssn_k8s_nlb_api_listener" {
  load_balancer_arn = aws_lb.ssn_k8s_nlb.arn
  port              = "6443"
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.ssn_k8s_nlb_api_target_group.arn
  }
}

resource "aws_lb_listener" "ssn_k8s_nlb_step_ca_listener" {
  load_balancer_arn = aws_lb.ssn_k8s_nlb.arn
  port              = "443"
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.ssn_k8s_nlb_step_ca_target_group.arn
  }
}