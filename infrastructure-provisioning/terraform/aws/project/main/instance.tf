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
  edge_instance_name = "${var.service_base_name}-edge"
}

resource "aws_instance" "edge" {
  ami                  = var.ami
  instance_type        = var.instance_type
  key_name             = var.key_name
  subnet_id            = var.subnet_id
  security_groups      = [aws_security_group.edge_sg.id]
  iam_instance_profile = aws_iam_instance_profile.edge_profile.id
  root_block_device {
    volume_type           = "gp2"
    volume_size           = var.edge_volume_size
    delete_on_termination = true
  }
  tags = {
    Name                           = local.edge_instance_name
    "${local.additional_tag[0]}"   = local.additional_tag[1]
    "${var.tag_resource_id}"       = "${var.service_base_name}:${local.edge_instance_name}"
    "${var.service_base_name}-tag" = local.edge_instance_name
    "Endpoint_tag"                 = var.endpoint_tag
  }
}

resource "aws_eip_association" "edge_ip_assoc" {
  instance_id   = aws_instance.edge.id
  allocation_id = aws_eip.edge_ip.id
}
