# Local vars for EC2-endpoint
locals {
  ec2_name = "${var.service_base_name}-endpoint"
  eip_name = "${var.service_base_name}-endpoint-EIP"
}


resource "aws_instance" "endpoint" {
  ami             = "${var.ami[var.env_os]}"
  instance_type   = var.endpoint_instance_shape
  key_name        = var.key_name
  subnet_id       = data.aws_subnet.data_subnet.id
  security_groups = ["${aws_security_group.endpoint_sec_group.id}"]
  tags = {
    Name = "${local.ec2_name}"
    "${var.service_base_name}-Tag" = "${local.ec2_name}"
    product = "${var.product}"
    "user:tag" = "${var.service_base_name}:${local.ec2_name}"
  }

}

resource "aws_eip" "e_ip" {
  instance = aws_instance.endpoint.id
  vpc      = true
  tags = {
    Name = "${local.eip_name}"
    "${var.service_base_name}-Tag" = "${local.eip_name}"
    product = "${var.product}"
    "user:tag" = "${var.service_base_name}:${local.eip_name}"
  }
  count = var.network_type == "public" ? 1 : 0
}

resource "aws_eip_association" "e_ip_assoc" {
  instance_id   = aws_instance.endpoint.id
  allocation_id = aws_eip.e_ip.0.id
  count         = var.network_type == "public" ? 1 : 0
}
