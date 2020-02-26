{
  "Version": "2012-10-17",
  "Statement": [
    {
        "Action": [
            "s3:CreateBucket",
            "s3:ListAllMyBuckets",
            "s3:GetBucketLocation",
            "s3:GetBucketTagging",
            "s3:PutBucketTagging",
            "s3:PutBucketPolicy",
            "s3:GetBucketPolicy",
            "s3:DeleteBucket",
            "s3:DeleteObject",
            "s3:GetObject",
            "s3:ListBucket",
            "s3:PutObject",
            "s3:PutEncryptionConfiguration"
        ],
        "Effect": "Allow",
        "Resource": "*"
    },
    {
        "Effect": "Allow",
        "Action": [
            "autoscaling:DescribeAutoScalingInstances",
            "ec2:DescribeInstances",
            "elasticloadbalancing:DescribeTargetHealth",
            "elasticloadbalancing:*",
            "ec2:*"
        ],
        "Resource": "*"
    },
    {
        "Action": [
            "pricing:GetProducts"
        ],
        "Effect": "Allow",
        "Resource": "*"
    }
  ]
}