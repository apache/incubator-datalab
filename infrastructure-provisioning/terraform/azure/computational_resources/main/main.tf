provider "azurerm" {
  subscription_id = var.subscription_id
  client_id       = var.client_id
  client_secret   = var.client_secret
  tenant_id       = var.tenant_id
}

module "common" {
  source         = "../modules/common"
  sbn            = var.service_base_name
  project_name   = var.project_name
  project_tag    = var.project_tag
  endpoint_tag   = var.endpoint_tag
  user_tag       = var.user_tag
  custom_tag     = var.custom_tag
  notebook_name  = var.notebook_name
  region         = var.region
  product        = var.product_name
  vpc            = var.vpc_id
  cidr_range     = var.cidr_range
  traefik_cidr   = var.traefik_cidr
  instance_type  = var.instance_type
  resource_group = var.resource_group
}

module "notebook" {
  source           = "../modules/notebook"
  sbn              = var.service_base_name
  project_name     = var.project_name
  project_tag      = var.project_tag
  endpoint_tag     = var.endpoint_tag
  user_tag         = var.user_tag
  custom_tag       = var.custom_tag
  os_env           = var.os_env
  notebook_name    = var.notebook_name
  region           = var.region
  subnet_id        = var.subnet_id
  nb-sg_id         = var.nb-sg_id
  product          = var.product_name
  ami              = var.ami
  custom_ami       = var.custom_ami
  instance_type    = var.instance_type
  ssh_key          = var.ssh_key
  initial_user     = var.initial_user
  resource_group   = var.resource_group
}

module "data_engine" {
  source           = "../modules/data_engine"
  sbn              = var.service_base_name
  project_name     = var.project_name
  project_tag      = var.project_tag
  endpoint_tag     = var.endpoint_tag
  user_tag         = var.user_tag
  custom_tag       = var.custom_tag
  notebook_name    = var.notebook_name
  region           = var.region
  subnet_id        = var.subnet_id
  nb-sg_id         = var.nb-sg_id
  product          = var.product_name
  ami              = var.ami
  master_shape     = var.master_shape
  slave_shape      = var.slave_shape
  ssh_key          = var.ssh_key
  initial_user     = var.initial_user
  cluster_name     = var.cluster_name
  slave_count      = var.slave_count
  resource_group   = var.resource_group
}