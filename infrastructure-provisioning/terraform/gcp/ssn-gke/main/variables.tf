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

variable "credentials_file_path" {
  default = ""
}

variable "project_id" {
  default = ""
}

variable "region" {
  default = "us-west1"
}

variable "zone" {
  default = "a"
}

variable "vpc_name" {
  default = ""
}

variable "subnet_name" {
  default = ""
}

variable "service_base_name" {
  default = "dlab-k8s"
}

variable "subnet_cidr" {
  default = "172.31.0.0/24"
}

variable "additional_tag" {
  default = "product:dlab"
}

variable "ssn_k8s_workers_count" {
  default = 1
}

variable "gke_cluster_version" {
  default = "1.12.8-gke.10"
}

// Couldn't assign in GCP
//variable "tag_resource_id" {
//  default = "user:tag"
//}

variable "ssn_k8s_workers_shape" {
  default = "n1-standard-1"
}