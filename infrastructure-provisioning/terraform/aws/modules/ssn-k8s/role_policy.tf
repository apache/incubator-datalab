data "template_file" "ssn_k8s_s3_policy" {
  template = file("../modules/ssn-k8s/files/ssn-policy.json.tpl")
  vars = {
    bucket_arn = aws_s3_bucket.ssn_k8s_bucket.arn
  }
}

resource "aws_iam_policy" "ssn_k8s_policy" {
  name        = "${var.service_base_name}-ssn-policy"
  description = "Policy for SSN K8S"
  policy      = data.template_file.ssn_k8s_s3_policy.rendered
}

resource "aws_iam_role" "ssn_k8s_role" {
  name               = "${var.service_base_name}-ssn-role"
  assume_role_policy = file("../modules/ssn-k8s/files/assume-policy.json")
  tags = {
    Name = "${var.service_base_name}-ssn-role"
  }
}

resource "aws_iam_role_policy_attachment" "ssn_k8s_policy_attachment" {
  role       = aws_iam_role.ssn_k8s_role.name
  policy_arn = aws_iam_policy.ssn_k8s_policy.arn
}

resource "aws_iam_instance_profile" "k8s-profile" {
  name = "${var.service_base_name}-instance-profile"
  role = aws_iam_role.ssn_k8s_role.name
}