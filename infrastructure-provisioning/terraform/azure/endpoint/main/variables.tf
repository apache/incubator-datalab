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

variable "auth_file_path" {}

variable "resource_group_name" {
  default = ""
}

variable "region" {
  default = "West US 2"
}

variable "service_base_name" {}

variable "endpoint_id" {}

variable "additional_tag" {
  default = "product:datalab"
}

variable "vpc_cidr" {}

variable "tag_resource_id" {
  default = "user:tag"
}

variable "vpc_id" {
  default = ""
}

variable "subnet_id" {
  default = ""
}

variable "allowed_ip_cidrs" {
  type    = list(string)
  default = ["0.0.0.0/0"]
}

variable "subnet_cidr" {}

variable "endpoint_shape" {}

variable "ami" {
  default = "Canonical:0001-com-ubuntu-server-focal:20_04-lts"
}

variable "endpoint_volume_size" {}

variable "key_path" {}

variable "authentication_file" {
  default = ""
}

variable "offer_number" {}

variable "currency" {}

variable "locale" {}

variable "region_info" {}

variable "mongo_password" {}

variable "mongo_host" {}

variable "billing_enable" {}