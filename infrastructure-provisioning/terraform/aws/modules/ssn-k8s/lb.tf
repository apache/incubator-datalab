resource "aws_lb" "k8s-lb" {
  name               = "${var.service_base_name}-lb"
  load_balancer_type = "network"

  subnet_mapping {
    subnet_id     = data.aws_subnet.k8s-subnet-data.id
    allocation_id = aws_eip.k8s-lb-eip.id
  }
  tags = {
    Name = "${var.service_base_name}-lb"
  }
}

resource "aws_lb_target_group" "k8s-lb-target-group" {
  name     = "${var.service_base_name}-lb-target-group"
  port     = 6443
  protocol = "TCP"
  vpc_id   = data.aws_vpc.k8s-vpc-data.id
  tags = {
    Name = "${var.service_base_name}-lb-target-group"
  }
}

resource "aws_lb_listener" "k8s-lb-listener" {
  load_balancer_arn = aws_lb.k8s-lb.arn
  port              = "6443"
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.k8s-lb-target-group.arn
  }
}