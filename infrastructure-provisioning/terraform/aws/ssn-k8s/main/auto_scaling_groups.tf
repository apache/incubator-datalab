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
  subnet_c_id = data.aws_subnet.k8s-subnet-c-data == [] ? "" : data.aws_subnet.k8s-subnet-c-data.0.id
}

resource "random_string" "ssn_keystore_password" {
  length = 16
  special = false
}

resource "random_string" "endpoint_keystore_password" {
  length = 16
  special = false
}

output "ssn_keystore_password" {
  value = random_string.ssn_keystore_password.result
}

output "endpoint_keystore_password" {
  value = random_string.endpoint_keystore_password.result
}

data "template_file" "ssn_k8s_masters_user_data" {
  template = file("./files/masters-user-data.sh")
  vars = {
    k8s-asg                    = "${var.service_base_name}-ssn-masters"
    k8s-region                 = var.region
    k8s-bucket-name            = aws_s3_bucket.ssn_k8s_bucket.id
    k8s-nlb-dns-name           = aws_lb.ssn_k8s_nlb.dns_name #aws_eip.k8s-lb-eip.public_ip
    k8s-tg-arn                 = aws_lb_target_group.ssn_k8s_nlb_api_target_group.arn
    k8s_os_user                = var.os_user
    ssn_keystore_password      = random_string.ssn_keystore_password.result
    endpoint_keystore_password = random_string.endpoint_keystore_password.result
    endpoint_elastic_ip        = aws_eip.k8s-endpoint-eip.public_ip
  }
}

data "template_file" "ssn_k8s_workers_user_data" {
  template = file("./files/workers-user-data.sh")
  vars = {
    k8s-bucket-name = aws_s3_bucket.ssn_k8s_bucket.id
    k8s_os_user = var.os_user
  }
}

resource "aws_launch_configuration" "ssn_k8s_launch_conf_masters" {
  name                 = "${var.service_base_name}-ssn-launch-conf-masters"
  image_id             = var.ami
  instance_type        = var.ssn_k8s_masters_shape
  key_name             = var.key_name
  security_groups      = [aws_security_group.ssn_k8s_sg.id]
  iam_instance_profile = aws_iam_instance_profile.k8s-profile.name
  root_block_device {
    volume_type           = "gp2"
    volume_size           = var.ssn_root_volume_size
    delete_on_termination = true
  }

  lifecycle {
    create_before_destroy = true
  }
  user_data = data.template_file.ssn_k8s_masters_user_data.rendered
}

resource "aws_launch_configuration" "ssn_k8s_launch_conf_workers" {
  name                 = "${var.service_base_name}-ssn-launch-conf-workers"
  image_id             = var.ami
  instance_type        = var.ssn_k8s_workers_shape
  key_name             = var.key_name
  security_groups      = [aws_security_group.ssn_k8s_sg.id]
  iam_instance_profile = aws_iam_instance_profile.k8s-profile.name
  root_block_device {
    volume_type           = "gp2"
    volume_size           = var.ssn_root_volume_size
    delete_on_termination = true
  }

  lifecycle {
    create_before_destroy = true
  }
  user_data = data.template_file.ssn_k8s_workers_user_data.rendered
}

resource "aws_autoscaling_group" "ssn_k8s_autoscaling_group_masters" {
  name                 = "${var.service_base_name}-ssn-masters"
  launch_configuration = aws_launch_configuration.ssn_k8s_launch_conf_masters.name
  min_size             = var.ssn_k8s_masters_count
  max_size             = var.ssn_k8s_masters_count
  vpc_zone_identifier  = compact([data.aws_subnet.k8s-subnet-a-data.id, data.aws_subnet.k8s-subnet-b-data.id,
                                  local.subnet_c_id])
  target_group_arns    = [aws_lb_target_group.ssn_k8s_nlb_api_target_group.arn,
                          aws_lb_target_group.ssn_k8s_nlb_mongo_target_group.arn,
                          aws_lb_target_group.ssn_k8s_alb_target_group.arn]

  lifecycle {
    create_before_destroy = true
  }
  tags = [
    {
      key                 = "Name"
      value               = "${var.service_base_name}-ssn-masters"
      propagate_at_launch = true
    }
  ]
}

resource "aws_autoscaling_group" "ssn_k8s_autoscaling_group_workers" {
  name                 = "${var.service_base_name}-ssn-workers"
  launch_configuration = aws_launch_configuration.ssn_k8s_launch_conf_workers.name
  min_size             = var.ssn_k8s_workers_count
  max_size             = var.ssn_k8s_workers_count
  vpc_zone_identifier  = compact([data.aws_subnet.k8s-subnet-a-data.id, data.aws_subnet.k8s-subnet-b-data.id,
                                  local.subnet_c_id])

  lifecycle {
    create_before_destroy = true
  }
  tags = [
    {
      key                 = "Name"
      value               = "${var.service_base_name}-ssn-workers"
      propagate_at_launch = true
    }
  ]
}

data "aws_instances" "ssn_k8s_masters_instances" {
  instance_tags = {
    Name = aws_autoscaling_group.ssn_k8s_autoscaling_group_masters.name
  }

  instance_state_names = ["running"]
  depends_on = [aws_autoscaling_group.ssn_k8s_autoscaling_group_masters]
}
