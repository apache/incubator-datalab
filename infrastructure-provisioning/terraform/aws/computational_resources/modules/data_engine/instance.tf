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
  cluster_name  = "${var.sbn}-de-${var.notebook_name}-${var.cluster_name}"
  notebook_name = "${var.sbn}-nb-${var.notebook_name}"
}

resource "aws_instance" "master" {
  ami                  = var.ami
  instance_type        = var.instance_type
  key_name             = var.key_name
  subnet_id            = var.subnet_id
  security_groups      = ["${var.nb-sg_id}"]
  iam_instance_profile = var.iam_profile_name
  tags = {
    Name                     = "${local.cluster_name}-m"
    Type                     = "master"
    dataengine_notebook_name = local.notebook_name
    "${var.sbn}-tag"         = "${local.cluster_name}-m"
    Product                  = var.product
    Project_name             = var.project_name
    Project_tag              = var.project_tag
    User_tag                 = var.user_tag
    Endpoint_Tag             = var.endpoint_tag
    "user:tag"               = "${var.sbn}:${local.cluster_name}"
    Custom_Tag               = var.custom_tag
  }
}


resource "aws_instance" "slave" {
  count                = var.slave_count
  ami                  = var.ami
  instance_type        = var.instance_type
  key_name             = var.key_name
  subnet_id            = var.subnet_id
  security_groups      = ["${var.nb-sg_id}"]
  iam_instance_profile = var.iam_profile_name
  tags = {
    Name                     = "${local.cluster_name}-s${count.index + 1}"
    Type                     = "slave"
    dataengine_notebook_name = local.notebook_name
    "${var.sbn}-tag"         = "${local.cluster_name}-s${count.index + 1}"
    Product                  = var.product
    Project_name             = var.project_name
    Project_tag              = var.project_tag
    User_tag                 = var.user_tag
    Endpoint_Tag             = var.endpoint_tag
    "user:tag"               = "${var.sbn}:${local.cluster_name}"
    Custom_Tag               = var.custom_tag
  }
}