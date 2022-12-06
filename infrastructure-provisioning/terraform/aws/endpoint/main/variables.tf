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

variable "service_base_name" {}

variable "access_key_id" {
  default = ""
}
variable "secret_access_key" {
  default = ""
}

variable "region" {}

variable "zone" {}

variable "product" {}

variable "subnet_cidr" {}

variable "allowed_ip_cidrs" {
  type    = list(string)
  default = ["0.0.0.0/0"]
}

variable "endpoint_instance_shape" {}

variable "key_name" {}

variable "ami" {
  default = "ami-07dd19a7900a1f049"
}

variable "vpc_id" {
  default = ""
}

variable "sg_id" {
  default = ""
}

variable "subnet_id" {
  default = ""
}

variable "network_type" {}

variable "vpc_cidr" {}

variable "endpoint_volume_size" {}

variable "endpoint_id" {}

variable "ssn_k8s_sg_id" {
  default = ""
}

variable "ldap_host" {}

variable "ldap_dn" {}

variable "ldap_user" {}

variable "ldap_bind_creds" {}

variable "ldap_users_group" {}

variable "additional_tag" {
  default = "product:datalab"
}

variable "tag_resource_id" {
  default = "user:tag"
}

variable "billing_enable" {}

variable "mongo_password" {}

variable "mongo_host" {}

variable "billing_bucket" {}

variable "report_path" {
  default = ""
}

variable "aws_job_enabled" {
  default = "false"
}

variable "billing_aws_account_id" {}

variable "billing_tag" {}
