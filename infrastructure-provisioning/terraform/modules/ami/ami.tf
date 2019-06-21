resource "aws_ami_from_instance" "ami" {
  name               = "${var.project_tag}-${var.notebook_name}-ami"
  source_instance_id = "${var.source_instance_id}"
}