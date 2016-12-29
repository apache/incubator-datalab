#!/usr/bin/python

# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ******************************************************************************

from dlab.aws_meta import *
from dlab.aws_actions import *
import boto3
import argparse
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--bucket_name', type=str)
parser.add_argument('--tag_name', type=str)
parser.add_argument('--nb_tag_value', type=str)
parser.add_argument('--ssh_user', type=str)
parser.add_argument('--key_path', type=str)
args = parser.parse_args()


##############
# Run script #
##############

if __name__ == "__main__":
    print 'Terminating EMR cluster and cleaning EMR config from S3 bucket'
    try:
        clusters_list = get_emr_list(args.nb_tag_value, 'Value')
        if clusters_list:
            for cluster_id in clusters_list:
                client = boto3.client('emr')
                cluster = client.describe_cluster(ClusterId=cluster_id)
                cluster = cluster.get("Cluster")
                emr_name = cluster.get('Name')
                emr_version = cluster.get('ReleaseLabel')
                s3_cleanup(args.bucket_name, emr_name, os.environ['notebook_user_name'])
                print "The bucket " + args.bucket_name + " has been cleaned successfully"
                terminate_emr(cluster_id)
                print "The EMR cluster " + emr_name + " has been terminated successfully"
                remove_kernels(emr_name, args.tag_name, args.nb_tag_value, args.ssh_user, args.key_path, emr_version)
                print emr_name + " kernels have been removed from notebook successfully"
        else:
            print "There are no EMR clusters to terminate."
    except:
        sys.exit(1)

    print "Stopping notebook"
    try:
        stop_ec2(args.tag_name, args.nb_tag_value)
    except:
        sys.exit(1)


