#!/usr/bin/python3

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

import argparse
import boto3
import datalab.ssn_lib
from datalab.logger import logging
import os
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--tag_name', type=str)
parser.add_argument('--nb_sg', type=str)
parser.add_argument('--edge_sg', type=str)
parser.add_argument('--de_sg', type=str)
parser.add_argument('--service_base_name', type=str)
parser.add_argument('--de_se_sg', type=str)
args = parser.parse_args()
tag2 = args.service_base_name + '-secondary-tag'

##############
# Run script #
##############

if __name__ == "__main__":
    logging.info('Terminating EMR cluster')
    try:
        clusters_list = datalab.meta_lib.get_emr_list(args.tag_name)
        if clusters_list:
            for cluster_id in clusters_list:
                client = boto3.client('emr')
                cluster = client.describe_cluster(ClusterId=cluster_id)
                cluster = cluster.get("Cluster")
                emr_name = cluster.get('Name')
                datalab.actions_lib.terminate_emr(cluster_id)
                logging.info("The EMR cluster {} has been terminated successfully".format(emr_name))
        else:
            logging.info("There are no EMR clusters to terminate.")
    except Exception as err:
        datalab.fab.append_result("Failed to terminate EMR cluster.", str(err))
        sys.exit(1)

    logging.info("Deregistering notebook's AMI")
    try:
        datalab.actions_lib.deregister_image()
    except Exception as err:
        datalab.fab.append_result("Failed to deregister images.", str(err))
        sys.exit(1)

    logging.info("Terminating EC2 instances")
    try:
        datalab.actions_lib.remove_ec2(args.tag_name, '*')
    except Exception as err:
        datalab.fab.append_result("Failed to terminate instances.", str(err))
        sys.exit(1)

    if 'ssn_hosted_zone_id' in os.environ and 'ssn_hosted_zone_name' in os.environ and 'ssn_subdomain' in os.environ:
        logging.info("Removing Route53 records")
        datalab.actions_lib.remove_route_53_record(os.environ['ssn_hosted_zone_id'], os.environ['ssn_hosted_zone_name'],
                                                   os.environ['ssn_subdomain'])

    logging.info("Removing security groups")
    try:
        try:
            datalab.actions_lib.remove_sgroups(args.de_se_sg)
            datalab.actions_lib.remove_sgroups(args.de_sg)
            datalab.actions_lib.remove_sgroups(args.nb_sg)
            datalab.actions_lib.remove_sgroups(args.edge_sg)
        except:
            logging.info("There are no SG for compute resources")
        try:
            datalab.actions_lib.remove_sgroups(args.tag_name)
        except:
            logging.info("There is SSN SG")
    except Exception as err:
        datalab.fab.append_result("Failed to remove security groups.", str(err))
        sys.exit(1)

    logging.info("Removing private subnet")
    try:
        datalab.actions_lib.remove_subnets('*')
    except Exception as err:
        datalab.fab.append_result("Failed to remove subnets.", str(err))
        sys.exit(1)

    logging.info("Removing peering connection")
    try:
        datalab.actions_lib.remove_peering('*')
    except Exception as err:
        datalab.fab.append_result("Failed to remove peering connections.", str(err))
        sys.exit(1)

    logging.info("Removing s3 buckets")
    try:
        datalab.actions_lib.remove_s3()
    except Exception as err:
        datalab.fab.append_result("Failed to remove buckets.", str(err))
        sys.exit(1)

    logging.info("Removing IAM roles, profiles and policies")
    try:
        datalab.actions_lib.remove_all_iam_resources('all')
    except Exception as err:
        datalab.fab.append_result("Failed to remove IAM roles, profiles and policies.", str(err))
        sys.exit(1)

    logging.info("Removing route tables")
    try:
        datalab.actions_lib.remove_route_tables(args.tag_name)
        datalab.actions_lib.remove_route_tables(tag2)
    except Exception as err:
        datalab.fab.append_result("Failed to remove route tables.", str(err))
        sys.exit(1)

    logging.info("Removing SSN subnet")
    try:
        datalab.actions_lib.remove_subnets(args.service_base_name + '-subnet')
    except Exception as err:
        datalab.fab.append_result("Failed to remove SSN subnet.", str(err))
        sys.exit(1)

    logging.info("Removing SSN VPC")
    try:
        vpc_id = datalab.meta_lib.get_vpc_by_tag(args.tag_name, args.service_base_name)
        if vpc_id != '':
            try:
                datalab.actions_lib.remove_vpc_endpoints(vpc_id)
            except:
                logging.info("There is no such VPC Endpoint")
            try:
                datalab.actions_lib.remove_internet_gateways(vpc_id, args.tag_name, args.service_base_name)
            except:
                logging.info("There is no such Internet gateway")
            datalab.actions_lib.remove_route_tables(args.tag_name, True)
            datalab.actions_lib.remove_vpc(vpc_id)
        else:
            logging.info("There is no pre-defined SSN VPC")
    except Exception as err:
        datalab.fab.append_result("Failed to remove SSN VPC.", str(err))
        sys.exit(1)

    logging.info("Removing notebook VPC")
    try:
        vpc_id = datalab.meta_lib.get_vpc_by_tag(tag2, args.service_base_name)
        if vpc_id != '':
            try:
                datalab.actions_lib.remove_vpc_endpoints(vpc_id)
            except:
                logging.info("There is no such VPC Endpoint")
            datalab.actions_lib.remove_route_tables(tag2, True)
            datalab.actions_lib.remove_vpc(vpc_id)
        else:
            logging.info("There is no pre-defined notebook VPC")
    except Exception as err:
        datalab.fab.append_result("Failed to remove wecondary VPC.", str(err))
        sys.exit(1)
