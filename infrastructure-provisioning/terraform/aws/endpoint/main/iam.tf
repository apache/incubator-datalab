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
  role_name    = "${var.service_base_name}-endpoint-role"
  role_profile = "${var.service_base_name}-endpoint-profile"
  policy_name  = "${var.service_base_name}-endpoint-policy"
}

data "template_file" "endpoint_policy" {
  template = file("../modules/endpoint/files/endpoint-policy.json")
}

resource "aws_iam_role" "endpoint_role" {
  name               = local.role_name
  assume_role_policy = file("../modules/endpoint/files/assume-policy.json")
  tags = {
    product = "${var.product}"
    Name = "${local.role_name}"
    "${var.service_base_name}-Tag" = "${local.role_name}"
  }
}

resource "aws_iam_instance_profile" "endpoint_profile" {
  name = local.role_profile
  role = aws_iam_role.endpoint_role.name
}

resource "aws_iam_policy" "endpoint_policy" {
  name   = local.policy_name
  policy = data.template_file.endpoint_policy.rendered
}

resource "aws_iam_role_policy_attachment" "endpoint_policy_attach" {
  role       = aws_iam_role.endpoint_role.name
  policy_arn = aws_iam_policy.endpoint_policy.arn
}
