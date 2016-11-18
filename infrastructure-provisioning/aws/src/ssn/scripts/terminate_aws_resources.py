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

import boto3
import os
import argparse
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--resource_name', type=str)
parser.add_argument('--ssn_tag_value_name', type=str)
parser.add_argument('--notebook_tag_value', type=str)
parser.add_argument('--notebook_name', type=str)
parser.add_argument('--instance_type', type=str)
parser.add_argument('--emr_name', type=str)
args = parser.parse_args()


# Function for terminating IAM roles
def remove_role(notebook_name, instance_type):
    print "========= Role =========="
    client = boto3.client('iam')
    if instance_type == "ssn":
        role_name = os.environ['conf_service_base_name'] + '-role'
        role_profile_name = os.environ['conf_service_base_name'] + '-role-profile'
    elif instance_type == "notebook":
        role_name = os.environ['conf_service_base_name'] + '-' + "{}".format(notebook_name) + '-role'
        role_profile_name = os.environ['conf_service_base_name'] + '-' + "{}".format(notebook_name) + '-role-profile'
    try:
        role = client.get_role(RoleName="{}".format(role_name)).get("Role").get("RoleName")
    except:
        print "Wasn't able to get role!"
    print "Name: ", role
    policy_list = client.list_attached_role_policies(RoleName=role).get('AttachedPolicies')
    for i in policy_list:
        policy_arn = i.get('PolicyArn')
        print policy_arn
        client.detach_role_policy(RoleName=role, PolicyArn=policy_arn)
    print "===== Role_profile ======"
    try:
        profile = client.get_instance_profile(InstanceProfileName="{}".format(role_profile_name)).get(
            "InstanceProfile").get("InstanceProfileName")
    except:
        print "Wasn't able to get instance profile!"
    print "Name: ", profile
    try:
        client.remove_role_from_instance_profile(InstanceProfileName=profile, RoleName=role)
    except:
        print "\nWasn't able to remove role from instance profile!"
    try:
        client.delete_instance_profile(InstanceProfileName=profile)
    except:
        print "\nWasn't able to remove instance profile!"
    try:
        client.delete_role(RoleName=role)
    except:
        print "\nWasn't able to remove role!"
    print "The IAM role " + role + " has been deleted successfully"


# Function for terminating any EC2 instances inc notebook servers
def remove_ec2(ssn_tag_value, notebook_tag_value):
    print "========== EC2 =========="
    ec2 = boto3.resource('ec2')
    client = boto3.client('ec2')
    ssn_tag_name = os.environ['conf_service_base_name'] + '-tag'
    notebook_tag_name = os.environ['conf_service_base_name']
    ssn_instances = ec2.instances.filter(
        Filters=[{'Name': 'instance-state-name', 'Values': ['running', 'stopped']},
                 {'Name': 'tag:{}'.format(ssn_tag_name), 'Values': ['{}'.format(ssn_tag_value)]}])
    for instance in ssn_instances:
        print("ID: ", instance.id)
        client.terminate_instances(InstanceIds=[instance.id])
        waiter = client.get_waiter('instance_terminated')
        waiter.wait(InstanceIds=[instance.id])
        remove_role("", "ssn")
        print "The ssn instance " + instance.id + " has been deleted successfully"
    notebook_instances = ec2.instances.filter(
        Filters=[{'Name': 'instance-state-name', 'Values': ['running', 'stopped']},
                 {'Name': 'tag:{}'.format(notebook_tag_name), 'Values': ['{}'.format(notebook_tag_value)]}])
    for instance in notebook_instances:
        print("ID: ", instance.id)
        for i in instance.tags:
            if i.get("Key") == "Name":
                notebook_name = i.get("Value")
        client.terminate_instances(InstanceIds=[instance.id])
        waiter = client.get_waiter('instance_terminated')
        waiter.wait(InstanceIds=[instance.id])
        remove_role(notebook_name, "notebook")
        print "The notebook instance " + instance.id + " has been deleted successfully"


