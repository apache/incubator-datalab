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

data "google_container_cluster" "ssn_k8s_gke_cluster" {
  name       = var.gke_cluster_name
  location   = var.region
  depends_on = []
}

data "google_client_config" "current" {}

provider "helm" {
  version = "0.10.6"

  kubernetes {
    host                   = data.google_container_cluster.ssn_k8s_gke_cluster.endpoint
    token                  = data.google_client_config.current.access_token
    client_certificate     = base64decode(data.google_container_cluster.ssn_k8s_gke_cluster.master_auth.0.client_certificate)
    client_key             = base64decode(data.google_container_cluster.ssn_k8s_gke_cluster.master_auth.0.client_key)
    cluster_ca_certificate = base64decode(data.google_container_cluster.ssn_k8s_gke_cluster.master_auth.0.cluster_ca_certificate)
  }

  install_tiller = true
  service_account = kubernetes_service_account.tiller_sa.metadata.0.name
}

provider "kubernetes" {
  version = "1.10.0"
  load_config_file = false
  host = data.google_container_cluster.ssn_k8s_gke_cluster.endpoint

  client_certificate     = base64decode(data.google_container_cluster.ssn_k8s_gke_cluster.master_auth.0.client_certificate)
  client_key             = base64decode(data.google_container_cluster.ssn_k8s_gke_cluster.master_auth.0.client_key)
  cluster_ca_certificate = base64decode(data.google_container_cluster.ssn_k8s_gke_cluster.master_auth.0.cluster_ca_certificate)
}

resource "kubernetes_service_account" "tiller_sa" {
  metadata {
    name = "tiller"
    namespace = "kube-system"
  }
}

resource "kubernetes_role_binding" "tiller_rb" {
  metadata {
    name      = "tiller"
    namespace = "kube-system"
  }
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "cluster-admin"
  }
  subject {
    kind      = "ServiceAccount"
    name      = "tiller"
    namespace = "kube-system"
  }
}

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
