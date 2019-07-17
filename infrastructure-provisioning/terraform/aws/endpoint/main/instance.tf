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
  ec2_name = "${var.service_base_name}-endpoint"
  eip_name = "${var.service_base_name}-endpoint-EIP"
}


resource "aws_instance" "endpoint" {
  ami             = var.ami
  instance_type   = var.endpoint_instance_shape
  key_name        = var.key_name
  subnet_id       = data.aws_subnet.data_subnet.id
  security_groups = ["${aws_security_group.endpoint_sec_group.id}"]
  root_block_device {
    volume_type           = "gp2"
    volume_size           = var.endpoint_volume_size
    delete_on_termination = true
  }
  tags = {
    Name = "${local.ec2_name}"
    "${var.service_base_name}-Tag" = "${local.ec2_name}"
    product = "${var.product}"
    "user:tag" = "${var.service_base_name}:${local.ec2_name}"
  }
}


resource "aws_eip_association" "e_ip_assoc" {
  instance_id   = aws_instance.endpoint.id
  allocation_id = var.endpoint_eip_allocation_id
  count         = var.network_type == "public" ? 1 : 0
}
