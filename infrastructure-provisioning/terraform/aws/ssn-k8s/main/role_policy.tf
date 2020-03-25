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
  ssn_policy_name = "${var.service_base_name}-ssn-policy"
  ssn_role_name   = "${var.service_base_name}-ssn-role"
}

data "template_file" "ssn_k8s_s3_policy" {
  template = file("./files/ssn-policy.json.tpl")
}

resource "aws_iam_policy" "ssn_k8s_policy" {
  name        = local.ssn_policy_name
  description = "Policy for SSN K8S"
  policy      = data.template_file.ssn_k8s_s3_policy.rendered
}

resource "aws_iam_role" "ssn_k8s_role" {
  name               = local.ssn_role_name
  assume_role_policy = file("./files/assume-policy.json")
  tags               = {
    Name                                          = local.ssn_role_name
    "${local.additional_tag[0]}"                  = local.additional_tag[1]
    "${var.tag_resource_id}"                      = "${var.service_base_name}:${local.ssn_role_name}"
    "${var.service_base_name}-tag"                = local.ssn_role_name
    "kubernetes.io/cluster/${local.cluster_name}" = "owned"
  }
}

resource "aws_iam_role_policy_attachment" "ssn_k8s_policy_attachment" {
  role       = aws_iam_role.ssn_k8s_role.name
  policy_arn = aws_iam_policy.ssn_k8s_policy.arn
}

resource "aws_iam_instance_profile" "k8s-profile" {
  name = "${var.service_base_name}-instance-profile"
  role = aws_iam_role.ssn_k8s_role.name
}