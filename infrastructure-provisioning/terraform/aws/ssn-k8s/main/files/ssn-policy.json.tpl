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