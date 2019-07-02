locals {
  cluster_name                = "${var.project_tag}-de-${var.notebook_name}-${var.cluster_name}"
  notebook_name = "${var.project_tag}-nb-${var.notebook_name}"
}

resource "aws_instance" "master" {
  ami                  = "${var.ami}"
  instance_type        = "${var.instance_type}"
  key_name             = "${var.key_name}"
  subnet_id            = "${var.subnet_id}"
  security_groups      = ["${var.nb-sg_id}"]
  iam_instance_profile = "${var.note_profile_name}"
  tags = {
    Name                     = "${local.cluster_name}-m"
    Type                     = "master"
    dataengine_notebook_name = "${local.notebook_name}"
    "${var.project_tag}-Tag" = "${local.cluster_name}-m"
    User_tag                 = "${var.user_tag}"
    Endpoint_Tag             = "${var.endpoint_tag}"
    "user:tag"               = "${var.project_tag}:${local.cluster_name}"
    Custom_Tag               = "${var.custom_tag}"
  }
}


resource "aws_instance" "slave" {
  count                = "${var.slave_count}"
  ami                  = "${var.ami}"
  instance_type        = "${var.instance_type}"
  key_name             = "${var.key_name}"
  subnet_id            = "${var.subnet_id}"
  security_groups      = ["${var.nb-sg_id}"]
  iam_instance_profile = "${var.note_profile_name}"
  tags = {
    Name                     = "${local.cluster_name}-s${count.index + 1}"
    Type                     = "slave"
    dataengine_notebook_name = "${local.notebook_name}"
    "${var.project_tag}-Tag" = "${local.cluster_name}-s${count.index + 1}"
    User_tag                 = "${var.user_tag}"
    Endpoint_Tag             = "${var.endpoint_tag}"
    "user:tag"               = "${var.project_tag}:${local.cluster_name}"
    Custom_Tag               = "${var.custom_tag}"
  }
}