data "template_file" "k8s-s3-policy" {
  template = file("../modules/ssn-k8s/files/ssn-policy.json.tpl")
  vars = {
    bucket_arn = aws_s3_bucket.k8s-bucket.arn
  }
}

resource "aws_iam_policy" "k8s-policy" {
  name        = "${var.service_base_name}-policy"
  description = "Policy for K8S"
  policy      = data.template_file.k8s-s3-policy.rendered
}

resource "aws_iam_role" "k8s-role" {
  name               = "${var.service_base_name}-role"
  assume_role_policy = file("../modules/ssn-k8s/files/assume-policy.json")
  tags = {
    Name = "${var.service_base_name}-role"
  }
}

resource "aws_iam_role_policy_attachment" "k8s-attach" {
  role       = aws_iam_role.k8s-role.name
  policy_arn = aws_iam_policy.k8s-policy.arn
}

resource "aws_iam_instance_profile" "k8s-profile" {
  name = "${var.service_base_name}-instance-profile"
  role = aws_iam_role.k8s-role.name
}