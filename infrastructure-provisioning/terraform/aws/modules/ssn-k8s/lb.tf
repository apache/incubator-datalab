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

resource "aws_lb" "ssn_k8s_lb" {
  name               = "${var.service_base_name}-ssn-lb"
  load_balancer_type = "network"

  subnet_mapping {
    subnet_id     = data.aws_subnet.k8s-subnet-data.id
    allocation_id = aws_eip.k8s-lb-eip.id
  }
  tags = {
    Name = "${var.service_base_name}-ssn-lb"
  }
}

resource "aws_lb_target_group" "ssn_k8s_lb_target_group" {
  name     = "${var.service_base_name}-ssn-lb-target-group"
  port     = 6443
  protocol = "TCP"
  vpc_id   = data.aws_vpc.ssn_k8s_vpc_data.id
  tags = {
    Name = "${var.service_base_name}-ssn-lb-target-group"
  }
}

resource "aws_lb_listener" "ssn_k8s_lb_listener" {
  load_balancer_arn = aws_lb.ssn_k8s_lb.arn
  port              = "6443"
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.ssn_k8s_lb_target_group.arn
  }
}