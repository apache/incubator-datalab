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
  node_name = "${var.project_tag}-nb-${var.notebook_name}"
}

resource "aws_instance" "notebook" {
  ami                  = "${var.note_ami}"
  instance_type        = "${var.instance_type}"
  key_name             = "${var.key_name}"
  subnet_id            = "${var.subnet_id}"
  security_groups      = ["${var.nb-sg_id}"]
  iam_instance_profile = "${var.note_profile_name}"
  tags = {
    Name                     = "${local.node_name}"
    "${var.project_tag}-Tag" = "${local.node_name}"
    Endpoint_Tag             = "${var.endpoint_tag}"
    "user:tag"               = "${var.project_tag}:${local.node_name}"
    product                  = "${var.product}"
    User_Tag                 = "${var.user_tag}"
    Custom_Tag               = "${var.custom_tag}"
  }
}