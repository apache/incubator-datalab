provider "aws" {
  region                  = var.region
}

module "ssn-k8s" {
  source            = "../modules/ssn-k8s"
  service_base_name = var.service_base_name
  vpc_id            = var.vpc_id
  vpc_cidr          = var.vpc_cidr
  subnet_id         = var.subnet_id
  env_os            = var.env_os
  ami               = var.ami
  key_name          = var.key_name
  region            = var.region
  zone              = var.zone
  masters_count     = var.masters_count
  workers_count     = var.workers_count
  root_volume_size  = var.root_volume_size
  allowed_cidrs     = var.allowed_cidrs
  subnet_cidr       = var.subnet_cidr
  masters_shape     = var.masters_shape
  workers_shape     = var.workers_shape
  os-user           = var.os-user
}
