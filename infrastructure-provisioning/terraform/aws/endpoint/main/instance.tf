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
  endpoint_instance_name = "${var.service_base_name}-${var.endpoint_id}-endpoint"
}

resource "aws_instance" "endpoint" {
  ami                  = var.ami
  instance_type        = var.endpoint_instance_shape
  key_name             = var.key_name
  subnet_id            = data.aws_subnet.data_subnet.id
  security_groups      = [data.aws_security_group.data_sg.id]
  iam_instance_profile = aws_iam_instance_profile.endpoint_profile.name
  root_block_device {
    volume_type           = "gp2"
    volume_size           = var.endpoint_volume_size
    delete_on_termination = true
  }
  tags = {
    Name                           = local.endpoint_instance_name
    "${local.additional_tag[0]}"   = local.additional_tag[1]
    "${var.tag_resource_id}"       = "${var.service_base_name}:${local.endpoint_instance_name}"
    "${var.service_base_name}-tag" = local.endpoint_instance_name
    endpoint_id                    = var.endpoint_id
  }
}

resource "aws_eip_association" "e_ip_assoc" {
  instance_id   = aws_instance.endpoint.id
  allocation_id = aws_eip.endpoint_eip.id
  count         = var.network_type == "public" ? 1 : 0
}
