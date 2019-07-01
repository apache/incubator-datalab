data "template_file" "k8s-masters-user-data" {
  template = file("../modules/ssn-k8s/files/masters-user-data.sh")
  vars = {
    k8s-asg = "${var.service_base_name}-master"
    k8s-region = var.region
    k8s-bucket-name = aws_s3_bucket.k8s-bucket.id
    k8s-eip = aws_eip.k8s-lb-eip.public_ip
    k8s-tg-arn = aws_lb_target_group.k8s-lb-target-group.arn
    k8s-os-user = var.os-user
  }
}

data "template_file" "k8s-workers-user-data" {
  template = file("../modules/ssn-k8s/files/workers-user-data.sh")
  vars = {
    k8s-bucket-name = aws_s3_bucket.k8s-bucket.id
    k8s-os-user = var.os-user
  }
}

resource "aws_launch_configuration" "as_conf_masters" {
  name                 = "${var.service_base_name}-as-conf-masters"
  image_id             = var.ami[var.env_os]
  instance_type        = var.masters_shape
  key_name             = var.key_name
  security_groups      = [aws_security_group.k8s-sg.id]
  iam_instance_profile = aws_iam_instance_profile.k8s-profile.name
  root_block_device {
    volume_type           = "gp2"
    volume_size           = var.root_volume_size
    delete_on_termination = true
  }

  lifecycle {
    create_before_destroy = true
  }
  user_data = data.template_file.k8s-masters-user-data.rendered
}

resource "aws_launch_configuration" "as_conf_workers" {
  name                 = "${var.service_base_name}-as-conf-workers"
  image_id             = var.ami[var.env_os]
  instance_type        = var.workers_shape
  key_name             = var.key_name
  security_groups      = [aws_security_group.k8s-sg.id]
  iam_instance_profile = aws_iam_instance_profile.k8s-profile.name
  root_block_device {
    volume_type           = "gp2"
    volume_size           = var.root_volume_size
    delete_on_termination = true
  }

  lifecycle {
    create_before_destroy = true
  }
  user_data = data.template_file.k8s-workers-user-data.rendered
}

resource "aws_autoscaling_group" "autoscaling_group_masters" {
  name                 = "${var.service_base_name}-master"
  launch_configuration = aws_launch_configuration.as_conf_masters.name
  min_size             = var.masters_count
  max_size             = var.masters_count
  vpc_zone_identifier  = [data.aws_subnet.k8s-subnet-data.id]
  target_group_arns    = [aws_lb_target_group.k8s-lb-target-group.arn]

  lifecycle {
    create_before_destroy = true
  }
  tags = [
    {
      key                 = "Name"
      value               = "${var.service_base_name}-master"
      propagate_at_launch = true
    }
  ]
}

resource "aws_autoscaling_group" "autoscaling_group_workers" {
  name                 = "${var.service_base_name}-worker"
  launch_configuration = aws_launch_configuration.as_conf_workers.name
  min_size             = var.workers_count
  max_size             = var.workers_count
  vpc_zone_identifier  = [data.aws_subnet.k8s-subnet-data.id]

  lifecycle {
    create_before_destroy = true
  }
  tags = [
    {
      key                 = "Name"
      value               = "${var.service_base_name}-worker"
      propagate_at_launch = true
    }
  ]
}