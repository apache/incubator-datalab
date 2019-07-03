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

data "template_file" "ssn_k8s_s3_policy" {
  template = file("../modules/ssn-k8s/files/ssn-policy.json.tpl")
  vars = {
    bucket_arn = aws_s3_bucket.ssn_k8s_bucket.arn
  }
}

resource "aws_iam_policy" "ssn_k8s_policy" {
  name        = "${var.service_base_name}-ssn-policy"
  description = "Policy for SSN K8S"
  policy      = data.template_file.ssn_k8s_s3_policy.rendered
}

resource "aws_iam_role" "ssn_k8s_role" {
  name               = "${var.service_base_name}-ssn-role"
  assume_role_policy = file("../modules/ssn-k8s/files/assume-policy.json")
  tags = {
    Name = "${var.service_base_name}-ssn-role"
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