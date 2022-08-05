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

variable "gcp_project_id" {
  default = ""
}

variable "creds_file" {
  default = ""
}

variable "endpoint_shape" {
  default = "n1-standard-2"
}

variable "region" {
  default = ""
}

variable "zone" {
  default = ""
}

variable "service_base_name" {
  default = ""
}

variable "endpoint_id" {
  default = ""
}

variable "vpc_id" {
  default = ""
}

variable "ami" {
  default = "/projects/ubuntu-os-cloud/global/images/ubuntu-2004-focal-v20210119a"
}

variable "subnet_id" {
  default = ""
}

variable "endpoint_volume_size" {
  default = "20"
}

variable "subnet_cidr" {
  default = "172.31.0.0/24"
}

variable "firewall_ing_cidr_range" {
  type = list(string)
  default = ["0.0.0.0/0"]
}

variable "firewall_eg_cidr_range" {
  type = list(string)
  default = ["0.0.0.0/0"]
}

variable "endpoint_policies" {
  type = list(string)
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
    "compute.networks.get",
    "compute.networks.create",
    "compute.networks.delete",
    "compute.networks.updatePolicy",
    "compute.projects.setCommonInstanceMetadata",
    "compute.projects.setDefaultServiceAccount",
    "compute.subnetworks.create",
    "compute.subnetworks.delete",
    "compute.routes.create",
    "compute.routes.delete",
    "compute.routes.get"
  ]
}

variable "endpoint_roles" {
  type = list(string)
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

variable "path_to_pub_key" {
  default = ""
}

variable "product" {
  default = "datalab"
}

variable "additional_tag" {
  default = "product:datalab"
}

variable "ldap_host" {}

variable "ldap_dn" {}

variable "ldap_user" {}

variable "ldap_bind_creds" {}

variable "ldap_users_group" {}

variable "billing_enable" {}

variable "billing_dataset_name" {}

variable "mongo_password" {}

variable "mongo_host" {}
