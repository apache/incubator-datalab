variable "access_key_var" {}

variable "secret_key_var" {}

variable "region" {
  default = "us-west-2"
}

variable "zone" {
  default = "a"
}

variable "service_base_name" {
  default = "k8s"
}

variable "vpc_id" {
  default = ""
}

variable "vpc_cidr" {
  default = "172.31.0.0/16"
}

variable "subnet_id" {
  default = ""
}

variable "subnet_cidr" {
  default = "172.31.0.0/24"
}

variable "env_os" {
  default = "debian"
}

variable "ami" {
  type = "map"
  default = {
    "debian" = "ami-08692d171e3cf02d6",
    "redhat" = ""
  }
}

variable "key_name" {
  default = "BDCC-DSS-POC"
}

variable "masters_count" {
  default = 3
}

variable "workers_count" {
  default = 2
}

variable "root_volume_size" {
  default = 30
}

variable "allowed_cidrs" {
  default = ["0.0.0.0/0"]
}

variable "masters_shape" {
  default = "t2.medium"
}

variable "workers_shape" {
  default = "t2.medium"
}

variable "os-user" {
  default = "dlab-user"
}

variable "project_tag" {
  default = ""
}

variable "endpoint_tag" {}

variable "user_tag" {}

variable "custom_tag" {}

variable "notebook_name" {}

variable "product_name" {}

variable "nb-sg_id" {}

variable "note_profile_name" {}

variable "note_cidr_range" {}

variable "traefik_cidr" {}

variable "note_ami" {}

variable "instance_type" {}

variable "cluster_name" {}

variable "slave_count" {}

variable "emr_template" {}

variable "master_shape" {}

variable "slave_shape" {}

variable "instance_count" {}

variable "bid_price" {}

variable "source_instance_id" {}