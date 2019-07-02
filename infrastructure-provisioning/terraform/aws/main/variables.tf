// AWS info
variable "access_key_id" {}
variable "secret_access_key" {}
variable "region" {
  default = "us-west-2"
}
variable "zone" {
  default = "a"
}

// Common
variable "env_os" {
  default = "debian"
}
variable "key_name" {
  default = "BDCC-DSS-POC"
}
variable "allowed_cidrs" {
  default = ["0.0.0.0/0"]
}
variable "os-user" {
  default = "dlab-user"
}

variable "project_tag" {
  default = ""
}

// SSN
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
variable "ami" {
  default = "ami-08692d171e3cf02d6"
}
variable "ssn_k8s_masters_count" {
  default = 3
}
variable "ssn_k8s_workers_count" {
  default = 2
}
variable "ssn_root_volume_size" {
  default = 30
}
variable "ssn_k8s_masters_shape" {
  default = "t2.medium"
}

variable "ssn_k8s_workers_shape" {
  default = "t2.medium"
}

variable "endpoint_tag" {
  default = ""
}

variable "user_tag" {
  default = ""
}

variable "custom_tag" {
  default = ""
}

variable "notebook_name" {
  default = ""
}

variable "product_name" {
  default = ""
}

variable "nb-sg_id" {
  default = ""
}

variable "note_profile_name" {
  default = ""
}

variable "note_cidr_range" {
  default = ""
}

variable "traefik_cidr" {
  default = ""
}

variable "note_ami" {
  default = ""
}

variable "instance_type" {
  default = ""
}

variable "cluster_name" {
  default = ""
}

variable "slave_count" {
  default = ""
}

variable "emr_template" {
  default = ""
}

variable "master_shape" {
  default = ""
}

variable "slave_shape" {
  default = ""
}

variable "instance_count" {
  default = ""
}

variable "bid_price" {
  default = ""
}

variable "source_instance_id" {
  default = ""
}