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
  cluster_name  = "${var.sbn}-des-${var.notebook_name}-${var.cluster_name}"
  notebook_name = "${var.sbn}-nb-${var.notebook_name}"
}

resource "aws_emr_cluster" "cluster" {
  name          = local.cluster_name
  release_label = var.emr_template
  applications  = ["Spark"]

  termination_protection            = false
  keep_job_flow_alive_when_no_steps = true

  ec2_attributes {
    subnet_id                         = var.subnet_id
    emr_managed_master_security_group = var.nb-sg_id
    emr_managed_slave_security_group  = var.nb-sg_id
    instance_profile                  = "arn:aws:iam::203753054073:instance-profile/EMR_EC2_DefaultRole"
  }

  master_instance_group {
    instance_type = var.master_shape
  }

  core_instance_group {
    instance_type  = var.slave_shape
    instance_count = "${var.instance_count - 1}"

    ebs_config {
      size                 = "40"
      type                 = "gp2"
      volumes_per_instance = 1
    }

    bid_price = "0.${var.bid_price}"
  }

  ebs_root_volume_size = 100

  tags = {
    ComputationalName        = var.cluster_name
    Name                     = local.cluster_name
    Notebook                 = local.notebook_name
    Product                  = var.product
    "${var.sbn}-tag"         = local.cluster_name
    Project_name             = var.project_name
    Project_tag              = var.project_tag
    User_tag                 = var.user_tag
    Endpoint_Tag             = var.endpoint_tag
    "user:tag"               = "${var.sbn}:${local.cluster_name}"
    Custom_Tag               = var.custom_tag
  }

  bootstrap_action {
    path = "s3://elasticmapreduce/bootstrap-actions/run-if"
    name = "runif"
    args = ["instance.isMaster=true", "echo running on master node"]
  }

  service_role = "arn:aws:iam::203753054073:role/EMR_DefaultRole"
}