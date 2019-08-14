provider "azurerm" {
  credentials = var.credentials
  region      = var.region
}

resource "azurerm_resource_group" "env_rg" {
    name     = var.service_base_name
    location = var.region

    tags = {
        Name = var.service_base_name
    }
}


module "common" {
  source        = "../modules/common"
  sbn           = var.service_base_name
  project_name  = var.project_name
  project_tag   = var.project_tag
  endpoint_tag  = var.endpoint_tag
  user_tag      = var.user_tag
  custom_tag    = var.custom_tag
  notebook_name = var.notebook_name
  region        = var.region
  zone          = var.zone
  product       = var.product_name
  vpc           = var.vpc_id
  cidr_range    = var.cidr_range
  traefik_cidr  = var.traefik_cidr
  instance_type = var.instance_type
  env_rg        = azurerm_resource_group.env_rg.name
}

module "notebook" {
  source           = "../modules/notebook"
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
  instance_type    = var.instance_type
  ssh_key          = var.ssh_key
  initial_user     = var.initial_user
  env_rg           = azurerm_resource_group.env_rg.name
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
  instance_type    = var.instance_type
  ssh_key          = var.ssh_key
  initial_user     = var.initial_user
  cluster_name     = var.cluster_name
  slave_count      = var.slave_count
  env_rg           = azurerm_resource_group.env_rg.name
}

module "ami" {
  source             = "../modules/ami"
  sbn                = var.service_base_name
  project_name       = var.project_name
  source_instance_id = var.source_instance_id
  project_tag        = var.project_tag
  notebook_name      = var.notebook_name
  product            = var.product_name
  endpoint_tag       = var.endpoint_tag
  user_tag           = var.user_tag
  custom_tag         = var.custom_tag
  env_rg             = azurerm_resource_group.env_rg.name
}