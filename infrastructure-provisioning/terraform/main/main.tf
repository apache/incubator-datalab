provider "aws" {
  access_key = "${var.access_key_var}"
  secret_key = "${var.secret_key_var}"
  region     = "${var.region_var}"
}

module "common" {
  source        = "../modules/common"
  project_tag   = "${var.project_tag}"
  endpoint_tag  = "${var.endpoint_tag}"
  user_tag      = "${var.user_tag}"
  custom_tag    = "${var.custom_tag}"
  notebook_name = "${var.notebook_name}"
  region        = "${var.region_var}"
  zone          = "${var.zone_var}"
  product       = "${var.product_name}"
  vpc           = "${var.vpc_id}"
  cidr_range    = "${var.cidr_range}"
  traefik_cidr  = "${var.traefik_cidr}"
  instance_type = "${var.instance_type}"
}

module "notebook" {
  source           = "../modules/notebook"
  project_tag      = "${var.project_tag}"
  endpoint_tag     = "${var.endpoint_tag}"
  user_tag         = "${var.user_tag}"
  custom_tag       = "${var.custom_tag}"
  notebook_name    = "${var.notebook_name}"
  subnet_id        = "${var.subnet_id}"
  nb-sg_id         = "${var.nb-sg_id}"
  iam_profile_name = "${var.iam_profile_name}"
  product          = "${var.product_name}"
  ami              = "${var.ami}"
  instance_type    = "${var.instance_type}"
  key_name         = "${var.key_name}"
}

module "data_engine" {
  source           = "../modules/data_engine"
  project_tag      = "${var.project_tag}"
  endpoint_tag     = "${var.endpoint_tag}"
  user_tag         = "${var.user_tag}"
  custom_tag       = "${var.custom_tag}"
  notebook_name    = "${var.notebook_name}"
  subnet_id        = "${var.subnet_id}"
  nb-sg_id         = "${var.nb-sg_id}"
  iam_profile_name = "${var.iam_profile_name}"
  product          = "${var.product_name}"
  ami              = "${var.ami}"
  instance_type    = "${var.instance_type}"
  key_name         = "${var.key_name}"
  cluster_name     = "${var.cluster_name}"
  slave_count      = "${var.slave_count}"
}

module "emr" {
  source           = "../modules/emr"
  project_tag      = "${var.project_tag}"
  endpoint_tag     = "${var.endpoint_tag}"
  user_tag         = "${var.user_tag}"
  custom_tag       = "${var.custom_tag}"
  notebook_name    = "${var.notebook_name}"
  subnet_id        = "${var.subnet_id}"
  nb-sg_id         = "${var.nb-sg_id}"
  iam_profile_name = "${var.iam_profile_name}"
  product          = "${var.product_name}"
  ami              = "${var.ami}"
  emr_template     = "${var.emr_template}"
  master_shape     = "${var.master_shape}"
  slave_shape      = "${var.slave_shape}"
  key_name         = "${var.key_name}"
  cluster_name     = "${var.cluster_name}"
  instance_count   = "${var.instance_count}"
  bid_price        = "${var.bid_price}"
}

module "ami" {
  source             = "../modules/ami"
  source_instance_id = "${var.source_instance_id}"
  project_tag        = "${var.project_tag}"
  notebook_name      = "${var.notebook_name}"
}