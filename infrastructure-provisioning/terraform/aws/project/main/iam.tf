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
  edge_role_name    = "${var.service_base_name}-edge-role"
  edge_role_profile = "${var.service_base_name}-edge-profile"
  edge_policy_name  = "${var.service_base_name}-edge-policy"
  nb_role_name      = "${var.service_base_name}-nb-de-Role"
  nb_role_profile   = "${var.service_base_name}-nb-Profile"
  nb_policy_name    = "${var.service_base_name}-strict_to_S3-Policy"
}

data "template_file" "edge_policy" {
  template = file("./files/edge-policy.json")
}

data "template_file" "nb_policy" {
  template = file("./files/nb-policy.json")
  vars = {
    sbn = var.service_base_name
  }
}

#################
### Edge node ###
#################

resource "aws_iam_role" "edge_role" {
  name               = local.edge_role_name
  assume_role_policy = file("./files/edge-assume-policy.json")
  tags = {
    Name = "${local.edge_role_name}"
    "${local.additional_tag[0]}" = local.additional_tag[1]
    "${var.tag_resource_id}" = "${var.service_base_name}:${local.edge_role_name}"
    "${var.service_base_name}-tag" = local.edge_role_name
  }
}

resource "aws_iam_instance_profile" "edge_profile" {
  name = local.edge_role_profile
  role = aws_iam_role.edge_role.name
}

resource "aws_iam_policy" "edge_policy" {
  name   = local.edge_policy_name
  policy = data.template_file.edge_policy.rendered
}

resource "aws_iam_role_policy_attachment" "edge_policy_attach" {
  role       = aws_iam_role.edge_role.name
  policy_arn = aws_iam_policy.edge_policy.arn
}

############################################################
### Explotratory environment and computational resources ###
############################################################

resource "aws_iam_role" "nb_de_role" {
  name               = local.nb_role_name
  assume_role_policy = file("./files/nb-assume-policy.json")

  tags = {
    Name                           = local.nb_role_name
    Environment_tag                = var.service_base_name
    "${var.service_base_name}-tag" = local.nb_role_name
    "${local.additional_tag[0]}"   = local.additional_tag[1]
    Project_name                   = var.project_name
    Project_tag                    = var.project_tag
    Endpoint_tag                   = var.endpoint_tag
    "user:tag"                     = "${var.service_base_name}:${local.nb_role_name}"
    User_tag                       = var.user_tag
    Custom_tag                     = var.custom_tag
  }
}

resource "aws_iam_instance_profile" "nb_profile" {
  name = local.nb_role_profile
  role = aws_iam_role.nb_de_role.name
}

resource "aws_iam_policy" "nb_policy" {
  name = local.nb_policy_name
  description = "Strict Bucket only policy"
  policy = data.template_file.nb_policy.rendered
}

resource "aws_iam_role_policy_attachment" "nb_policy-attach" {
  role       = aws_iam_role.nb_de_role.name
  policy_arn = aws_iam_policy.nb_policy.arn
}