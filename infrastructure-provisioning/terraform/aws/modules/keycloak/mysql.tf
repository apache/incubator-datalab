resource "helm_release" "keycloak-mysql" {
  name = "keycloak-mysql"
  chart = "stable/mysql"
  wait = false

  set {
    name = "mysqlRootPassword"
    value = "1234567890o"
  }

  set {
    name = "mysqlUser"
    value = "keycloak"
  }

  set {
    name = "mysqlPassword"
    value = "1234567890o"
  }

  set {
    name = "mysqlDatabase"
    value = "keycloak"
  }

  set {
    name = "persistence.existingClaim"
    value = "${kubernetes_persistent_volume_claim.example.metadata.0.name}"
  }
}


provider "kubernetes" {
  }

resource "kubernetes_persistent_volume" "example" {
  metadata {
    name = "mysql-keycloak-pv2"
  }
  spec {
    capacity = {
      storage = "8Gi"
    }
    access_modes = ["ReadWriteMany"]
    persistent_volume_source {
      host_path {
        path = "/home/dlab-user/keycloak-pv2"
      }
    }
  }
}


resource "kubernetes_persistent_volume_claim" "example" {
  metadata {
    name = "mysql-keycloak-pvc2"
  }
  spec {
    access_modes = ["ReadWriteMany"]
    resources {
      requests = {
        storage = "5Gi"
      }
    }
    volume_name = "${kubernetes_persistent_volume.example.metadata.0.name}"
  }
}

