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

import boto3
from botocore.client import Config
import json, urllib2
import time
import logging
import traceback
import sys
import backoff
import random
import string
from dlab.fab import *
import actions_lib


def get_instance_hostname(tag_name, instance_name):
    try:
        public = ''
        private = ''
        ec2 = boto3.resource('ec2')
        instances = ec2.instances.filter(
            Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': [instance_name]},
                     {'Name': 'instance-state-name', 'Values': ['running']}])
        for instance in instances:
            public = getattr(instance, 'public_dns_name')
            private = getattr(instance, 'private_dns_name')
            if public:
                return public
            else:
                return private
        if public == '' and private == '':
            raise Exception("Unable to find instance hostname with instance name: " + instance_name)
    except Exception as err:
        logging.error("Error with finding instance hostname with instance name: " + instance_name + " : " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with finding instance hostname", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_vpc_endpoints(vpc_id):
    try:
        # Returns LIST of Endpoint DICTIONARIES
        ec2 = boto3.client('ec2')
        endpoints = ec2.describe_vpc_endpoints(
            Filters=[{
                'Name': 'vpc-id',
                'Values': [vpc_id]
            }]
        ).get('VpcEndpoints')
        return endpoints
    except Exception as err:
        logging.error("Error with getting VPC Endpoints: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with getting VPC Endpoints", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_route_tables(vpc, tags):
    try:
        ec2 = boto3.client('ec2')
        tag_name = json.loads(tags).get('Key')
        tag_value = json.loads(tags).get('Value')
        rts = []
        result = ec2.describe_route_tables(
            Filters=[
                {'Name': 'vpc-id', 'Values': [vpc]},
                {'Name': 'tag-key', 'Values': [tag_name]},
                {'Name': 'tag-value', 'Values': [tag_value]}
            ]
        ).get('RouteTables')
        for i in result:
            rts.append(i.get('RouteTableId'))
        return rts
    except Exception as err:
        logging.error("Error with getting Route tables: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with getting Route tables", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_bucket_by_name(bucket_name):
    try:
        s3 = boto3.resource('s3', config=Config(signature_version='s3v4'))
        for bucket in s3.buckets.all():
            if bucket.name == bucket_name:
                return bucket.name
        return ''
    except Exception as err:
        logging.error("Error with getting bucket by name: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with getting bucket by name", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_instance_ip_address(tag_name, instance_name):
    try:
        ec2 = boto3.resource('ec2')
        instances = ec2.instances.filter(
            Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': [instance_name]},
                     {'Name': 'instance-state-name', 'Values': ['running']}])
        ips = {}
        for instance in instances:
            public = getattr(instance, 'public_ip_address')
            private = getattr(instance, 'private_ip_address')
            ips = {'Public': public, 'Private': private}
        if ips == {}:
            raise Exception("Unable to find instance IP addresses with instance name: " + instance_name)
        return ips
    except Exception as err:
        logging.error("Error with getting ip address by name: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with getting ip address by name", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_instance_ip_address_by_id(instance_id):
    try:
        ec2 = boto3.resource('ec2')
        instances = ec2.instances.filter(
            Filters = [{'Name': 'instance-id', 'Values': [instance_id]},
                       {'Name': 'instance-state-name', 'Values': ['running']}])
        ips = {}
        for instance in instances:
            public = getattr(instance, 'public_ip_address')
            private = getattr(instance, 'private_ip_address')
            ips = {'Public': public, 'Private': private}
        if ips == {}:
            raise Exception("Unable to find instance IP addresses with instance id: " + instance_id)
        return ips
    except Exception as err:
        logging.error("Error with getting ip address by id: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with getting ip address by id", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_instance_private_ip_address(tag_name, instance_name):
    try:
        actions_lib.create_aws_config_files()
        return get_instance_ip_address(tag_name, instance_name).get('Private')
    except Exception as err:
        logging.error("Error with getting private ip address by name: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with getting private ip address by name", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


@backoff.on_predicate(backoff.fibo, max_tries=5)
def get_ami_id_by_name(ami_name, state="*"):
    ec2 = boto3.resource('ec2')
    try:
        for image in ec2.images.filter(Filters=[{'Name': 'name', 'Values': [ami_name]}, {'Name': 'state', 'Values': [state]}]):
            return image.id
    except Exception as err:
        logging.error("Error with getting AMI ID by name: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with getting AMI ID by name",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)
        return ''
    return ''

def get_ami_id_by_instance_name(instance_name):
    ec2 = boto3.resource('ec2')
    try:
        for instance in ec2.instances.filter(Filters=[{'Name': 'tag:{}'.format('Name'), 'Values': [instance_name]}]):
            return instance.image_id
    except Exception as err:
        logging.error("Error with getting AMI ID by instance name: " + str(
            err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with getting AMI ID by instance name",
                           "error_message": str(
                               err) + "\n Traceback: " + traceback.print_exc(
                               file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)
        return ''
    return ''

def get_security_group_by_name(security_group_name):
    try:
        ec2 = boto3.resource('ec2')
        for security_group in ec2.security_groups.filter(Filters=[{'Name': 'group-name', 'Values': [security_group_name]}]):
            return security_group.id
    except Exception as err:
        logging.error("Error with getting Security Group ID by name: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting Security Group ID by name",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)
        return ''
    return ''


def get_instance_attr(instance_id, attribute_name):
    try:
        ec2 = boto3.resource('ec2')
        instances = ec2.instances.filter(
            Filters=[{'Name': 'instance-id', 'Values': [instance_id]},
                     {'Name': 'instance-state-name', 'Values': ['running']}])
        for instance in instances:
            return getattr(instance, attribute_name)
        return ''
    except Exception as err:
        logging.error("Error with getting instance attribute: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting instance attribute",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_instance_by_name(tag_name, instance_name):
    try:
        ec2 = boto3.resource('ec2')
        instances = ec2.instances.filter(
            Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': [instance_name]},
                     {'Name': 'instance-state-name', 'Values': ['running','pending','stopping','stopped']}])
        for instance in instances:
            return instance.id
        return ''
    except Exception as err:
        logging.error("Error with getting instance ID by name: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting instance ID by name",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_role_by_name(role_name):
    try:
        iam = boto3.resource('iam')
        for role in iam.roles.all():
            if role.name == role_name:
                return role.name
        return ''
    except Exception as err:
        logging.error("Error with getting role by name: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting role by name",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_subnet_by_cidr(cidr, vpc_id=''):
    try:
        ec2 = boto3.resource('ec2')
        if vpc_id:
            for subnet in ec2.subnets.filter(Filters=[
                {'Name': 'cidrBlock', 'Values': [cidr]},
                {'Name': 'vpc-id', 'Values': [vpc_id]}
            ]):
                return subnet.id
        else:
            for subnet in ec2.subnets.filter(Filters=[
                {'Name': 'cidrBlock', 'Values': [cidr]}
            ]):
                return subnet.id
        return ''
    except Exception as err:
        logging.error("Error with getting Subnet ID by CIDR: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting Subnet ID by CIDR",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_subnet_by_tag(tag, subnet_id=False, vpc_id=''):
    try:
        ec2 = boto3.resource('ec2')
        if vpc_id:
            for subnet in ec2.subnets.filter(Filters=[
                {'Name': 'tag-key', 'Values': [tag.get('Key')]},
                {'Name': 'tag-value', 'Values': [tag.get('Value')]},
                {'Name': 'vpc-id', 'Values': [vpc_id]}
            ]):
                if subnet_id:
                    return subnet.id
                else:
                    return subnet.cidr_block
        else:
            for subnet in ec2.subnets.filter(Filters=[
                {'Name': 'tag-key', 'Values': [tag.get('Key')]},
                {'Name': 'tag-value', 'Values': [tag.get('Value')]}
            ]):
                if subnet_id:
                    return subnet.id
                else:
                    return subnet.cidr_block
        return ''
    except Exception as err:
        logging.error("Error with getting Subnet CIDR block by tag: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting Subnet CIDR block by tag",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_vpc_by_cidr(cidr):
    try:
        ec2 = boto3.resource('ec2')
        for vpc in ec2.vpcs.filter(Filters=[{'Name': 'cidr', 'Values': [cidr]}]):
            return vpc.id
        return ''
    except Exception as err:
        logging.error("Error with getting VPC ID by CIDR: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting VPC ID by CIDR",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)

def get_cidr_by_vpc(vpc_id):
    try:
        client = boto3.client('ec2')
        cidr = client.describe_vpcs(VpcIds=[vpc_id]).get('Vpcs')[0].get('CidrBlock')
        return cidr
    except Exception as err:
        logging.error("Error with getting VPC CidrBlock by id: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting VPC CidrBlock by id",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)

def get_vpc_by_tag(tag_name, tag_value):
    try:
        ec2 = boto3.resource('ec2')
        for vpc in ec2.vpcs.filter(Filters=[{'Name': 'tag-key', 'Values': [tag_name]}, {'Name': 'tag-value', 'Values': [tag_value]}]):
            return vpc.id
        return ''
    except Exception as err:
        logging.error("Error with getting VPC ID by tag: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting VPC ID by tag",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)

def get_peering_by_tag(tag_name, tag_value):
    try:
        client = boto3.client('ec2')
        peering_id = client.describe_vpc_peering_connections(Filters=[{'Name': 'tag-key', 'Values': [tag_name]}, {'Name': 'tag-value', 'Values': [tag_value]},
                                                                   {'Name': 'status-code', 'Values': ['active']}]).get('VpcPeeringConnections')[0].get('VpcPeeringConnectionId')
        return peering_id
    except Exception as err:
        logging.error("Error with getting peering connection ID by tag: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting peering connection ID by tag",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)

def get_vpc_cidr_by_id(vpc_id):
    try:
        cidr_list = list()
        ec2 = boto3.client('ec2')
        for vpc in ec2.describe_vpcs(VpcIds=[vpc_id]).get('Vpcs'):
            for cidr_set in vpc.get('CidrBlockAssociationSet'):
                cidr_list.append(cidr_set.get('CidrBlock'))
            return cidr_list
        return ''
    except Exception as err:
        logging.error("Error with getting VPC CIDR by ID: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting VPC CIDR by ID",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_emr_info(id, key=''):
    try:
        emr = boto3.client('emr')
        info = emr.describe_cluster(ClusterId=id)['Cluster']
        if key:
            try:
                result = info[key]
            except:
                print("Cluster has no {} attribute".format(key))
                result = info
        else:
            result = info
        return result
    except Exception as err:
        logging.error("Error with getting EMR information: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting EMR information",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_emr_list(tag_name, type='Key', emr_count=False, emr_active=False):
    try:
        emr = boto3.client('emr')
        if emr_count:
            clusters = emr.list_clusters(
                ClusterStates=['RUNNING', 'WAITING', 'STARTING', 'BOOTSTRAPPING', 'TERMINATING']
            )
        else:
            clusters = emr.list_clusters(
                ClusterStates=['RUNNING', 'WAITING', 'STARTING', 'BOOTSTRAPPING']
            )
        if emr_active:
            clusters = emr.list_clusters(
                ClusterStates=['RUNNING', 'STARTING', 'BOOTSTRAPPING', 'TERMINATING']
            )
        clusters = clusters.get('Clusters')
        clusters_list = []
        for i in clusters:
            response = emr.describe_cluster(ClusterId=i.get('Id'))
            time.sleep(5)
            tag = response.get('Cluster').get('Tags')
            for j in tag:
                if tag_name in j.get(type):
                    clusters_list.append(i.get('Id'))
        return clusters_list
    except Exception as err:
        logging.error("Error with getting EMR list: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting EMR list",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_not_configured_emr_list(tag_name, instance_name):
    try:
        emr = boto3.client('emr')
        clusters = emr.list_clusters(ClusterStates=['WAITING'])
        clusters = clusters.get('Clusters')
        clusters_list = []
        for i in clusters:
            tags_found = 0
            response = emr.describe_cluster(ClusterId=i.get('Id'))
            time.sleep(5)
            tag = response.get('Cluster').get('Tags')
            for j in tag:
                if tag_name in j.get('Key'):
                    tags_found += 1
                if instance_name in j.get('Value'):
                    tags_found += 1
            if tags_found >= 2:
                clusters_list.append(i.get('Id'))
        return clusters_list
    except Exception as err:
        logging.error("Error with getting not configured EMR list: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting not configured EMR list",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_not_configured_emr(tag_name, instance_name, return_name=False):
    try:
        emr = boto3.client('emr')
        clusters_list = get_not_configured_emr_list(tag_name, instance_name)
        if clusters_list:
            for cluster_id in clusters_list:
                response = emr.describe_cluster(ClusterId=cluster_id)
                time.sleep(5)
                tag = response.get('Cluster').get('Tags')
                for j in tag:
                    if j.get('Value') == 'not-configured':
                        if return_name:
                            return response.get('Cluster').get('Name')
                        else:
                            return True
            return False
        else:
            return False
    except Exception as err:
        logging.error("Error with getting not configured EMR list: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting not configured EMR list",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_emr_id_by_name(name):
    try:
        cluster_id = ''
        emr = boto3.client('emr')
        clusters = emr.list_clusters(
            ClusterStates=['RUNNING', 'WAITING', 'STARTING', 'BOOTSTRAPPING']
        )
        clusters = clusters.get('Clusters')
        for i in clusters:
            response = emr.describe_cluster(ClusterId=i.get('Id'))
            time.sleep(5)
            if response.get('Cluster').get('Name') == name:
                cluster_id = i.get('Id')
        if cluster_id == '':
            raise Exception("Unable to find EMR cluster by name: " + name)
        return cluster_id
    except Exception as err:
        logging.error("Error with getting EMR ID by name: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting EMR ID by name",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_emr_instances_list(cluster_id, instance_type=''):
    #instance_type 'MASTER' or 'CORE'
    try:
        emr = boto3.client('emr')
        if instance_type != '':
            instances = emr.list_instances(ClusterId=cluster_id, InstanceGroupTypes=[instance_type])
        else:
            instances = emr.list_instances(ClusterId=cluster_id)
        return instances.get('Instances')
    except Exception as err:
        logging.error("Error with getting EMR instances list: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting EMR instances list",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_ec2_list(tag_name, value=''):
    try:
        ec2 = boto3.resource('ec2')
        if value:
            notebook_instances = ec2.instances.filter(
                Filters=[{'Name': 'instance-state-name', 'Values': ['running', 'stopped']},
                         {'Name': 'tag:{}'.format(tag_name), 'Values': ['{}*'.format(value)]}])
        else:
            notebook_instances = ec2.instances.filter(
                Filters=[{'Name': 'instance-state-name', 'Values': ['running', 'stopped']},
                         {'Name': 'tag:{}'.format(tag_name), 'Values': ['*nb*']}])
        return notebook_instances
    except Exception as err:
        logging.error("Error with getting EC2 list: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting EC2 list",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def provide_index(resource_type, tag_name, tag_value=''):
    try:
        ids = []
        if resource_type == 'EMR':
            if tag_value:
                list = get_emr_list(tag_value, 'Value', True)
            else:
                list = get_emr_list(tag_name, 'Key', True)
            emr = boto3.client('emr')
            for i in list:
                response = emr.describe_cluster(ClusterId=i)
                time.sleep(5)
                number = response.get('Cluster').get('Name').split('-')[-1]
                if number not in ids:
                    ids.append(int(number))
        elif resource_type == 'EC2':
            if tag_value:
                list = get_ec2_list(tag_name, tag_value)
            else:
                list = get_ec2_list(tag_name)
            for i in list:
                for tag in i.tags:
                    if tag['Key'] == 'Name':
                        ids.append(int(tag['Value'].split('-')[-1]))
        else:
            print("Incorrect resource type!")
        index = 1
        while True:
            if index not in ids:
                break
            else:
                index += 1
        return index
    except Exception as err:
        logging.error("Error with providing index: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with providing index",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_route_table_by_tag(tag_name, tag_value):
    try:
        client = boto3.client('ec2')
        route_tables = client.describe_route_tables(
            Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': ['{}'.format(tag_value)]}])
        rt_id = route_tables.get('RouteTables')[0].get('RouteTableId')
        return rt_id
    except Exception as err:
        logging.error("Error with getting Route table by tag: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with getting Route table by tag",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


@backoff.on_predicate(backoff.fibo, max_tries=4)
def get_ami_id(ami_name):
    try:
        client = boto3.client('ec2')
        image_id = ''
        response = client.describe_images(
            Filters=[
                {
                    'Name': 'name',
                    'Values': [ami_name]
                },
                {
                    'Name': 'virtualization-type', 'Values': ['hvm']
                },
                {
                    'Name': 'state', 'Values': ['available']
                },
                {
                    'Name': 'root-device-name', 'Values': ['/dev/sda1']
                },
                {
                    'Name': 'root-device-type', 'Values': ['ebs']
                },
                {
                    'Name': 'architecture', 'Values': ['x86_64']
                }
            ])
        response = response.get('Images')
        for i in response:
            image_id = i.get('ImageId')
        if image_id == '':
            raise Exception("Unable to find image id with name: " + ami_name)
        return image_id
    except Exception as err:
        logging.error("Failed to find AMI: " + ami_name + " : " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to find AMI", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_iam_profile(profile_name, count=0):
    client = boto3.client('iam')
    iam_profile = ''
    try:
        if count < 10:
            response = client.get_instance_profile(InstanceProfileName=profile_name)
            iam_profile = response.get('InstanceProfile').get('InstanceProfileName')
            time.sleep(5)
            print('IAM profile checked. Creating instance...')
        else:
            print("Unable to find IAM profile by name: {}".format(profile_name))
            return False
    except:
        count += 1
        print('IAM profile is not available yet. Waiting...')
        time.sleep(5)
        get_iam_profile(profile_name, count)
    print(iam_profile)
    return iam_profile


def check_security_group(security_group_name, count=0):
    try:
        ec2 = boto3.resource('ec2')
        if count < 20:
            for security_group in ec2.security_groups.filter(Filters=[{'Name': 'group-name', 'Values': [security_group_name]}]):
                while security_group.id == '':
                    count = count + 1
                    time.sleep(10)
                    print("Security group is not available yet. Waiting...")
                    check_security_group(security_group_name, count)
                if security_group.id == '':
                    raise Exception("Unable to check Security group by name: " + security_group_name)
                return security_group.id
    except Exception as err:
        logging.error("Error with checking Security group by name: " + security_group_name + " : " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with checking Security group by name", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def emr_waiter(tag_name, tag_value):
    if len(get_emr_list(tag_value, 'Value', False, True)) > 0 or os.path.exists('/response/.emr_creating_' + os.environ['exploratory_name']) or get_not_configured_emr(tag_name, tag_value):
        with hide('stderr', 'running', 'warnings'):
            local("echo 'Some EMR cluster is still being created/terminated, waiting..'")
        time.sleep(60)
        emr_waiter(tag_name, tag_value)
    else:
        return True


def get_spark_version(cluster_name):
    spark_version = ''
    emr = boto3.client('emr')
    clusters = emr.list_clusters(ClusterStates=['WAITING'])
    clusters = clusters.get('Clusters')
    for i in clusters:
        response = emr.describe_cluster(ClusterId=i.get('Id'))
        time.sleep(5)
        if response.get("Cluster").get("Name") == cluster_name:
            response =  response.get("Cluster").get("Applications")
            for j in response:
                if j.get("Name") == 'Spark':
                    spark_version = j.get("Version")
    return spark_version


def get_hadoop_version(cluster_name):
    hadoop_version = ''
    emr = boto3.client('emr')
    clusters = emr.list_clusters(ClusterStates=['WAITING'])
    clusters = clusters.get('Clusters')
    for i in clusters:
        response = emr.describe_cluster(ClusterId=i.get('Id'))
        time.sleep(5)
        if response.get("Cluster").get("Name") == cluster_name:
            response =  response.get("Cluster").get("Applications")
            for j in response:
                if j.get("Name") == 'Hadoop':
                    hadoop_version = j.get("Version")
    return hadoop_version[0:3]


def get_instance_status(tag_name, instance_name):
    client = boto3.client('ec2')
    response = client.describe_instances(Filters=[
        {'Name': 'tag:{}'.format(tag_name), 'Values': [instance_name]}]).get('Reservations')
    for i in response:
        if len(response) > 1:
            inst = i.get('Instances')
            for j in inst:
                if j.get('State').get('Name') == 'running':
                    return j.get('State').get('Name')
        else:
            inst = i.get('Instances')
            for j in inst:
                return j.get('State').get('Name')
    return 'not-running'


def get_list_instance_statuses(instance_ids):
    data = []
    client = boto3.client('ec2')
    for h in instance_ids:
        host = {}
        try:
            response = client.describe_instances(InstanceIds=[h.get('id')]).get('Reservations')
            for i in response:
                inst = i.get('Instances')
                for j in inst:
                    host['id'] = j.get('InstanceId')
                    host['status'] = j.get('State').get('Name')
                    data.append(host)
        except Exception as err:
            host['id'] = i.get('id')
            host['status'] = 'terminated'
            host['error_response'] = err
            data.append(host)
    return data


def get_list_cluster_statuses(cluster_ids, data=[]):
    client = boto3.client('emr')
    for i in cluster_ids:
        host = {}
        try:
            response = client.describe_cluster(ClusterId=i.get('id')).get('Cluster')
            host['id'] = i.get('id')
            if response.get('Status').get('State').lower() == 'waiting':
                host['status'] = 'running'
            elif response.get('Status').get('State').lower() == 'running':
                host['status'] = 'configuring'
            else:
                host['status'] = response.get('Status').get('State').lower()
            data.append(host)
        except:
            host['id'] = i.get('id')
            host['status'] = 'terminated'
            data.append(host)
    return data


def get_allocation_id_by_elastic_ip(elastic_ip):
    try:
        client = boto3.client('ec2')
        response = client.describe_addresses(PublicIps=[elastic_ip]).get('Addresses')
        for i in response:
            return i.get('AllocationId')
    except Exception as err:
        logging.error("Error with getting allocation id by elastic ip: " + elastic_ip + " : " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with getting allocation id by elastic ip", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_ec2_price(instance_shape, region):
    try:
        price = '0.001'
        # Price API endpoints: us-east-1, ap-south-1
        client = boto3.client('pricing', 'us-east-1')
        # Price API require full name of region, for example: eu-west-1 -> 'EU (Ireland)'
        # endpoints will be loaded from: botocore/botocore/data/endpoints.json
        data = client._loader._cache.get(('load_data', 'endpoints'))
        standard_partition = filter(lambda x: 'AWS Standard' == x['partitionName'], data['partitions'])[0]
        region_description = standard_partition['regions'][region]['description']

        response = client.get_products(
            ServiceCode='AmazonEC2',
            Filters=[
                {
                    'Type': 'TERM_MATCH',
                    'Field': 'instanceType',
                    'Value': '{0}'.format(instance_shape)
                },
                {
                    'Type': 'TERM_MATCH',
                    'Field': 'operatingSystem',
                    'Value': 'Linux'
                },
                {
                    'Type': 'TERM_MATCH',
                    'Field': 'tenancy',
                    'Value': 'Shared'
                },
                {
                    'Type': 'TERM_MATCH',
                    'Field': 'location',
                    'Value': '{0}'.format(region_description)
                },
                {
                    'Type': 'TERM_MATCH',
                    'Field': 'preInstalledSw',
                    'Value': 'NA'
                },
                {
                    'Type': 'TERM_MATCH',
                    'Field': 'capacityStatus',
                    'Value': 'UnusedCapacityReservation'
                }
            ],
            FormatVersion='aws_v1',
            NextToken='',
            MaxResults=1
        )

        data = json.loads(response['PriceList'][0])
        ondemand = data['terms']['OnDemand']
        for offer in ondemand:
            if (data['product']['sku'] in offer):
                for i in ondemand[offer]['priceDimensions'].keys():
                    price = ondemand[offer]['priceDimensions'][i]['pricePerUnit']['USD']

        return price
    except Exception as err:
        logging.error("Error with getting EC2 price: " + str(err) + "\n Traceback: " +
                      traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with getting EC2 price",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_spot_instances_status(cluster_id):
    try:
        ec2 = boto3.client('ec2')
        emr = boto3.client('emr')
        ec2_ids = emr.list_instances(ClusterId=cluster_id).get('Instances')
        ids_list = []
        for ins in ec2_ids:
            ids_list.append(ins.get('Ec2InstanceId'))
        response = ec2.describe_spot_instance_requests(Filters=[
            {'Name': 'instance-id', 'Values': ids_list}]).get('SpotInstanceRequests')
        if response:
            for i in response:
                if i.get('Status').get('Code') != 'fulfilled':
                    return False, i.get('Status').get('Code'), i.get('Status').get('Message')
            return True, i.get('Status').get('Code'), "Spot instances have been successfully created!"
        return False, None, "Spot instances status weren't received for cluster id {}".format(cluster_id)
    except Exception as err:
        logging.error("Error with getting Spot instances status: " + str(err) + "\n Traceback: " +
                      traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with getting Spot instances status",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def node_count(cluster_name):
    try:
        ec2 = boto3.client('ec2')
        node_list = ec2.describe_instances(Filters=[
            {'Name': 'instance-state-name', 'Values': ['running']},
            {'Name': 'tag:Name', 'Values': [cluster_name + '-*']}]).get('Reservations')
        result = len(node_list)
        return result
    except Exception as err:
        logging.error("Error with counting nodes in cluster: " + str(err) + "\n Traceback: " +
                      traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with counting nodes in cluster",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def get_list_private_ip_by_conf_type_and_id(conf_type, instance_id):
    try:
        private_list_ip = []
        if conf_type == 'edge_node':
                private_list_ip.append(
                    get_instance_ip_address_by_id(
                        instance_id).get('Private'))
        elif conf_type == 'exploratory':
            private_list_ip.append(
                get_instance_ip_address('Name', instance_id).get('Private'))
        elif conf_type == 'computational_resource':
            group_tag_name = os.environ['conf_service_base_name'] + ':' + instance_id
            print(group_tag_name)
            instance_list = get_ec2_list(os.environ['conf_tag_resource_id'], group_tag_name)
            for instance in instance_list:
                private_list_ip.append(
                    get_instance_ip_address_by_id(instance.id).get('Private'))
        return private_list_ip
    except Exception as err:
        logging.error("Error getting private ip by conf_type and id: " + str(err) + "\n Traceback: " +
                      traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error getting private ip by conf_type and id",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def format_sg(sg_rules):
    try:
        formatted_sg_rules = list()
        for rule in sg_rules:
            if rule['IpRanges']:
                for ip_range in rule['IpRanges']:
                    formatted_rule = dict()
                    for key in rule.keys():
                        if key == 'IpRanges':
                            formatted_rule['IpRanges'] = [ip_range]
                        else:
                            formatted_rule[key] = rule[key]
                    if formatted_rule not in formatted_sg_rules:
                        formatted_sg_rules.append(formatted_rule)
            else:
                formatted_sg_rules.append(rule)
        return formatted_sg_rules
    except Exception as err:
        logging.error("Error with formatting SG rules: " + str(err) + "\n Traceback: " +
                      traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Error with formatting SG rules",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)

