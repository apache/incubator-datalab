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
  ami_name = "${var.sbn}-ami"
}

resource "aws_ami_from_instance" "ami" {
  name               = "${var.project_tag}-${var.notebook_name}-ami"
  source_instance_id = var.source_instance_id
  tags {
    Name             = local.ami_name
    "${var.sbn}-tag" = local.ami_name
    Product          = var.product
    Project_name     = var.project_name
    Project_tag      = var.project_tag
    Endpoint_tag     = var.endpoint_tag
    "user:tag"       = "${var.sbn}:${local.ami_name}"
    User_tag         = var.user_tag
    Custom_tag       = var.custom_tag
  }
}