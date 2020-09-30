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
provider "helm" {
    install_tiller  = true
    namespace       = "kube-system"
    service_account = "tiller"
    tiller_image    = "gcr.io/kubernetes-helm/tiller:v2.15.0"
}

provider "kubernetes" {}

resource "kubernetes_namespace" "datalab-namespace" {
  metadata {
    annotations = {
      name = var.namespace_name
    }

    name = var.namespace_name
  }
}

resource "kubernetes_namespace" "cert-manager-namespace" {
  metadata {
    annotations = {
      name = "cert-manager"
    }
    labels = {
      "certmanager.k8s.io/disable-validation" = "true"
    }

    name = "cert-manager"
  }
}

resource "kubernetes_storage_class" "datalab-storage-class" {
  metadata {
    name = "aws-ebs"
  }
  storage_provisioner = "kubernetes.io/aws-ebs"
  reclaim_policy = "Delete"
  parameters = {
    type = "gp2"
  }
}
