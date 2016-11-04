#!/usr/bin/python

# ******************************************************************************************************
#
# Copyright (c) 2016 EPAM Systems Inc.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including # without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject # to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. # IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH # # THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# ****************************************************************************************************/

from dlab.aws_meta import *
from dlab.aws_actions import *
import boto3
import argparse
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--emr_name', type=str)
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
                s3_cleanup(args.bucket_name, emr_name)
                print "The bucket " + args.bucket_name + " has been cleaned successfully"
                terminate_emr(cluster_id)
                print "The EMR cluster " + emr_name + " has been terminated successfully"
        else:
            print "There are no EMR clusters to terminate."
    except:
        sys.exit(1)

    print "Removing EMR kernels from notebook"
    try:
        remove_kernels(args.emr_name, args.tag_name, args.nb_tag_value, args.ssh_user, args.key_path)
    except:
        sys.exit(1)
