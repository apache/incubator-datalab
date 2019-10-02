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
  ssn_s3_name = "${var.service_base_name}-ssn-bucket"
  ssn_s3_shared_name = "${var.service_base_name}-shared-bucket"
}

resource "aws_s3_bucket" "ssn_k8s_bucket" {
  bucket = local.ssn_s3_name
  acl    = "private"
  tags   = {
    Name                           = local.ssn_s3_name
    "${local.additional_tag[0]}"      = local.additional_tag[1]
    "${var.tag_resource_id}"       = "${var.service_base_name}:${local.ssn_s3_name}"
    "${var.service_base_name}-Tag" = local.ssn_s3_name
  }
  force_destroy = true
}

resource "aws_s3_bucket" "ssn_k8s_shared_bucket" {
  bucket = local.ssn_s3_shared_name
  acl    = "private"
  tags   = {
    Name                           = local.ssn_s3_shared_name
    "${local.additional_tag[0]}"      = local.additional_tag[1]
    "${var.tag_resource_id}"       = "${var.service_base_name}:${local.ssn_s3_shared_name}"
    "${var.service_base_name}-Tag" = local.ssn_s3_shared_name
  }
  force_destroy = true
}

