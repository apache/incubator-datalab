locals {
  role_name    = "${var.project_tag}-nb-de-Role"
  role_profile = "${var.project_tag}-nb-Profile"
  policy_name  = "${var.project_tag}-strict_to_S3-Policy"
}

resource "aws_iam_role" "nb_de_role" {
  name               = "${local.role_name}"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF

  tags = {
    product = "${var.product}"
    Name = "${local.role_name}"
    environment_tag = "${var.project_tag}"
  }
}

resource "aws_iam_instance_profile" "nb_profile" {
  name = "${local.role_profile}"
  role = "${aws_iam_role.nb_de_role.name}"
}

resource "aws_iam_policy" "strict_S3_policy" {
  name = "${local.policy_name}"
  description = "Strict Bucket only policy"
  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "s3:ListAllMyBuckets",
            "Resource": "arn:aws:s3:::*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucket",
                "s3:GetBucketLocation",
                "s3:PutBucketPolicy",
                "s3:PutEncryptionConfiguration"
            ],
            "Resource": [
                "arn:aws:s3:::${var.project_tag}*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:HeadObject"
            ],
            "Resource": "arn:aws:s3:::${var.project_tag}-ssn-bucket/*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:HeadObject",
                "s3:PutObject",
                "s3:GetObject",
                "s3:DeleteObject"
            ],
            "Resource": [
                "arn:aws:s3:::${var.project_tag}-bucket/*",
                "arn:aws:s3:::${var.project_tag}-shared-bucket/*"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "strict_S3_policy-attach" {
  role       = "${aws_iam_role.nb_de_role.name}"
  policy_arn = "${aws_iam_policy.strict_S3_policy.arn}"
}