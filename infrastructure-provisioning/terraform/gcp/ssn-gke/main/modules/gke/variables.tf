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

variable "additional_tag" {}

variable "service_base_name" {}

variable "region" {}

variable "gke_cluster_version" {}

variable "ssn_k8s_workers_count" {}

variable "ssn_k8s_workers_shape" {}

variable "project_id" {}

variable "service_account_iam_roles" {}

variable "vpc_name" {}

variable "subnet_name" {}

variable "subnet_cidr" {}

