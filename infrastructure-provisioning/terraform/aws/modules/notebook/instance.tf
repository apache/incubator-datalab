locals {
  node_name = "${var.project_tag}-nb-${var.notebook_name}"
}

resource "aws_instance" "notebook" {
  ami                  = "${var.note_ami}"
  instance_type        = "${var.instance_type}"
  key_name             = "${var.key_name}"
  subnet_id            = "${var.subnet_id}"
  security_groups      = ["${var.nb-sg_id}"]
  iam_instance_profile = "${var.note_profile_name}"
  tags = {
    Name                     = "${local.node_name}"
    "${var.project_tag}-Tag" = "${local.node_name}"
    Endpoint_Tag             = "${var.endpoint_tag}"
    "user:tag"               = "${var.project_tag}:${local.node_name}"
    product                  = "${var.product}"
    User_Tag                 = "${var.user_tag}"
    Custom_Tag               = "${var.custom_tag}"
  }
}