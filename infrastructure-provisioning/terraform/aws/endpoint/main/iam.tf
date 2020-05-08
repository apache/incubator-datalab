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
  endpoint_role_name    = "${var.service_base_name}-${var.endpoint_id}-role"
  endpoint_role_profile = "${var.service_base_name}-${var.endpoint_id}-profile"
  endpoint_policy_name  = "${var.service_base_name}-${var.endpoint_id}-policy"
}

data "template_file" "endpoint_policy" {
  template = file("./files/endpoint-policy.json")
}

resource "aws_iam_role" "endpoint_role" {
  name               = local.endpoint_role_name
  assume_role_policy = file("./files/assume-policy.json")
  tags = {
    Name = local.endpoint_role_name
    "${local.additional_tag[0]}" = local.additional_tag[1]
    "${var.tag_resource_id}" = "${var.service_base_name}:${local.endpoint_role_name}"
    "${var.service_base_name}-tag" = local.endpoint_role_name
  }
}

resource "aws_iam_instance_profile" "endpoint_profile" {
  name = local.endpoint_role_profile
  role = aws_iam_role.endpoint_role.name
}

resource "aws_iam_policy" "endpoint_policy" {
  name   = local.endpoint_policy_name
  policy = data.template_file.endpoint_policy.rendered
}

resource "aws_iam_role_policy_attachment" "endpoint_policy_attach" {
  role       = aws_iam_role.endpoint_role.name
  policy_arn = aws_iam_policy.endpoint_policy.arn
}
