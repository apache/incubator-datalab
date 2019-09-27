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

variable "subscription_id" {
  default = "493eb9b7-74b9-4b16-8b5b-2b25cab1e41e"
}

variable "client_id" {
  default = "86d10c1d-a668-43fd-a2e7-debe517150ac"
}

variable "client_secret" {
  default = "3HqDqG]+b=0opkPQqB71vRS/c6104F+q"
}

variable "tenant_id" {
  default = "b41b72d0-4e9f-4c26-8a69-f949f367c91d"
}

variable "service_base_name" {
  default = "dem-edge1"
}

variable "resource_group" {
  default = "demyan-rg"
}

variable "project_name" {
  default = "proj1"
}

variable "project_tag" {
  default = "proj1"
}

variable "endpoint_tag" {
  default = "end1"
}

variable "user_tag" {
  default = "dem1"
}

variable "custom_tag" {
  default = ""
}

variable "os_env" {
  default = "redhat"
}

variable "region" {
  default = "westeurope"
}

variable "product" {
  default = "dlab"
}

variable "vpc_id" {
  default = "dem-vnet"
}

variable "subnet_id" {
  default = "/subscriptions/493eb9b7-74b9-4b16-8b5b-2b25cab1e41e/resourceGroups/demyan-rg/providers/Microsoft.Network/virtualNetworks/dem-vnet/subnets/default"
}

variable "ps_cidr" {
  default = "172.31.18.0/24"
}

variable "edge_cidr" {
  default = "172.31.1.0/24"
}

variable "edge_private_ip" {
  default = "172.31.1.4"
}

variable "instance_type" {
  default = "Standard_DS1_v2"
}

variable "ssh_key" {
  default = "~/.keys/id_rsa.pub"
}

variable "initial_user" {
  default = "ubuntu"
}

variable "ami_publisher" {
  type = "map"
  default = {
    debian = "Canonical"
    redhat = "RedHat"
    custom = ""
  }
}

variable "ami_offer" {
  type = "map"
  default = {
    debian = "UbuntuServer"
    redhat = "RHEL"
    custom = ""
  }
}

variable "ami_sku" {
  type = "map"
  default = {
    debian = "16.04-LTS"
    redhat = "7.3"
    custom = ""
  }
}

variable "ami_version" {
  type = "map"
  default = {
    debian = "16.04.201907290"
    redhat = "7.3.2017090800"
    custom = ""
  }
}