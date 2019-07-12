resource "helm_release" "keycloak" {
  name = "keycloak"
  chart = "stable/keycloak"
  wait = false

  set {
    name = "keycloak.username"
    value = "dlab-admin"
  }

  set {
    name = "keycloak.password"
    value = "12345o"
  }

  set {
    name = "keycloak.persistence.dbVendor"
    value = "mysql"
  }

  set {
    name = "keycloak.persistence.dbName"
    value = "keycloak"
  }

  set {
    name = "keycloak.persistence.dbHost"
    value = "keycloak-mysql"
  }

  set {
    name = "keycloak.persistence.dbPort"
    value = "3306"
  }

  set {
    name = "keycloak.persistence.dbUser"
    value = "keycloak"
  }

 set {
    name = "keycloak.persistence.dbPassword"
    value = "1234567890o"
  }

  set {
    name = "keycloak.service.type"
    value = "NodePort"
  }

  set {
    name = "keycloak.service.nodePort"
    value = "31088"
  }

}