# Function for terminating security groups
def remove_sgroups():
    print "========== SG ==========="
    ec2 = boto3.resource('ec2')
    client = boto3.client('ec2')
    tag_name = os.environ['conf_service_base_name'] + '-tag'
    sgs = ec2.security_groups.filter(
        Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': ['*']}])
    for sg in sgs:
        print sg.id
        client.delete_security_group(GroupId=sg.id)
        print "The security group " + sg.id + " has been deleted successfully"


# Function for terminating subnets
def remove_subnets():
    print "======== Subnet ========="
    ec2 = boto3.resource('ec2')
    client = boto3.client('ec2')
    tag_name = os.environ['conf_service_base_name'] + '-tag'
    subnets = ec2.subnets.filter(
        Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': ['*']}])
    for subnet in subnets:
        print subnet.id
        client.delete_subnet(SubnetId=subnet.id)
        print "The subnet " + subnet.id + " has been deleted successfully"


# Function for terminating VPC
def remove_vpc():
    print "========== VPC =========="
    ec2 = boto3.resource('ec2')
    client = boto3.client('ec2')
    tag_name = os.environ['conf_service_base_name'] + '-tag'
    vpcs = ec2.vpcs.filter(
        Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': ['*']}])
    for vpc in vpcs:
        print vpc.id
        client.delete_vpc(VpcId=vpc.id)
        print "The VPC " + vpc.id + " has been deleted successfully"


# Function for terminating S3 buckets
def remove_s3():
    print "========== S3 ==========="
    s3 = boto3.resource('s3')
    client = boto3.client('s3')
    bucket_name = (os.environ['conf_service_base_name'] + '-bucket').lower().replace('_', '-')
    bucket = s3.Bucket("{}".format(bucket_name))
    print bucket.name
    try:
        list_obj = client.list_objects(Bucket=bucket.name)
    except:
        print "Wasn't able to get S3!"
    try:
        list_obj = list_obj.get('Contents')
    except:
        print "Wasn't able to get S3!"
    if list_obj is not None:
        for o in list_obj:
            list_obj = o.get('Key')
            print list_obj
            client.delete_objects(
                Bucket=bucket_name,
                Delete={'Objects': [{'Key': list_obj}]}
            )
            print "The S3 bucket " + bucket.name + " has been cleaned"
    try:
        client.delete_bucket(Bucket=bucket.name)
    except:
        print "Wasn't able to remove S3!"
    print "The S3 bucket " + bucket.name + " has been deleted successfully"


# Function for terminating EMR clusters, cleaning buckets and removing notebook's local kernels
def remove_emr(emr_name):
    print "========= EMR =========="
    client = boto3.client('emr')
    clusters = client.list_clusters(ClusterStates=['STARTING', 'BOOTSTRAPPING', 'RUNNING', 'WAITING'])
    clusters = clusters.get("Clusters")
    for c in clusters:
        if c.get('Name') == "{}".format(emr_name):
            cluster_id = c.get('Id')
            cluster_name = c.get('Name')
            print cluster_id
            client.terminate_job_flows(JobFlowIds=[cluster_id])
            print "The EMR cluster " + cluster_name + " has been deleted successfully"


# remove all function
def remove_all():
    remove_emr(args.emr_name)
    remove_ec2(args.ssn_tag_value, args.notebook_tag_value)
    remove_sgroups()
    remove_subnets()
    remove_s3()
    remove_vpc()


# Switch-case function
def run_case(resource_name):
    switcher = {
        "EC2": remove_ec2(args.ssn_tag_value, args.notebook_tag_value),
        "SG": remove_sgroups(),
        "SUBNET": remove_subnets(),
        "VPC": remove_vpc(),
        "S3": remove_s3(),
        "ROLE": remove_role(args.notebook_name, args.instance_type),
        "EMR": remove_emr(args.emr_name),
        "all": remove_all()
    }
    return switcher.get(resource_name, "\nPlease type correct resource name to delete")


##############
# Run script #
##############

if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        run_case(args.resource_name)
