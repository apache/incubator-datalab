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

// AWS info
variable "access_key_id" {
  default = ""
}
variable "secret_access_key" {
  default = ""
}
variable "region" {
  default = "us-west-2"
}
variable "zone" {
  default = "a"
}

// Common
variable "env_os" {
  default = "debian"
}
variable "key_name" {
  default = "BDCC-DSS-POC"
}
variable "allowed_cidrs" {
  type = list
  default = ["0.0.0.0/0"]
}
variable "os_user" {
  default = "datalab-user"
}

variable "project_tag" {
  default = ""
}

variable "additional_tag" {
  default = "product:datalab"
}

variable "tag_resource_id" {
  default = "user:tag"
}

// SSN
variable "service_base_name" {
  default = "datalab-k8s"
}
variable "vpc_id" {
  default = ""
}
variable "vpc_cidr" {
  default = "172.31.0.0/16"
}
variable "subnet_id_a" {
  default = ""
}
variable "subnet_id_b" {
  default = ""
}
variable "subnet_cidr_a" {
  default = "172.31.0.0/24"
}
variable "subnet_cidr_b" {
  default = "172.31.1.0/24"
}
variable "subnet_cidr_c" {
  default = "172.31.2.0/24"
}
variable "ami" {
  default = "ami-07b4f3c02c7f83d59"
}
variable "ssn_k8s_masters_count" {
  default = 3
}
variable "ssn_k8s_workers_count" {
  default = 2
}
variable "ssn_root_volume_size" {
  default = 30
}
variable "ssn_k8s_masters_shape" {
  default = "t2.medium"
}
variable "ssn_k8s_workers_shape" {
  default = "t2.medium"
}
variable "kubernetes_version" {
  default = "1.15.5-00"
}