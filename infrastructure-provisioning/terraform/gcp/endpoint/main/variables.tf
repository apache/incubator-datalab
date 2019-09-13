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
# id of gcp project
variable "project_name" {
  default = ""
}
# path to .json file with creds
variable "creds_file" {
  default = ""
}

variable "endpoint_shape" {
  default = "n1-standard-2"
}
# for example <us-west1>
variable "region" {
  default = ""
}
# for example <us-west1-a>
variable "zone" {
  default = ""
}

variable "service_base_name" {
  default = ""
}

variable "endpoint_id" {
  default = ""
}

variable "vpc_name" {
  default = ""
}

variable "ami" {
  default = "/projects/ubuntu-os-cloud/global/images/ubuntu-1604-xenial-v20190628"
}

variable "subnet_name" {
  default = ""
}

variable "endpoint_volume_size" {
  default = "20"
}

variable "subnet_cidr" {
  default = "172.31.0.0/24"
}
# TEMPORARY
variable "firewall_ing_cidr_range" {
  default = "0.0.0.0/0"
}
# created by ssn (bcs of certs)
variable "endpoint_eip" {
  default = ""
}

variable "firewall_eg_cidr_range" {
  default = "0.0.0.0/0"
}

variable "endpoint_policies" {
  type = "list"
  default = [
    "storage.buckets.create",
    "storage.buckets.delete",
    "storage.buckets.get",
    "storage.buckets.getIamPolicy",
    "storage.buckets.list",
    "storage.buckets.setIamPolicy",
    "storage.buckets.update",
    "storage.objects.create",
    "storage.objects.delete",
    "storage.objects.get",
    "storage.objects.getIamPolicy",
    "storage.objects.list",
    "storage.objects.setIamPolicy",
    "storage.objects.update",
    "compute.autoscalers.get",
    "compute.instances.get",
    "compute.healthChecks.get",
    "compute.addresses.create",
    "compute.addresses.delete",
    "compute.firewalls.create",
    "compute.firewalls.delete",
    "compute.firewalls.get",
    "compute.firewalls.list",
    "compute.images.create",
    "compute.images.delete",
    "compute.images.get",
    "compute.images.list",
    "compute.images.setLabels",
    "compute.networks.create",
    "compute.networks.delete",
    "compute.networks.updatePolicy",
    "compute.projects.setCommonInstanceMetadata",
    "compute.projects.setDefaultServiceAccount",
    "compute.subnetworks.create",
    "compute.subnetworks.delete"
  ]
}

variable "endpoint_roles" {
  type = "list"
  default = [
    "roles/iam.serviceAccountUser",
    "roles/iam.serviceAccountAdmin",
    "roles/storage.admin",
    "roles/dataproc.editor",
    "roles/resourcemanager.projectIamAdmin",
    "roles/iam.roleAdmin",
    "roles/compute.instanceAdmin",
    "roles/bigquery.dataViewer",
    "roles/bigquery.jobUser"
  ]
}
# path for public key to connect to instance
variable "path_to_pub_key" {
  default = ""
}

variable "product" {
  default = "dlab"
}