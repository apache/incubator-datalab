# Local vars for IAM-things
locals {
  role_name    = "${var.service_base_name}-endpoint-role"
  role_profile = "${var.service_base_name}-endpoint-profile"
  policy_name  = "${var.service_base_name}-endpoint-policy"
}

# IAM role
resource "aws_iam_role" "endpoint_role" {
  name               = local.role_name
  assume_role_policy = <<EOF
{
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Action": "sts:AssumeRole",
                    "Effect": "Allow",
                    "Principal": {
                        "Service": "ec2.amazonaws.com"
                    }
                }
            ]
        }
EOF
  tags = {
    product = "${var.product}"
    Name = "${local.role_name}"
    "${var.service_base_name}-Tag" = "${local.role_name}"
  }
}

# IAM profile for attaching to EC2-instance
resource "aws_iam_instance_profile" "endpoint_profile" {
  name = local.role_profile
  role = aws_iam_role.endpoint_role.name
}

# IAM policy
resource "aws_iam_policy" "endpoint_policy" {
  name   = local.policy_name
  policy = <<EOF
{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Action": [
				"iam:ListRoles",
				"iam:CreateRole",
				"iam:CreateInstanceProfile",
				"iam:PutRolePolicy",
				"iam:AddRoleToInstanceProfile",
				"iam:PassRole",
				"iam:GetInstanceProfile",
				"iam:ListInstanceProfilesForRole",
				"iam:RemoveRoleFromInstanceProfile",
				"iam:DeleteInstanceProfile"
			],
			"Effect": "Allow",
			"Resource": "*"
		},
		{
			"Action": [
				"ec2:DescribeImages",
				"ec2:CreateTags",
				"ec2:DescribeRouteTables",
				"ec2:CreateRouteTable",
				"ec2:AssociateRouteTable",
				"ec2:DescribeVpcEndpoints",
				"ec2:CreateVpcEndpoint",
				"ec2:ModifyVpcEndpoint",
				"ec2:DescribeInstances",
				"ec2:RunInstances",
				"ec2:DescribeAddresses",
				"ec2:AllocateAddress",
				"ec2:DescribeInstances",
				"ec2:AssociateAddress",
				"ec2:DisassociateAddress",
				"ec2:ReleaseAddress",
				"ec2:TerminateInstances"
			],
			"Effect": "Allow",
			"Resource": "*"
		},
		{
			"Action": [
				"s3:ListAllMyBuckets",
				"s3:CreateBucket",
				"s3:PutBucketTagging",
				"s3:GetBucketTagging"
			],
			"Effect": "Allow",
			"Resource": "*"
		}
	]
}
EOF
}

# Policy attachment to the Role
resource "aws_iam_role_policy_attachment" "endpoint_policy_attach" {
  role       = aws_iam_role.endpoint_role.name
  policy_arn = aws_iam_policy.endpoint_policy.arn
}
