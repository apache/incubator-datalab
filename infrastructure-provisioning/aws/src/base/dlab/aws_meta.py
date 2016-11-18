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
import json


def get_instance_hostname(instance_name):
    ec2 = boto3.resource('ec2')
    instances = ec2.instances.filter(
        Filters=[{'Name': 'tag:Name', 'Values': [instance_name]},
                 {'Name': 'instance-state-name', 'Values': ['running']}])
    for instance in instances:
        public = getattr(instance, 'public_dns_name')
        private = getattr(instance, 'private_dns_name')
        if public:
            return public
        else:
            return private


def get_vpc_endpoints(vpc_id):
    # Returns LIST of Endpoint DICTIONARIES
    ec2 = boto3.client('ec2')
    endpoints = ec2.describe_vpc_endpoints(
        Filters=[{
            'Name':'vpc-id',
            'Values':[vpc_id]
        }]
    ).get('VpcEndpoints')
    return endpoints


def get_route_tables(vpc, tags):
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


def get_bucket_by_name(bucket_name):
    s3 = boto3.resource('s3')
    for bucket in s3.buckets.all():
        if bucket.name == bucket_name:
            return bucket.name
    return ''


def get_instance_ip_address(instance_name):
    ec2 = boto3.resource('ec2')
    instances = ec2.instances.filter(
        Filters=[{'Name': 'tag:Name', 'Values': [instance_name]},
                 {'Name': 'instance-state-name', 'Values': ['running']}])
    ips = {}
    for instance in instances:
        public = getattr(instance, 'public_ip_address')
        private = getattr(instance, 'private_ip_address')
        ips = {'Public': public, 'Private': private}
    return ips


def get_ami_id_by_name(ami_name):
    ec2 = boto3.resource('ec2')
    try:
        for image in ec2.images.filter(Filters=[{'Name': 'name', 'Values': [ami_name]}, {'Name': 'state', 'Values': ['available']}]):
            return image.id
    except:
        return ''
    return ''


def get_security_group_by_name(security_group_name):
    ec2 = boto3.resource('ec2')
    try:
        for security_group in ec2.security_groups.filter(GroupNames=[security_group_name]):
            return security_group.id
    except:
        return ''
    return ''


def get_instance_attr(instance_id, attribute_name):
    ec2 = boto3.resource('ec2')
    instances = ec2.instances.filter(
        Filters=[{'Name': 'instance-id', 'Values': [instance_id]},
                 {'Name': 'instance-state-name', 'Values': ['running']}])
    for instance in instances:
        return getattr(instance, attribute_name)
    return ''


def get_instance_by_name(instance_name):
    ec2 = boto3.resource('ec2')
    instances = ec2.instances.filter(
        Filters=[{'Name': 'tag:Name', 'Values': [instance_name]},
                 {'Name': 'instance-state-name', 'Values': ['running','pending','stopping','stopped']}])
    for instance in instances:
        return instance.id
    return ''


def get_role_by_name(role_name):
    iam = boto3.resource('iam')
    for role in iam.roles.all():
        if role.name == role_name:
            return role.name
    return ''


def get_subnet_by_cidr(cidr):
    ec2 = boto3.resource('ec2')
    for subnet in ec2.subnets.filter(Filters=[{'Name': 'cidrBlock', 'Values': [cidr]}]):
        return subnet.id
    return ''


def get_subnet_by_tag(tag):
    ec2 = boto3.resource('ec2')
    for subnet in ec2.subnets.filter(Filters=[
        {'Name': 'tag-key', 'Values': [tag.get('Key')]},
        {'Name': 'tag-value', 'Values': [tag.get('Value')]}
    ]):
        return subnet.cidr_block
    return ''


def get_vpc_by_cidr(cidr):
    ec2 = boto3.resource('ec2')
    for vpc in ec2.vpcs.filter(Filters=[{'Name': 'cidr', 'Values': [cidr]}]):
        return vpc.id
    return ''


def get_emr_info(id, key = ''):
    emr = boto3.client('emr')
    info = emr.describe_cluster(ClusterId=id)['Cluster']
    if key:
        try:
            result = info[key]
        except:
            print "Cluster has no {} attribute".format(key)
            result = info
    else:
        result = info
    return result


def get_emr_list(tag_name, type='Key', emr_count=False):
    emr = boto3.client('emr')
    if emr_count:
        clusters = emr.list_clusters(
            ClusterStates=['RUNNING', 'WAITING', 'STARTING', 'BOOTSTRAPPING', 'TERMINATING']
        )
    else:
        clusters = emr.list_clusters(
            ClusterStates=['RUNNING', 'WAITING', 'STARTING', 'BOOTSTRAPPING']
        )
    clusters = clusters.get('Clusters')
    clusters_list = []
    for i in clusters:
        response = emr.describe_cluster(ClusterId=i.get('Id'))
        tag = response.get('Cluster').get('Tags')
        for j in tag:
            if j.get(type) == tag_name:
                clusters_list.append(i.get('Id'))
    return clusters_list


def get_ec2_list(tag_name, value=''):
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


def provide_index(resource_type, tag_name, tag_value=''):
    ids = []
    if resource_type == 'EMR':
        if tag_value:
            list = get_emr_list(tag_value, 'Value', True)
        else:
            list = get_emr_list(tag_name, 'Key', True)
        emr = boto3.client('emr')
        for i in list:
            response = emr.describe_cluster(ClusterId=i)
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
        print "Incorrect resource type!"
    index = 1
    while True:
        if index not in ids:
            break
        else:
            index += 1
    return index


def get_route_table_by_tag(tag_name, tag_value):
    client = boto3.client('ec2')
    route_tables = client.describe_route_tables(
        Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': ['{}'.format(tag_value)]}])
    rt_id = route_tables.get('RouteTables')[0].get('RouteTableId')
    return rt_id