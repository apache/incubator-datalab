data "aws_eip" "k8s-lb-eip" {
  id = aws_eip.k8s-lb-eip.id
  depends_on = [aws_lb_listener.k8s-lb-listener]
}

resource "aws_security_group" "k8s-sg" {
  name        = "${var.service_base_name}-sg"
  description = "SG for K8S cluster"
  vpc_id      = data.aws_vpc.k8s-vpc-data.id

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    cidr_blocks = [data.aws_vpc.k8s-vpc-data.cidr_block]
  }
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.allowed_cidrs
  }
  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    cidr_blocks = ["0.0.0.0/0"]
    description = "Need to be changed in the future"
  }
  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    cidr_blocks = ["${data.aws_eip.k8s-lb-eip.public_ip}/32", "${data.aws_eip.k8s-lb-eip.private_ip}/32"]
  }

  egress {
    from_port   = 0
    protocol    = -1
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.service_base_name}-sg"
  }
}