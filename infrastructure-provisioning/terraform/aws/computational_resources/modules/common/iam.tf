# *****************************************************************************
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# ******************************************************************************

locals {
  role_name    = "${var.sbn}-nb-de-Role"
  role_profile = "${var.sbn}-nb-Profile"
  policy_name  = "${var.sbn}-strict_to_S3-Policy"
}

resource "aws_iam_role" "nb_de_role" {
  name               = local.role_name
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
    Name             = local.role_name
    Environment_tag  = var.sbn
    "${var.sbn}-tag" = local.role_name
    Product          = var.product
    Project_name     = var.project_name
    Project_tag      = var.project_tag
    Endpoint_tag     = var.endpoint_tag
    "user:tag"       = "${var.sbn}:${local.role_name}"
    User_tag         = var.user_tag
    Custom_tag       = var.custom_tag
  }
}

resource "aws_iam_instance_profile" "nb_profile" {
  name = local.role_profile
  role = aws_iam_role.nb_de_role.name
}

resource "aws_iam_policy" "strict_S3_policy" {
  name = local.policy_name
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
                "arn:aws:s3:::${var.sbn}*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:HeadObject"
            ],
            "Resource": "arn:aws:s3:::${var.sbn}-ssn-bucket/*"
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
                "arn:aws:s3:::${var.sbn}-bucket/*",
                "arn:aws:s3:::${var.sbn}-shared-bucket/*"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "strict_S3_policy-attach" {
  role       = aws_iam_role.nb_de_role.name
  policy_arn = aws_iam_policy.strict_S3_policy.arn
}