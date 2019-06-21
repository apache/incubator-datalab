locals {
  node_name                = "${var.project_tag}-de-${var.notebook_name}-Spark"
  dataengine_notebook_name = "${var.project_tag}-nb-${var.notebook_name}"
}

resource "aws_instance" "master" {
  ami                  = "${var.ami}"
  instance_type        = "${var.instance_type}"
  subnet_id            = "${var.aws_subnet_id}"
  security_groups      = ["${var.nb-sg_id}"]
  iam_instance_profile = "${var.iam_profile_name}"
  tags = {
    Name                     = "${local.node_name}-m"
    Type                     = "master"
    dataengine_notebook_name = "${local.dataengine_notebook_name}"
    "${var.project_tag}-Tag" = "${local.node_name}-m"
    User_tag                 = "${var.user_tag}"
    Endpoint_Tag             = "${var.endpoint_tag}"
    "user:tag"               = "${var.project_tag}:${local.node_name}"
    Custom_Tag               = "${var.custom_tag}"
  }
}


resource "aws_instance" "slave" {
  count = "${var.slave_count}"
  ami                  = "${var.ami}"
  instance_type        = "${var.instance_type}"
  subnet_id            = "${var.aws_subnet_id}"
  security_groups      = ["${var.nb-sg_id}"]
  iam_instance_profile = "${var.iam_profile_name}"
  tags = {
    Name                     = "${local.node_name}-s${count.index + 1}"
    Type                     = "slave"
    dataengine_notebook_name = "${local.dataengine_notebook_name}"
    "${var.project_tag}-Tag" = "${local.node_name}-s${count.index + 1}"
    User_tag                 = "${var.user_tag}"
    Endpoint_Tag             = "${var.endpoint_tag}"
    "user:tag"               = "${var.project_tag}:${local.node_name}"
    Custom_Tag               = "${var.custom_tag}"
  }
}