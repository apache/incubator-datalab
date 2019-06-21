locals {
  node_name = "${var.project_tag}-nb-${var.notebook_name}"
}

resource "aws_instance" "notebook" {
  ami                  = "${var.ami}"
  instance_type        = "${var.instance_type}"
  subnet_id            = "${var.aws_subnet_id}"
  security_groups      = ["${var.nb-sg_id}"]
  iam_instance_profile = "${var.iam_profile_name}"
  tags = {
    Name                     = "${local.node_name}"
    "${var.project_tag}-Tag" = "${local.node_name}"
    Endpoint_Tag             = "${var.endpoint_tag}"
    "user:tag"               = "${var.project_tag}:${local.node_name}"
    User_Tag                 = "${var.user_tag}"
    Custom_Tag               = "${var.custom_tag}"
  }
}