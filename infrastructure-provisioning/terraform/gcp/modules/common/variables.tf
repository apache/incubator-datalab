variable "project_tag" {}

variable "endpoint_tag" {}

variable "user_tag" {}

variable "custom_tag" {}

variable "region" {}

variable "product" {}

variable "vpc_name" {}

variable "fw_ingress" {}

variable "fw_egress_public" {}

variable "fw_egress_private" {}

variable "network_tag" {}

variable "cidr_range" {}

variable "traefik_cidr" {}

variable "ps_roles" {
  type = "list"
  default = [
    "roles/dataproc.worker"
  ]
}

variable "ps_policy" {
  type = "list"
  default = [

  ]
}