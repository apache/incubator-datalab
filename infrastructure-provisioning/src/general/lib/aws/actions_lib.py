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
import botocore
from botocore.client import Config
import backoff
from botocore.exceptions import ClientError
import time
import sys
import os
import json
from fabric.api import *
from fabric.contrib.files import exists
import logging
from dlab.meta_lib import *
from dlab.fab import *
import traceback
import urllib2
import meta_lib
import dlab.fab
import uuid
import ast


def backoff_log(err):
    logging.info("Unable to create Tag: " + \
                 str(err) + "\n Traceback: " + \
                 traceback.print_exc(file=sys.stdout))
    append_result(str({"error": "Unable to create Tag", \
                       "error_message": str(err) + "\n Traceback: " + \
                                        traceback.print_exc(file=sys.stdout)}))
    traceback.print_exc(file=sys.stdout)


def put_to_bucket(bucket_name, local_file, destination_file):
    try:
        s3 = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=os.environ['aws_region'])
        with open(local_file, 'rb') as data:
            s3.upload_fileobj(data, bucket_name, destination_file, ExtraArgs={'ServerSideEncryption': 'AES256'})
        return True
    except Exception as err:
        logging.info("Unable to upload files to S3 bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to upload files to S3 bucket",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)
        return False


def create_s3_bucket(bucket_name, bucket_tags, region, bucket_name_tag):
    try:
        s3 = boto3.resource('s3', config=Config(signature_version='s3v4'))
        if region == "us-east-1":
            bucket = s3.create_bucket(Bucket=bucket_name)
        else:
            bucket = s3.create_bucket(Bucket=bucket_name, CreateBucketConfiguration={'LocationConstraint': region})
        boto3.client('s3', config=Config(signature_version='s3v4')).put_bucket_encryption(
            Bucket=bucket_name, ServerSideEncryptionConfiguration={
                'Rules': [
                    {
                        'ApplyServerSideEncryptionByDefault': {
                            'SSEAlgorithm': 'AES256'
                        }
                    },
                ]
            })
        tags = list()
        tags.append({'Key': os.environ['conf_tag_resource_id'],
                     'Value': os.environ['conf_service_base_name'] + ':' + bucket_name_tag})
        for tag in bucket_tags.split(','):
            tags.append(
                {
                    'Key': tag.split(':')[0],
                    'Value': tag.split(':')[1]
                }
            )
        tagging = bucket.Tagging()
        tagging.put(Tagging={'TagSet': tags})
        tagging.reload()
        return bucket.name
    except Exception as err:
        logging.info("Unable to create S3 bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to create S3 bucket",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_vpc(vpc_cidr, tag):
    try:
        ec2 = boto3.resource('ec2')
        vpc = ec2.create_vpc(CidrBlock=vpc_cidr)
        create_tag(vpc.id, tag)
        return vpc.id
    except Exception as err:
        logging.info("Unable to create VPC: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create VPC",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def enable_vpc_dns(vpc_id):
    try:
        client = boto3.client('ec2')
        client.modify_vpc_attribute(VpcId=vpc_id,
                                    EnableDnsHostnames={'Value': True})
    except Exception as err:
        logging.info("Unable to modify VPC attributes: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to modify VPC attributes",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_vpc(vpc_id):
    try:
        client = boto3.client('ec2')
        client.delete_vpc(VpcId=vpc_id)
        print("VPC {} has been removed".format(vpc_id))
    except Exception as err:
        logging.info("Unable to remove VPC: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove VPC",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


@backoff.on_exception(backoff.expo,
                      botocore.exceptions.ClientError,
                      max_tries=40,
                      on_giveup=backoff_log)
def create_tag(resource, tag, with_tag_res_id=True):
    print('Tags for the resource {} will be created'.format(resource))
    tags_list = list()
    ec2 = boto3.client('ec2')
    if type(tag) == dict:
        resource_name = tag.get('Value')
        resource_tag = tag
    else:
        resource_name = json.loads(tag).get('Value')
        resource_tag = json.loads(tag)
    if type(resource) != list:
        resource = [resource]
    tags_list.append(resource_tag)
    if with_tag_res_id:
        tags_list.append(
            {
                'Key': os.environ['conf_tag_resource_id'],
                'Value': os.environ['conf_service_base_name'] + ':' + resource_name
            }
        )
        tags_list.append(
            {
                'Key': os.environ['conf_billing_tag_key'],
                'Value': os.environ['conf_billing_tag_value']
            }
        )
    if 'conf_additional_tags' in os.environ:
        for tag in os.environ['conf_additional_tags'].split(';'):
            tags_list.append(
                {
                    'Key': tag.split(':')[0],
                    'Value': tag.split(':')[1]
                }
            )
    ec2.create_tags(
        Resources=resource,
        Tags=tags_list
    )


def remove_emr_tag(emr_id, tag):
    try:
        emr = boto3.client('emr')
        emr.remove_tags(ResourceId=emr_id, TagKeys=tag)
    except Exception as err:
        logging.info("Unable to remove Tag: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove Tag",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_rt(vpc_id, infra_tag_name, infra_tag_value, secondary):
    try:
        tag = {"Key": infra_tag_name, "Value": infra_tag_value}
        route_table = []
        ec2 = boto3.client('ec2')
        rt = ec2.create_route_table(VpcId=vpc_id)
        rt_id = rt.get('RouteTable').get('RouteTableId')
        route_table.append(rt_id)
        print('Created Route-Table with ID: {}'.format(rt_id))
        create_tag(route_table, json.dumps(tag))
        if not secondary:
            ig = ec2.create_internet_gateway()
            ig_id = ig.get('InternetGateway').get('InternetGatewayId')
            route_table = []
            route_table.append(ig_id)
            create_tag(route_table, json.dumps(tag))
            ec2.attach_internet_gateway(InternetGatewayId=ig_id, VpcId=vpc_id)
            ec2.create_route(DestinationCidrBlock='0.0.0.0/0', RouteTableId=rt_id, GatewayId=ig_id)
        return rt_id
    except Exception as err:
        logging.info(
            "Unable to create Route Table: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create Route Table",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_subnet(vpc_id, subnet, tag, zone):
    try:
        ec2 = boto3.resource('ec2')
        if zone != "":
            subnet = ec2.create_subnet(VpcId=vpc_id, CidrBlock=subnet, AvailabilityZone=zone)
        else:
            subnet = ec2.create_subnet(VpcId=vpc_id, CidrBlock=subnet)
        create_tag(subnet.id, tag)
        subnet.reload()
        return subnet.id
    except Exception as err:
        logging.info("Unable to create Subnet: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create Subnet",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_security_group(security_group_name, vpc_id, security_group_rules, egress, tag):
    try:
        ec2 = boto3.resource('ec2')
        tag_name = {"Key": "Name", "Value": security_group_name}
        group = ec2.create_security_group(GroupName=security_group_name, Description='security_group_name',
                                          VpcId=vpc_id)
        time.sleep(10)
        create_tag(group.id, tag)
        create_tag(group.id, tag_name)
        if 'conf_billing_tag_key' in os.environ and 'conf_billing_tag_value' in os.environ:
            create_tag(group.id, {'Key': os.environ['conf_billing_tag_key'],
                                  'Value': os.environ['conf_billing_tag_value']})
        try:
            group.revoke_egress(IpPermissions=[{"IpProtocol": "-1", "IpRanges": [{"CidrIp": "0.0.0.0/0"}],
                                                "UserIdGroupPairs": [], "PrefixListIds": []}])
        except:
            print("Mentioned rule does not exist")
        for rule in security_group_rules:
            group.authorize_ingress(IpPermissions=[rule])
        for rule in egress:
            group.authorize_egress(IpPermissions=[rule])
        return group.id
    except Exception as err:
        logging.info(
            "Unable to create security group: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create security group",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_route_by_id(subnet_id, vpc_id, peering_id, another_cidr):
    client = boto3.client('ec2')
    try:
        table_id = client.describe_route_tables(Filters=[{'Name': 'association.subnet-id', 'Values': [subnet_id]}]).get(
            'RouteTables')
        if table_id:
            final_id = table_id[0]['Associations'][0]['RouteTableId']
        else:
            table_id = client.describe_route_tables(
                Filters=[{'Name': 'vpc-id', 'Values': [vpc_id]}, {'Name': 'association.main', 'Values': ['true']}]).get(
                'RouteTables')
            final_id = table_id[0]['Associations'][0]['RouteTableId']
        for table in table_id:
            routes = table.get('Routes')
            routeExists = False
            for route in routes:
                if route.get('DestinationCidrBlock') == another_cidr:
                    routeExists = True
            if not routeExists:
                client.create_route(
                    DestinationCidrBlock=another_cidr,
                    VpcPeeringConnectionId=peering_id,
                    RouteTableId=final_id)
    except Exception as err:
        logging.info("Unable to create route: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create route",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_peer_routes(peering_id, service_base_name):
    client = boto3.client('ec2')
    try:
        route_tables = client.describe_route_tables(
            Filters=[{'Name': 'tag:{}-tag'.format(service_base_name), 'Values': ['{}'.format(
                service_base_name)]}]).get('RouteTables')
        route_tables2 = client.describe_route_tables(Filters=[
            {'Name': 'tag:{}-secondary-tag'.format(service_base_name), 'Values': ['{}'.format(
                service_base_name)]}]).get('RouteTables')
        for table in route_tables:
            routes = table.get('Routes')
            routeExists = False
            for route in routes:
                if route.get('DestinationCidrBlock') == os.environ['conf_vpc2_cidr'].replace("'", ""):
                    routeExists = True
            if not routeExists:
                client.create_route(
                    DestinationCidrBlock=os.environ['conf_vpc2_cidr'].replace("'", ""),
                    VpcPeeringConnectionId=peering_id,
                    RouteTableId=table.get('RouteTableId'))
        for table in route_tables2:
            routes = table.get('Routes')
            routeExists = False
            for route in routes:
                if route.get('DestinationCidrBlock') == os.environ['conf_vpc_cidr'].replace("'", ""):
                    routeExists = True
            if not routeExists:
                client.create_route(
                    DestinationCidrBlock=os.environ['conf_vpc_cidr'].replace("'", ""),
                    VpcPeeringConnectionId=peering_id,
                    RouteTableId=table.get('RouteTableId'))
    except Exception as err:
        logging.info("Unable to create route: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create route",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_peering_connection(vpc_id, vpc2_id, service_base_name):
    try:
        ec2 = boto3.resource('ec2')
        client = boto3.client('ec2')
        tag = {"Key": service_base_name + '-tag', "Value": service_base_name}
        tag_name = {"Key": 'Name', "Value": "{0}-peering-connection".format(service_base_name)}
        peering = ec2.create_vpc_peering_connection(PeerVpcId=vpc_id, VpcId=vpc2_id)
        client.accept_vpc_peering_connection(VpcPeeringConnectionId=peering.id)
        client.modify_vpc_peering_connection_options(
            AccepterPeeringConnectionOptions={
                'AllowDnsResolutionFromRemoteVpc': True,
            },
            RequesterPeeringConnectionOptions={
                'AllowDnsResolutionFromRemoteVpc': True,
            },
            VpcPeeringConnectionId=peering.id
        )
        create_tag(peering.id, json.dumps(tag))
        create_tag(peering.id, json.dumps(tag_name))
        return peering.id
    except Exception as err:
        logging.info("Unable to create peering connection: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to create peering connection",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def enable_auto_assign_ip(subnet_id):
    try:
        client = boto3.client('ec2')
        client.modify_subnet_attribute(MapPublicIpOnLaunch={'Value': True}, SubnetId=subnet_id)
    except Exception as err:
        logging.info("Unable to create Subnet: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create Subnet",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_instance(definitions, instance_tag, primary_disk_size=12):
    try:
        ec2 = boto3.resource('ec2')
        security_groups_ids = []
        for chunk in definitions.security_group_ids.split(','):
            security_groups_ids.append(chunk.strip())
        user_data = ''
        if definitions.user_data_file != '':
            try:
                with open(definitions.user_data_file, 'r') as f:
                    for line in f:
                        user_data = user_data + line
                f.close()
            except:
                print("Error reading user-data file")
        if definitions.instance_class == 'notebook':
            instances = ec2.create_instances(ImageId=definitions.ami_id, MinCount=1, MaxCount=1,
                                             BlockDeviceMappings=[
                                                 {
                                                     "DeviceName": "/dev/sda1",
                                                     "Ebs":
                                                         {
                                                             "VolumeSize": int(primary_disk_size)
                                                         }
                                                 },
                                                 {
                                                     "DeviceName": "/dev/sdb",
                                                     "Ebs":
                                                         {
                                                             "VolumeSize": int(definitions.instance_disk_size)
                                                         }
                                                 }],
                                             KeyName=definitions.key_name,
                                             SecurityGroupIds=security_groups_ids,
                                             InstanceType=definitions.instance_type,
                                             SubnetId=definitions.subnet_id,
                                             IamInstanceProfile={'Name': definitions.iam_profile},
                                             UserData=user_data)
        elif definitions.instance_class == 'dataengine':
            instances = ec2.create_instances(ImageId=definitions.ami_id, MinCount=1, MaxCount=1,
                                             BlockDeviceMappings=[
                                                 {
                                                     "DeviceName": "/dev/sda1",
                                                     "Ebs":
                                                         {
                                                             "VolumeSize": int(primary_disk_size)
                                                         }
                                                 }],
                                             KeyName=definitions.key_name,
                                             SecurityGroupIds=security_groups_ids,
                                             InstanceType=definitions.instance_type,
                                             SubnetId=definitions.subnet_id,
                                             IamInstanceProfile={'Name': definitions.iam_profile},
                                             UserData=user_data)
        elif definitions.instance_class == 'ssn':
            get_iam_profile(definitions.iam_profile)
            instances = ec2.create_instances(ImageId=definitions.ami_id, MinCount=1, MaxCount=1,
                                             BlockDeviceMappings=[
                                                 {
                                                     "DeviceName": "/dev/sda1",
                                                     "Ebs":
                                                         {
                                                             "VolumeSize": int(primary_disk_size)
                                                         }
                                                 }],
                                             KeyName=definitions.key_name,
                                             SecurityGroupIds=security_groups_ids,
                                             InstanceType=definitions.instance_type,
                                             SubnetId=definitions.subnet_id,
                                             IamInstanceProfile={'Name': definitions.iam_profile},
                                             UserData=user_data)
        else:
            get_iam_profile(definitions.iam_profile)
            instances = ec2.create_instances(ImageId=definitions.ami_id, MinCount=1, MaxCount=1,
                                             KeyName=definitions.key_name,
                                             SecurityGroupIds=security_groups_ids,
                                             InstanceType=definitions.instance_type,
                                             SubnetId=definitions.subnet_id,
                                             IamInstanceProfile={'Name': definitions.iam_profile},
                                             UserData=user_data)
        for instance in instances:
            print("Waiting for instance {} become running.".format(instance.id))
            instance.wait_until_running()
            tag = {'Key': 'Name', 'Value': definitions.node_name}
            create_tag(instance.id, tag)
            create_tag(instance.id, instance_tag)
            tag_intance_volume(instance.id, definitions.node_name, instance_tag)
            return instance.id
        return ''
    except Exception as err:
        logging.info("Unable to create EC2: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create EC2",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def tag_intance_volume(instance_id, node_name, instance_tag):
    try:
        print('volume tagging')
        volume_list = meta_lib.get_instance_attr(instance_id, 'block_device_mappings')
        counter = 0
        instance_tag_value = instance_tag.get('Value')
        for volume in volume_list:
            if counter == 1:
                volume_postfix = '-volume-secondary'
            else:
                volume_postfix = '-volume-primary'
            tag = {'Key': 'Name',
                   'Value': node_name + volume_postfix}
            volume_tag = instance_tag
            volume_tag['Value'] = instance_tag_value + volume_postfix
            volume_id = volume.get('Ebs').get('VolumeId')
            create_tag(volume_id, tag)
            create_tag(volume_id, volume_tag)
            counter += 1
    except Exception as err:
        logging.info(
            "Unable to tag volumes: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
        append_result(str({"error": "Unable to tag volumes",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                               file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def tag_emr_volume(cluster_id, node_name, billing_tag):
    try:
        client = boto3.client('emr')
        cluster = client.list_instances(ClusterId=cluster_id)
        instances = cluster['Instances']
        for instance in instances:
            instance_tag = {'Key': os.environ['conf_service_base_name'] + '-tag',
                            'Value': node_name}
            tag_intance_volume(instance['Ec2InstanceId'], node_name, instance_tag)
    except Exception as err:
        logging.info(
            "Unable to tag emr volumes: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
        append_result(str({"error": "Unable to tag emr volumes",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                               file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_iam_role(role_name, role_profile, region, service='ec2', tag=None, user_tag=None):
    conn = boto3.client('iam')
    try:
        if region == 'cn-north-1':
            conn.create_role(
                RoleName=role_name,
                AssumeRolePolicyDocument=
                '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":["' + service +
                '.amazonaws.com.cn"]},"Action":["sts:AssumeRole"]}]}')
        else:
            conn.create_role(
                RoleName=role_name, AssumeRolePolicyDocument=
                '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":["' + service +
                '.amazonaws.com"]},"Action":["sts:AssumeRole"]}]}')
        if tag:
            conn.tag_role(RoleName=role_name, Tags=[tag])
            conn.tag_role(RoleName=role_name, Tags=[{"Key": "Name", "Value": role_name}])
            if user_tag:
                conn.tag_role(RoleName=role_name, Tags=[user_tag])
            if 'conf_billing_tag_key' in os.environ and 'conf_billing_tag_value' in os.environ:
                conn.tag_role(RoleName=role_name, Tags=[{'Key': os.environ['conf_billing_tag_key'],
                                                         'Value': os.environ['conf_billing_tag_value']}])
            if 'project_name' in os.environ:
                conn.tag_role(RoleName=role_name, Tags=[{'Key': "project_tag",
                                                         'Value': os.environ['project_name']}])
            if 'endpoint_name' in os.environ:
                conn.tag_role(RoleName=role_name, Tags=[{'Key': "endpoint_tag",
                                                         'Value': os.environ['endpoint_name']}])
    except botocore.exceptions.ClientError as e_role:
        if e_role.response['Error']['Code'] == 'EntityAlreadyExists':
            print("IAM role already exists. Reusing...")
        else:
            logging.info("Unable to create IAM role: " + str(e_role.response['Error']['Message']) +
                         "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create IAM role",
                               "error_message": str(e_role.response['Error']['Message']) + "\n Traceback: " +
                                                traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return
    if service == 'ec2':
        try:
            conn.create_instance_profile(InstanceProfileName=role_profile)
            waiter = conn.get_waiter('instance_profile_exists')
            waiter.wait(InstanceProfileName=role_profile)
        except botocore.exceptions.ClientError as e_profile:
            if e_profile.response['Error']['Code'] == 'EntityAlreadyExists':
                print("Instance profile already exists. Reusing...")
            else:
                logging.info("Unable to create Instance Profile: " + str(e_profile.response['Error']['Message']) +
                             "\n Traceback: " + traceback.print_exc(file=sys.stdout))
                append_result(str({"error": "Unable to create Instance Profile",
                                   "error_message": str(e_profile.response['Error']['Message']) + "\n Traceback: " +
                                                    traceback.print_exc(file=sys.stdout)}))
                traceback.print_exc(file=sys.stdout)
                return
        try:
            conn.add_role_to_instance_profile(InstanceProfileName=role_profile, RoleName=role_name)
            time.sleep(30)
        except botocore.exceptions.ClientError as err:
            logging.info("Unable to add IAM role to instance profile: " + str(err.response['Error']['Message']) +
                         "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to add IAM role to instance profile",
                               "error_message": str(err.response['Error']['Message']) + "\n Traceback: " +
                                                traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)


def attach_policy(role_name, policy_arn):
    try:
        conn = boto3.client('iam')
        conn.attach_role_policy(PolicyArn=policy_arn, RoleName=role_name)
        time.sleep(30)
    except botocore.exceptions.ClientError as err:
        logging.info("Unable to attach Policy: " + str(err.response['Error']['Message']) + "\n Traceback: " +
                     traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to attach Policy",
                           "error_message": str(err.response['Error']['Message']) + "\n Traceback: " +
                                            traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_attach_policy(policy_name, role_name, file_path):
    try:
        conn = boto3.client('iam')
        with open(file_path, 'r') as myfile:
            json_file = myfile.read()
        conn.put_role_policy(RoleName=role_name, PolicyName=policy_name, PolicyDocument=json_file)
    except Exception as err:
        logging.info("Unable to attach Policy: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to attach Policy",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_route_53_record(hosted_zone_id, hosted_zone_name, subdomain, ip_address):
    try:
        if 'ssn_assume_role_arn' in os.environ:
            role_session_name = str(uuid.uuid4()).split('-')[0]
            sts_client = boto3.client('sts')
            credentials = sts_client.assume_role(
                RoleArn=os.environ['ssn_assume_role_arn'],
                RoleSessionName=role_session_name
            ).get('Credentials')
            route53_client = boto3.client('route53',
                                          aws_access_key_id=credentials.get('AccessKeyId'),
                                          aws_secret_access_key=credentials.get('SecretAccessKey'),
                                          aws_session_token=credentials.get('SessionToken')
                                          )
        else:
            route53_client = boto3.client('route53')
        route53_client.change_resource_record_sets(
            HostedZoneId=hosted_zone_id,
            ChangeBatch={
                'Changes': [
                    {
                        'Action': 'CREATE',
                        'ResourceRecordSet': {
                            'Name': "{}.{}".format(subdomain, hosted_zone_name),
                            'Type': 'A',
                            'TTL': 300,
                            'ResourceRecords': [
                                {
                                    'Value': ip_address
                                }
                            ]
                        }
                    }
                ]
            }
        )
    except Exception as err:
        logging.info("Unable to create Route53 record: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to create Route53 record",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_route_53_record(hosted_zone_id, hosted_zone_name, subdomain):
    try:
        if 'ssn_assume_role_arn' in os.environ:
            role_session_name = str(uuid.uuid4()).split('-')[0]
            sts_client = boto3.client('sts')
            credentials = sts_client.assume_role(
                RoleArn=os.environ['ssn_assume_role_arn'],
                RoleSessionName=role_session_name
            ).get('Credentials')
            route53_client = boto3.client('route53',
                                          aws_access_key_id=credentials.get('AccessKeyId'),
                                          aws_secret_access_key=credentials.get('SecretAccessKey'),
                                          aws_session_token=credentials.get('SessionToken')
                                          )
        else:
            route53_client = boto3.client('route53')
        for record_set in route53_client.list_resource_record_sets(
                HostedZoneId=hosted_zone_id).get('ResourceRecordSets'):
            if record_set['Name'] == "{}.{}.".format(subdomain, hosted_zone_name):
                for record in record_set['ResourceRecords']:
                    route53_client.change_resource_record_sets(
                        HostedZoneId=hosted_zone_id,
                        ChangeBatch={
                            'Changes': [
                                {
                                    'Action': 'DELETE',
                                    'ResourceRecordSet': {
                                        'Name': record_set['Name'],
                                        'Type': 'A',
                                        'TTL': 300,
                                        'ResourceRecords': [
                                            {
                                                'Value': record['Value']
                                            }
                                        ]
                                    }
                                }
                            ]
                        }
                    )
    except Exception as err:
        logging.info("Unable to remove Route53 record: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to remove Route53 record",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def allocate_elastic_ip():
    try:
        client = boto3.client('ec2')
        response = client.allocate_address(Domain='vpc')
        return response.get('AllocationId')
    except Exception as err:
        logging.info("Unable to allocate Elastic IP: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to allocate Elastic IP",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def release_elastic_ip(allocation_id):
    try:
        client = boto3.client('ec2')
        client.release_address(AllocationId=allocation_id)
    except Exception as err:
        logging.info("Unable to release Elastic IP: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to release Elastic IP",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def associate_elastic_ip(instance_id, allocation_id):
    try:
        client = boto3.client('ec2')
        response = client.associate_address(InstanceId=instance_id, AllocationId=allocation_id)
        return response.get('AssociationId')
    except Exception as err:
        logging.info("Unable to associate Elastic IP: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to associate Elastic IP",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def disassociate_elastic_ip(association_id):
    try:
        client = boto3.client('ec2')
        client.disassociate_address(AssociationId=association_id)
    except Exception as err:
        logging.info("Unable to disassociate Elastic IP: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to disassociate Elastic IP",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_ec2(tag_name, tag_value):
    try:
        ec2 = boto3.resource('ec2')
        client = boto3.client('ec2')
        association_id = ''
        allocation_id = ''
        inst = ec2.instances.filter(
            Filters=[{'Name': 'instance-state-name', 'Values': ['running', 'stopped', 'pending', 'stopping']},
                     {'Name': 'tag:{}'.format(tag_name), 'Values': ['{}'.format(tag_value)]}])
        instances = list(inst)
        if instances:
            for instance in instances:
                try:
                    response = client.describe_instances(InstanceIds=[instance.id])
                    for i in response.get('Reservations'):
                        for h in i.get('Instances'):
                            elastic_ip = h.get('PublicIpAddress')
                            try:
                                response = client.describe_addresses(PublicIps=[elastic_ip]).get('Addresses')
                                for el_ip in response:
                                    allocation_id = el_ip.get('AllocationId')
                                    association_id = el_ip.get('AssociationId')
                                    disassociate_elastic_ip(association_id)
                                    release_elastic_ip(allocation_id)
                                    print("Releasing Elastic IP: {}".format(elastic_ip))
                            except:
                                print("There is no such Elastic IP: {}".format(elastic_ip))
                except Exception as err:
                    print(err)
                    print("There is no Elastic IP to disassociate from instance: {}".format(instance.id))
                client.terminate_instances(InstanceIds=[instance.id])
                waiter = client.get_waiter('instance_terminated')
                waiter.wait(InstanceIds=[instance.id])
                print("The instance {} has been terminated successfully".format(instance.id))
        else:
            print("There are no instances with '{}' tag to terminate".format(tag_name))
    except Exception as err:
        logging.info("Unable to remove EC2: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to EC2", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def stop_ec2(tag_name, tag_value):
    try:
        ec2 = boto3.resource('ec2')
        client = boto3.client('ec2')
        inst = ec2.instances.filter(
            Filters=[{'Name': 'instance-state-name', 'Values': ['running', 'pending']},
                     {'Name': 'tag:{}'.format(tag_name), 'Values': ['{}'.format(tag_value)]}])
        instances = list(inst)
        if instances:
            id_instances = list()
            for instance in instances:
                id_instances.append(instance.id)
            client.stop_instances(InstanceIds=id_instances)
            waiter = client.get_waiter('instance_stopped')
            waiter.wait(InstanceIds=id_instances)
            print("The instances {} have been stopped successfully".format(id_instances))
        else:
            print("There are no instances with {} name to stop".format(tag_value))
    except Exception as err:
        logging.info("Unable to stop EC2: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to stop EC2",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def start_ec2(tag_name, tag_value):
    try:
        ec2 = boto3.resource('ec2')
        client = boto3.client('ec2')
        inst = ec2.instances.filter(
            Filters=[{'Name': 'instance-state-name', 'Values': ['stopped']},
                     {'Name': 'tag:{}'.format(tag_name), 'Values': ['{}'.format(tag_value)]}])
        instances = list(inst)
        if instances:
            id_instances = list()
            for instance in instances:
                id_instances.append(instance.id)
            client.start_instances(InstanceIds=id_instances)
            waiter = client.get_waiter('instance_status_ok')
            waiter.wait(InstanceIds=id_instances)
            print("The instances {} have been started successfully".format(id_instances))
        else:
            print("There are no instances with {} name to start".format(tag_value))
    except Exception as err:
        logging.info("Unable to start EC2: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to start EC2",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_detach_iam_policies(role_name, action=''):
    client = boto3.client('iam')
    service_base_name = os.environ['conf_service_base_name']
    try:
        policy_list = client.list_attached_role_policies(RoleName=role_name).get('AttachedPolicies')
        for i in policy_list:
            policy_arn = i.get('PolicyArn')
            client.detach_role_policy(RoleName=role_name, PolicyArn=policy_arn)
            print("The IAM policy {} has been detached successfully".format(policy_arn))
            if action == 'delete' and service_base_name in i.get('PolicyName'):
                client.delete_policy(PolicyArn=policy_arn)
                print("The IAM policy {} has been deleted successfully".format(policy_arn))
    except Exception as err:
        logging.info("Unable to remove/detach IAM policy: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to remove/detach IAM policy",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_roles_and_profiles(role_name, role_profile_name):
    client = boto3.client('iam')
    try:
        client.remove_role_from_instance_profile(InstanceProfileName=role_profile_name, RoleName=role_name)
        client.delete_instance_profile(InstanceProfileName=role_profile_name)
        client.delete_role(RoleName=role_name)
        print("The IAM role {0} and instance profile {1} have been deleted successfully".format(role_name,
                                                                                                role_profile_name))
    except Exception as err:
        logging.info("Unable to remove IAM role/profile: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to remove IAM role/profile",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_all_iam_resources(instance_type, project_name='', endpoint_name=''):
    try:
        client = boto3.client('iam')
        service_base_name = os.environ['conf_service_base_name']
        roles_list = []
        if project_name:
            start_prefix = '{}-{}-{}-'.format(service_base_name, project_name, endpoint_name)
        else:
            start_prefix = '{}-'.format(service_base_name)
        for item in client.list_roles(MaxItems=250).get("Roles"):
            if item.get("RoleName").startswith(start_prefix):
                roles_list.append(item.get('RoleName'))
        if roles_list:
            roles_list.sort(reverse=True)
            for iam_role in roles_list:
                if '-ssn-role' in iam_role and instance_type == 'ssn' or instance_type == 'all':
                    try:
                        client.delete_role_policy(RoleName=iam_role, PolicyName='{0}-ssn-policy'.format(
                            service_base_name))
                    except:
                        print('There is no policy {}-ssn-policy to delete'.format(service_base_name))
                    role_profiles = client.list_instance_profiles_for_role(RoleName=iam_role).get('InstanceProfiles')
                    if role_profiles:
                        for i in role_profiles:
                            role_profile_name = i.get('InstanceProfileName')
                            if role_profile_name == '{0}-ssn-profile'.format(service_base_name):
                                remove_roles_and_profiles(iam_role, role_profile_name)
                    else:
                        print("There is no instance profile for {}".format(iam_role))
                        client.delete_role(RoleName=iam_role)
                        print("The IAM role {} has been deleted successfully".format(iam_role))
                if '-edge-role' in iam_role:
                    if instance_type == 'edge' and project_name in iam_role:
                        remove_detach_iam_policies(iam_role, 'delete')
                        role_profile_name = '{0}-{1}-{2}-edge-profile'.format(service_base_name, project_name,
                                                                              os.environ['endpoint_name'].lower())
                        try:
                            client.get_instance_profile(InstanceProfileName=role_profile_name)
                            remove_roles_and_profiles(iam_role, role_profile_name)
                        except:
                            print("There is no instance profile for {}".format(iam_role))
                            client.delete_role(RoleName=iam_role)
                            print("The IAM role {} has been deleted successfully".format(iam_role))
                    if instance_type == 'all':
                        remove_detach_iam_policies(iam_role, 'delete')
                        role_profile_name = client.list_instance_profiles_for_role(
                            RoleName=iam_role).get('InstanceProfiles')
                        if role_profile_name:
                            for i in role_profile_name:
                                role_profile_name = i.get('InstanceProfileName')
                                remove_roles_and_profiles(iam_role, role_profile_name)
                        else:
                            print("There is no instance profile for {}".format(iam_role))
                            client.delete_role(RoleName=iam_role)
                            print("The IAM role {} has been deleted successfully".format(iam_role))
                if '-nb-de-role' in iam_role:
                    if instance_type == 'notebook' and project_name in iam_role:
                        remove_detach_iam_policies(iam_role)
                        role_profile_name = '{0}-{1}-{2}-nb-de-profile'.format(service_base_name, project_name,
                                                                               os.environ['endpoint_name'].lower())
                        try:
                            client.get_instance_profile(InstanceProfileName=role_profile_name)
                            remove_roles_and_profiles(iam_role, role_profile_name)
                        except:
                            print("There is no instance profile for {}".format(iam_role))
                            client.delete_role(RoleName=iam_role)
                            print("The IAM role {} has been deleted successfully".format(iam_role))
                    if instance_type == 'all':
                        remove_detach_iam_policies(iam_role)
                        role_profile_name = client.list_instance_profiles_for_role(
                            RoleName=iam_role).get('InstanceProfiles')
                        if role_profile_name:
                            for i in role_profile_name:
                                role_profile_name = i.get('InstanceProfileName')
                                remove_roles_and_profiles(iam_role, role_profile_name)
                        else:
                            print("There is no instance profile for {}".format(iam_role))
                            client.delete_role(RoleName=iam_role)
                            print("The IAM role {} has been deleted successfully".format(iam_role))
        else:
            print("There are no IAM roles to delete. Checking instance profiles...")
        profile_list = []
        for item in client.list_instance_profiles(MaxItems=250).get("InstanceProfiles"):
            if item.get("InstanceProfileName").startswith(start_prefix):
                profile_list.append(item.get('InstanceProfileName'))
        if profile_list:
            for instance_profile in profile_list:
                if '-ssn-profile' in instance_profile and instance_type == 'ssn' or instance_type == 'all':
                    client.delete_instance_profile(InstanceProfileName=instance_profile)
                    print("The instance profile {} has been deleted successfully".format(instance_profile))
                if '-edge-profile' in instance_profile:
                    if instance_type == 'edge' and project_name in instance_profile:
                        client.delete_instance_profile(InstanceProfileName=instance_profile)
                        print("The instance profile {} has been deleted successfully".format(instance_profile))
                    if instance_type == 'all':
                        client.delete_instance_profile(InstanceProfileName=instance_profile)
                        print("The instance profile {} has been deleted successfully".format(instance_profile))
                if '-nb-de-profile' in instance_profile:
                    if instance_type == 'notebook' and project_name in instance_profile:
                        client.delete_instance_profile(InstanceProfileName=instance_profile)
                        print("The instance profile {} has been deleted successfully".format(instance_profile))
                    if instance_type == 'all':
                        client.delete_instance_profile(InstanceProfileName=instance_profile)
                        print("The instance profile {} has been deleted successfully".format(instance_profile))
        else:
            print("There are no instance profiles to delete")
    except Exception as err:
        logging.info("Unable to remove some of the IAM resources: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to remove some of the IAM resources",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def s3_cleanup(bucket, cluster_name, user_name):
    s3_res = boto3.resource('s3', config=Config(signature_version='s3v4'))
    client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=os.environ['aws_region'])
    try:
        client.head_bucket(Bucket=bucket)
    except:
        print("There is no bucket {} or you do not permission to access it".format(bucket))
        sys.exit(0)
    try:
        resource = s3_res.Bucket(bucket)
        prefix = user_name + '/' + cluster_name + "/"
        for i in resource.objects.filter(Prefix=prefix):
            s3_res.Object(resource.name, i.key).delete()
    except Exception as err:
        logging.info("Unable to clean S3 bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to clean S3 bucket",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_s3(bucket_type='all', scientist=''):
    try:
        client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=os.environ['aws_region'])
        s3 = boto3.resource('s3')
        bucket_list = []
        if bucket_type == 'ssn':
            bucket_name = (os.environ['conf_service_base_name'] + '-ssn-bucket').lower().replace('_', '-')
            bucket_list.append(('{0}-{1}-shared-bucket'.format(os.environ['conf_service_base_name'],
                                                               os.environ['default_endpoint_name'])).lower().replace('_', '-'))
        elif bucket_type == 'edge':
            bucket_name = (os.environ['conf_service_base_name'] + '-' + "{}".format(scientist) + '-' +
                           os.environ['endpoint_name'] + '-bucket').lower().replace('_', '-')
        else:
            bucket_name = (os.environ['conf_service_base_name']).lower().replace('_', '-')
        for item in client.list_buckets().get('Buckets'):
            if bucket_name in item.get('Name'):
                for i in client.get_bucket_tagging(Bucket=item.get('Name')).get('TagSet'):
                    i.get('Key')
                    if i.get('Key') == os.environ['conf_service_base_name'].lower() + '-tag':
                        bucket_list.append(item.get('Name'))
        for s3bucket in bucket_list:
            if s3bucket:
                bucket = s3.Bucket(s3bucket)
                bucket.objects.all().delete()
                print("The S3 bucket {} has been cleaned".format(s3bucket))
                client.delete_bucket(Bucket=s3bucket)
                print("The S3 bucket {} has been deleted successfully".format(s3bucket))
            else:
                print("There are no buckets to delete")
    except Exception as err:
        logging.info("Unable to remove S3 bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to remove S3 bucket",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_subnets(tag_value):
    try:
        ec2 = boto3.resource('ec2')
        client = boto3.client('ec2')
        tag_name = os.environ['conf_service_base_name'].lower() + '-tag'
        tag2_name = os.environ['conf_service_base_name'].lower() + '-secondary-tag'
        subnets = ec2.subnets.filter(
            Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': [tag_value]}])
        subnets2 = ec2.subnets.filter(
            Filters=[{'Name': 'tag:{}'.format(tag2_name), 'Values': [tag_value]}])
        if subnets or subnets2:
            if subnets:
                for subnet in subnets:
                    client.delete_subnet(SubnetId=subnet.id)
                    print("The subnet {} has been deleted successfully".format(subnet.id))
            if subnets2:
                for subnet in subnets2:
                    client.delete_subnet(SubnetId=subnet.id)
                    print("The subnet {} has been deleted successfully".format(subnet.id))
        else:
            print("There are no private subnets to delete")
    except Exception as err:
        logging.info("Unable to remove subnet: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove subnet", "error_message": str(err) + "\n Traceback: " +
                                                                                traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_peering(tag_value):
    try:
        client = boto3.client('ec2')
        tag_name = os.environ['conf_service_base_name'].lower() + '-tag'
        if os.environ['conf_duo_vpc_enable'] == 'true':
            peering_id = client.describe_vpc_peering_connections(Filters=[
                {'Name': 'tag-key', 'Values': [tag_name]},
                {'Name': 'tag-value', 'Values': [tag_value]},
                {'Name': 'status-code', 'Values':
                    ['active']}]).get('VpcPeeringConnections')[0].get('VpcPeeringConnectionId')
            if peering_id:
                client.delete_vpc_peering_connection(VpcPeeringConnectionId=peering_id)
                print("Peering connection {} has been deleted successfully".format(peering_id))
            else:
                print("There are no peering connections to delete")
        else:
            print("There are no peering connections to delete because duo vpc option is disabled")
    except Exception as err:
        logging.info("Unable to remove peering connection: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to remove peering connection",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_sgroups(tag_value):
    try:
        ec2 = boto3.resource('ec2')
        client = boto3.client('ec2')
        tag_name = os.environ['conf_service_base_name']
        sgs = ec2.security_groups.filter(
            Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': [tag_value]}])
        if sgs:
            for sg in sgs:
                client.delete_security_group(GroupId=sg.id)
                print("The security group {} has been deleted successfully".format(sg.id))
        else:
            print("There are no security groups to delete")
    except Exception as err:
        logging.info("Unable to remove SG: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove SG",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def add_inbound_sg_rule(sg_id, rule):
    try:
        client = boto3.client('ec2')
        client.authorize_security_group_ingress(
            GroupId=sg_id,
            IpPermissions=[rule]
        )
    except Exception as err:
        if err.response['Error']['Code'] == 'InvalidPermission.Duplicate':
            print("The following inbound rule is already exist:")
            print(str(rule))
        else:
            logging.info("Unable to add inbound rule to SG: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Unable to add inbound rule to SG",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)


def add_outbound_sg_rule(sg_id, rule):
    try:
        client = boto3.client('ec2')
        client.authorize_security_group_egress(
            GroupId=sg_id,
            IpPermissions=[rule]
        )
    except Exception as err:
        if err.response['Error']['Code'] == 'InvalidPermission.Duplicate':
            print("The following outbound rule is already exist:")
            print(str(rule))
        else:
            logging.info("Unable to add outbound rule to SG: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Unable to add outbound rule to SG",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)


def deregister_image(image_name='*'):
    try:
        resource = boto3.resource('ec2')
        client = boto3.client('ec2')
        for image in resource.images.filter(
                Filters=[{'Name': 'tag-value', 'Values': [os.environ['conf_service_base_name']]},
                         {'Name': 'tag-value', 'Values': [image_name]}]):
            client.deregister_image(ImageId=image.id)
            for device in image.block_device_mappings:
                if device.get('Ebs'):
                    client.delete_snapshot(SnapshotId=device.get('Ebs').get('SnapshotId'))
            print("Notebook AMI {} has been deregistered successfully".format(image.id))
    except Exception as err:
        logging.info("Unable to de-register image: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to de-register image",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def terminate_emr(id):
    try:
        emr = boto3.client('emr')
        emr.terminate_job_flows(
            JobFlowIds=[id]
        )
        waiter = emr.get_waiter('cluster_terminated')
        waiter.wait(ClusterId=id)
    except Exception as err:
        logging.info("Unable to remove EMR: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove EMR",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_kernels(emr_name, tag_name, nb_tag_value, ssh_user, key_path, emr_version, computational_name=''):
    try:
        ec2 = boto3.resource('ec2')
        inst = ec2.instances.filter(
            Filters=[{'Name': 'instance-state-name', 'Values': ['running']},
                     {'Name': 'tag:{}'.format(tag_name), 'Values': ['{}'.format(nb_tag_value)]}])
        instances = list(inst)
        if instances:
            for instance in instances:
                private = getattr(instance, 'private_dns_name')
                env.hosts = "{}".format(private)
                env.user = "{}".format(ssh_user)
                env.key_filename = "{}".format(key_path)
                env.host_string = env.user + "@" + env.hosts
                sudo('rm -rf /home/{}/.local/share/jupyter/kernels/*_{}'.format(ssh_user, emr_name))
                if exists('/home/{}/.ensure_dir/dataengine-service_{}_interpreter_ensured'.format(ssh_user, emr_name)):
                    if os.environ['notebook_multiple_clusters'] == 'true':
                        try:
                            livy_port = sudo("cat /opt/" + emr_version + "/" + emr_name +
                                             "/livy/conf/livy.conf | grep livy.server.port | tail -n 1 | "
                                             "awk '{printf $3}'")
                            process_number = sudo("netstat -natp 2>/dev/null | grep ':" + livy_port +
                                                  "' | awk '{print $7}' | sed 's|/.*||g'")
                            sudo('kill -9 ' + process_number)
                            sudo('systemctl disable livy-server-' + livy_port)
                        except:
                            print("Wasn't able to find Livy server for this EMR!")
                    sudo('sed -i \"s/^export SPARK_HOME.*/export SPARK_HOME=\/opt\/spark/\" '
                         '/opt/zeppelin/conf/zeppelin-env.sh')
                    sudo("rm -rf /home/{}/.ensure_dir/dataengine-service_interpreter_ensure".format(ssh_user))
                    zeppelin_url = 'http://' + private + ':8080/api/interpreter/setting/'
                    opener = urllib2.build_opener(urllib2.ProxyHandler({}))
                    req = opener.open(urllib2.Request(zeppelin_url))
                    r_text = req.read()
                    interpreter_json = json.loads(r_text)
                    interpreter_prefix = emr_name
                    for interpreter in interpreter_json['body']:
                        if interpreter_prefix in interpreter['name']:
                            print("Interpreter with ID: {0} and name: {1} will be removed from zeppelin!".
                                  format(interpreter['id'], interpreter['name']))
                            request = urllib2.Request(zeppelin_url + interpreter['id'], data='')
                            request.get_method = lambda: 'DELETE'
                            url = opener.open(request)
                            print(url.read())
                    sudo('chown ' + ssh_user + ':' + ssh_user + ' -R /opt/zeppelin/')
                    sudo('systemctl daemon-reload')
                    sudo("service zeppelin-notebook stop")
                    sudo("service zeppelin-notebook start")
                    zeppelin_restarted = False
                    while not zeppelin_restarted:
                        sudo('sleep 5')
                        result = sudo('nmap -p 8080 localhost | grep "closed" > /dev/null; echo $?')
                        result = result[:1]
                        if result == '1':
                            zeppelin_restarted = True
                    sudo('sleep 5')
                    sudo('rm -rf /home/{}/.ensure_dir/dataengine-service_{}_interpreter_ensured'.format(ssh_user,
                                                                                                        emr_name))
                if exists('/home/{}/.ensure_dir/rstudio_dataengine-service_ensured'.format(ssh_user)):
                    dlab.fab.remove_rstudio_dataengines_kernel(computational_name, ssh_user)
                sudo('rm -rf  /opt/' + emr_version + '/' + emr_name + '/')
                print("Notebook's {} kernels were removed".format(env.hosts))
        else:
            print("There are no notebooks to clean kernels.")
    except Exception as err:
        logging.info("Unable to remove kernels on Notebook: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to remove kernels on Notebook",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_route_tables(tag_name, ssn=False):
    try:
        client = boto3.client('ec2')
        rtables = client.describe_route_tables(Filters=[{'Name': 'tag-key', 'Values': [tag_name]}]).get('RouteTables')
        for rtable in rtables:
            if rtable:
                rtable_associations = rtable.get('Associations')
                rtable = rtable.get('RouteTableId')
                if ssn:
                    for association in rtable_associations:
                        client.disassociate_route_table(AssociationId=association.get('RouteTableAssociationId'))
                        print("Association {} has been removed".format(association.get('RouteTableAssociationId')))
                client.delete_route_table(RouteTableId=rtable)
                print("Route table {} has been removed".format(rtable))
            else:
                print("There are no route tables to remove")
    except Exception as err:
        logging.info("Unable to remove route table: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to remove route table",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_internet_gateways(vpc_id, tag_name, tag_value):
    try:
        ig_id = ''
        client = boto3.client('ec2')
        response = client.describe_internet_gateways(
            Filters=[
                {'Name': 'tag-key', 'Values': [tag_name]},
                {'Name': 'tag-value', 'Values': [tag_value]}]).get('InternetGateways')
        for i in response:
            ig_id = i.get('InternetGatewayId')
        client.detach_internet_gateway(InternetGatewayId=ig_id, VpcId=vpc_id)
        print("Internet gateway {0} has been detached from VPC {1}".format(ig_id, vpc_id.format))
        client.delete_internet_gateway(InternetGatewayId=ig_id)
        print("Internet gateway {} has been deleted successfully".format(ig_id))
    except Exception as err:
        logging.info("Unable to remove internet gateway: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to remove internet gateway",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_vpc_endpoints(vpc_id):
    try:
        client = boto3.client('ec2')
        response = client.describe_vpc_endpoints(Filters=[{'Name': 'vpc-id', 'Values': [vpc_id]}]).get('VpcEndpoints')
        for i in response:
            client.delete_vpc_endpoints(VpcEndpointIds=[i.get('VpcEndpointId')])
            print("VPC Endpoint {} has been removed successfully".format(i.get('VpcEndpointId')))
    except Exception as err:
        logging.info("Unable to remove VPC Endpoint: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to remove VPC Endpoint",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_image_from_instance(tag_name='', instance_name='', image_name='', tags=''):
    try:
        ec2 = boto3.resource('ec2')
        client = boto3.client('ec2')
        instances = ec2.instances.filter(
            Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': [instance_name]},
                     {'Name': 'instance-state-name', 'Values': ['running']}])
        for instance in instances:
            image = instance.create_image(Name=image_name,
                                          Description='Automatically created image for notebook server',
                                          NoReboot=False)
            image.load()
            while image.state != 'available':
                local("echo Waiting for image creation; sleep 20")
                image.load()
            tag = {'Key': 'Name', 'Value': image_name}
            sbn_tag = {'Key': 'SBN', 'Value': os.environ['conf_service_base_name']}
            response = client.describe_images(ImageIds=[image.id]).get('Images')[0].get('BlockDeviceMappings')
            for ebs in response:
                if ebs.get('Ebs'):
                    snapshot_id = ebs.get('Ebs').get('SnapshotId')
                    create_tag(snapshot_id, sbn_tag)
                    create_tag(snapshot_id, tag)
            create_tag(image.id, sbn_tag)
            create_tag(image.id, tag)
            if tags:
                all_tags = json.loads(tags)
                for key in all_tags.keys():
                    tag = {'Key': key, 'Value': all_tags[key]}
                    create_tag(image.id, tag)
            return image.id
        return ''
    except botocore.exceptions.ClientError as err:
        if err.response['Error']['Code'] == 'InvalidAMIName.Duplicate':
            print("Image is already created.")
        else:
            logging.info("Unable to create image: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Unable to create image",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)


def install_emr_spark(args):
    s3_client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=args.region)
    s3_client.download_file(args.bucket, args.project_name + '/' + args.cluster_name + '/spark.tar.gz',
                            '/tmp/spark.tar.gz')
    s3_client.download_file(args.bucket, args.project_name + '/' + args.cluster_name + '/spark-checksum.chk',
                            '/tmp/spark-checksum.chk')
    if 'WARNING' in local('md5sum -c /tmp/spark-checksum.chk', capture=True):
        local('rm -f /tmp/spark.tar.gz')
        s3_client.download_file(args.bucket, args.project_name + '/' + args.cluster_name + '/spark.tar.gz',
                                '/tmp/spark.tar.gz')
        if 'WARNING' in local('md5sum -c /tmp/spark-checksum.chk', capture=True):
            print("The checksum of spark.tar.gz is mismatched. It could be caused by aws network issue.")
            sys.exit(1)
    local('sudo tar -zhxvf /tmp/spark.tar.gz -C /opt/' + args.emr_version + '/' + args.cluster_name + '/')


def jars(args, emr_dir):
    print("Downloading jars...")
    s3_client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=args.region)
    s3_client.download_file(args.bucket, 'jars/' + args.emr_version + '/jars.tar.gz', '/tmp/jars.tar.gz')
    s3_client.download_file(args.bucket, 'jars/' + args.emr_version + '/jars-checksum.chk', '/tmp/jars-checksum.chk')
    if 'WARNING' in local('md5sum -c /tmp/jars-checksum.chk', capture=True):
        local('rm -f /tmp/jars.tar.gz')
        s3_client.download_file(args.bucket, 'jars/' + args.emr_version + '/jars.tar.gz', '/tmp/jars.tar.gz')
        if 'WARNING' in local('md5sum -c /tmp/jars-checksum.chk', capture=True):
            print("The checksum of jars.tar.gz is mismatched. It could be caused by aws network issue.")
            sys.exit(1)
    local('tar -zhxvf /tmp/jars.tar.gz -C ' + emr_dir)


def yarn(args, yarn_dir):
    print("Downloading yarn configuration...")
    if args.region == 'cn-north-1':
        s3client = boto3.client('s3', config=Config(signature_version='s3v4'),
                                endpoint_url='https://s3.cn-north-1.amazonaws.com.cn', region_name=args.region)
        s3resource = boto3.resource('s3', config=Config(signature_version='s3v4'),
                                    endpoint_url='https://s3.cn-north-1.amazonaws.com.cn', region_name=args.region)
    else:
        s3client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=args.region)
        s3resource = boto3.resource('s3', config=Config(signature_version='s3v4'))
    get_files(s3client, s3resource, args.project_name + '/' + args.cluster_name + '/config/', args.bucket, yarn_dir)
    local('sudo mv ' + yarn_dir + args.project_name + '/' + args.cluster_name + '/config/* ' + yarn_dir)
    local('sudo rm -rf ' + yarn_dir + args.project_name + '/')


def get_files(s3client, s3resource, dist, bucket, local):
    s3list = s3client.get_paginator('list_objects')
    for result in s3list.paginate(Bucket=bucket, Delimiter='/', Prefix=dist):
        if result.get('CommonPrefixes') is not None:
            for subdir in result.get('CommonPrefixes'):
                get_files(s3client, s3resource, subdir.get('Prefix'), bucket, local)
        if result.get('Contents') is not None:
            for file in result.get('Contents'):
                if not os.path.exists(os.path.dirname(local + os.sep + file.get('Key'))):
                    os.makedirs(os.path.dirname(local + os.sep + file.get('Key')))
                s3resource.meta.client.download_file(bucket, file.get('Key'), local + os.sep + file.get('Key'))


def get_cluster_python_version(region, bucket, user_name, cluster_name):
    s3_client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=region)
    s3_client.download_file(bucket, user_name + '/' + cluster_name + '/python_version', '/tmp/python_version')


def get_gitlab_cert(bucket, certfile):
    try:
        s3 = boto3.resource('s3')
        s3.Bucket(bucket).download_file(certfile, certfile)
        return True
    except botocore.exceptions.ClientError as err:
        if err.response['Error']['Code'] == "404":
            print("The object does not exist.")
        return False


def create_aws_config_files(generate_full_config=False):
    try:
        aws_user_dir = os.environ['AWS_DIR']
        logging.info(local("rm -rf " + aws_user_dir + " 2>&1", capture=True))
        logging.info(local("mkdir -p " + aws_user_dir + " 2>&1", capture=True))

        with open(aws_user_dir + '/config', 'w') as aws_file:
            aws_file.write("[default]\n")
            aws_file.write("region = {}\n".format(os.environ['aws_region']))

        if generate_full_config:
            with open(aws_user_dir + '/credentials', 'w') as aws_file:
                aws_file.write("[default]\n")
                aws_file.write("aws_access_key_id = {}\n".format(os.environ['aws_access_key']))
                aws_file.write("aws_secret_access_key = {}\n".format(os.environ['aws_secret_access_key']))

        logging.info(local("chmod 600 " + aws_user_dir + "/*" + " 2>&1", capture=True))
        logging.info(local("chmod 550 " + aws_user_dir + " 2>&1", capture=True))

        return True
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)


def installing_python(region, bucket, user_name, cluster_name, application='', pip_mirror='', numpy_version='1.14.3'):
    get_cluster_python_version(region, bucket, user_name, cluster_name)
    with file('/tmp/python_version') as f:
        python_version = f.read()
    python_version = python_version[0:5]
    if not os.path.exists('/opt/python/python' + python_version):
        local('wget https://www.python.org/ftp/python/' + python_version +
              '/Python-' + python_version + '.tgz -O /tmp/Python-' + python_version + '.tgz')
        local('tar zxvf /tmp/Python-' + python_version + '.tgz -C /tmp/')
        with lcd('/tmp/Python-' + python_version):
            local('./configure --prefix=/opt/python/python' + python_version +
                  ' --with-zlib-dir=/usr/local/lib/ --with-ensurepip=install')
            local('sudo make altinstall')
        with lcd('/tmp/'):
            local('sudo rm -rf Python-' + python_version + '/')
        if region == 'cn-north-1':
            local('sudo -i /opt/python/python{}/bin/python{} -m pip install -U pip=={} --no-cache-dir'.format(
                python_version, python_version[0:3], os.environ['conf_pip_version']))
            local('sudo mv /etc/pip.conf /etc/back_pip.conf')
            local('sudo touch /etc/pip.conf')
            local('sudo echo "[global]" >> /etc/pip.conf')
            local('sudo echo "timeout = 600" >> /etc/pip.conf')
        local('sudo -i virtualenv /opt/python/python' + python_version)
        venv_command = '/bin/bash /opt/python/python' + python_version + '/bin/activate'
        pip_command = '/opt/python/python' + python_version + '/bin/pip' + python_version[:3]
        if region == 'cn-north-1':
            try:
                local(venv_command + ' && sudo -i ' + pip_command +
                      ' install -i https://{0}/simple --trusted-host {0} --timeout 60000 -U pip==9.0.3 '
                      '--no-cache-dir'.format(pip_mirror))
                local(venv_command + ' && sudo -i ' + pip_command + ' install pyzmq==17.0.0')
                local(venv_command + ' && sudo -i ' + pip_command +
                      ' install -i https://{0}/simple --trusted-host {0} --timeout 60000 ipython ipykernel '
                      '--no-cache-dir'.format(pip_mirror))
                local(venv_command + ' && sudo -i ' + pip_command + ' install NumPy=={0}'.format(numpy_version))
                local(venv_command + ' && sudo -i ' + pip_command +
                      ' install -i https://{0}/simple --trusted-host {0} --timeout 60000 boto boto3 SciPy '
                      'Matplotlib==2.0.2 pandas Sympy Pillow sklearn --no-cache-dir'.format(pip_mirror))
                # Need to refactor when we add GPU cluster
                if application == 'deeplearning':
                    local(venv_command + ' && sudo -i ' + pip_command +
                          ' install -i https://{0}/simple --trusted-host {0} --timeout 60000 mxnet-cu80 opencv-python '
                          'keras Theano --no-cache-dir'.format(pip_mirror))
                    python_without_dots = python_version.replace('.', '')
                    local(venv_command + ' && sudo -i ' + pip_command +
                          ' install  https://cntk.ai/PythonWheel/GPU/cntk-2.0rc3-cp{0}-cp{0}m-linux_x86_64.whl '
                          '--no-cache-dir'.format(python_without_dots[:2]))
                local('sudo rm /etc/pip.conf')
                local('sudo mv /etc/back_pip.conf /etc/pip.conf')
            except:
                local('sudo rm /etc/pip.conf')
                local('sudo mv /etc/back_pip.conf /etc/pip.conf')
                local('sudo rm -rf /opt/python/python{}/'.format(python_version))
                sys.exit(1)
        else:
            local(venv_command + ' && sudo -i ' + pip_command + ' install -U pip==9.0.3')
            local(venv_command + ' && sudo -i ' + pip_command + ' install pyzmq==17.0.0')
            local(venv_command + ' && sudo -i ' + pip_command + ' install ipython ipykernel --no-cache-dir')
            local(venv_command + ' && sudo -i ' + pip_command + ' install NumPy=={}'.format(numpy_version))
            local(venv_command + ' && sudo -i ' + pip_command +
                  ' install boto boto3 SciPy Matplotlib==2.0.2 pandas Sympy Pillow sklearn '
                  '--no-cache-dir')
            # Need to refactor when we add GPU cluster
            if application == 'deeplearning':
                local(venv_command + ' && sudo -i ' + pip_command +
                      ' install mxnet-cu80 opencv-python keras Theano --no-cache-dir')
                python_without_dots = python_version.replace('.', '')
                local(venv_command + ' && sudo -i ' + pip_command +
                      ' install  https://cntk.ai/PythonWheel/GPU/cntk-2.0rc3-cp{0}-cp{0}m-linux_x86_64.whl '
                      '--no-cache-dir'.format(python_without_dots[:2]))
        local('sudo rm -rf /usr/bin/python{}-dp'.format(python_version[0:3]))
        local('sudo ln -fs /opt/python/python{0}/bin/python{1} /usr/bin/python{1}-dp'.format(python_version,
                                                                                             python_version[0:3]))


def spark_defaults(args):
    spark_def_path = '/opt/' + args.emr_version + '/' + args.cluster_name + '/spark/conf/spark-defaults.conf'
    for i in eval(args.excluded_lines):
        local(""" sudo bash -c " sed -i '/""" + i + """/d' """ + spark_def_path + """ " """)
    local(""" sudo bash -c " sed -i '/#/d' """ + spark_def_path + """ " """)
    local(""" sudo bash -c " sed -i '/^\s*$/d' """ + spark_def_path + """ " """)
    local(""" sudo bash -c "sed -i '/spark.driver.extraClassPath/,/spark.driver.extraLibraryPath/s|"""
          """/usr|/opt/DATAENGINE-SERVICE_VERSION/jars/usr|g' """ + spark_def_path + """ " """)
    local(
        """ sudo bash -c "sed -i '/spark.yarn.dist.files/s/\/etc\/spark\/conf/\/opt\/DATAENGINE-SERVICE_VERSION\/CLUSTER\/conf/g' """
        + spark_def_path + """ " """)
    template_file = spark_def_path
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('DATAENGINE-SERVICE_VERSION', args.emr_version)
    text = text.replace('CLUSTER', args.cluster_name)
    with open(spark_def_path, 'w') as f:
        f.write(text)
    if args.region == 'us-east-1':
        endpoint_url = 'https://s3.amazonaws.com'
    elif args.region == 'cn-north-1':
        endpoint_url = "https://s3.{}.amazonaws.com.cn".format(args.region)
    else:
        endpoint_url = 'https://s3-' + args.region + '.amazonaws.com'
    local("""bash -c 'echo "spark.hadoop.fs.s3a.endpoint    """ + endpoint_url + """ " >> """ +
          spark_def_path + """'""")
    local('echo "spark.hadoop.fs.s3a.server-side-encryption-algorithm   AES256" >> {}'.format(spark_def_path))


def ensure_local_jars(os_user, jars_dir):
    if not exists('/home/{}/.ensure_dir/local_jars_ensured'.format(os_user)):
        try:
            sudo('mkdir -p {0}'.format(jars_dir))
            sudo('wget https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/{0}/hadoop-aws-{0}.jar -O \
                    {1}hadoop-aws-{0}.jar'.format('2.7.4', jars_dir))
            sudo('wget https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk/{0}/aws-java-sdk-{0}.jar -O \
                    {1}aws-java-sdk-{0}.jar'.format('1.7.4', jars_dir))
            # sudo('wget https://maven.twttr.com/com/hadoop/gplcompression/hadoop-lzo/{0}/hadoop-lzo-{0}.jar -O \
            #         {1}hadoop-lzo-{0}.jar'.format('0.4.20', jars_dir))
            sudo('touch /home/{}/.ensure_dir/local_jars_ensured'.format(os_user))
        except:
            sys.exit(1)


def configure_local_spark(jars_dir, templates_dir, memory_type='driver'):
    try:
        # Checking if spark.jars parameter was generated previously
        spark_jars_paths = None
        if exists('/opt/spark/conf/spark-defaults.conf'):
            try:
                spark_jars_paths = sudo('cat /opt/spark/conf/spark-defaults.conf | grep -e "^spark.jars " ')
            except:
                spark_jars_paths = None
        region = sudo('curl http://169.254.169.254/latest/meta-data/placement/availability-zone')[:-1]
        if region == 'us-east-1':
            endpoint_url = 'https://s3.amazonaws.com'
        elif region == 'cn-north-1':
            endpoint_url = "https://s3.{}.amazonaws.com.cn".format(region)
        else:
            endpoint_url = 'https://s3-' + region + '.amazonaws.com'
        put(templates_dir + 'notebook_spark-defaults_local.conf', '/tmp/notebook_spark-defaults_local.conf')
        sudo('echo "spark.hadoop.fs.s3a.endpoint     {}" >> /tmp/notebook_spark-defaults_local.conf'.format(
            endpoint_url))
        sudo('echo "spark.hadoop.fs.s3a.server-side-encryption-algorithm   AES256" >> '
             '/tmp/notebook_spark-defaults_local.conf')
        if os.environ['application'] == 'zeppelin':
            sudo('echo \"spark.jars $(ls -1 ' + jars_dir + '* | tr \'\\n\' \',\')\" >> '
                                                           '/tmp/notebook_spark-defaults_local.conf')
        sudo('\cp -f /tmp/notebook_spark-defaults_local.conf /opt/spark/conf/spark-defaults.conf')
        if memory_type == 'driver':
            spark_memory = dlab.fab.get_spark_memory()
            sudo('sed -i "/spark.*.memory/d" /opt/spark/conf/spark-defaults.conf')
            sudo('echo "spark.{0}.memory {1}m" >> /opt/spark/conf/spark-defaults.conf'.format(memory_type,
                                                                                              spark_memory))
        if 'spark_configurations' in os.environ:
            dlab_header = sudo('cat /tmp/notebook_spark-defaults_local.conf | grep "^#"')
            spark_configurations = ast.literal_eval(os.environ['spark_configurations'])
            new_spark_defaults = list()
            spark_defaults = sudo('cat /opt/spark/conf/spark-defaults.conf')
            current_spark_properties = spark_defaults.split('\n')
            for param in current_spark_properties:
                if param.split(' ')[0] != '#':
                    for config in spark_configurations:
                        if config['Classification'] == 'spark-defaults':
                            for property in config['Properties']:
                                if property == param.split(' ')[0]:
                                    param = property + ' ' + config['Properties'][property]
                                else:
                                    new_spark_defaults.append(property + ' ' + config['Properties'][property])
                    new_spark_defaults.append(param)
            new_spark_defaults = set(new_spark_defaults)
            sudo("echo '{}' > /opt/spark/conf/spark-defaults.conf".format(dlab_header))
            for prop in new_spark_defaults:
                prop = prop.rstrip()
                sudo('echo "{}" >> /opt/spark/conf/spark-defaults.conf'.format(prop))
            sudo('sed -i "/^\s*$/d" /opt/spark/conf/spark-defaults.conf')
            if spark_jars_paths:
                sudo('echo "{}" >> /opt/spark/conf/spark-defaults.conf'.format(spark_jars_paths))
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)


def configure_zeppelin_emr_interpreter(emr_version, cluster_name, region, spark_dir, os_user, yarn_dir, bucket,
                                       user_name, endpoint_url, multiple_emrs):
    try:
        port_number_found = False
        zeppelin_restarted = False
        default_port = 8998
        get_cluster_python_version(region, bucket, user_name, cluster_name)
        with file('/tmp/python_version') as f:
            python_version = f.read()
        python_version = python_version[0:5]
        livy_port = ''
        livy_path = '/opt/{0}/{1}/livy/'.format(emr_version, cluster_name)
        spark_libs = "/opt/{0}/jars/usr/share/aws/aws-java-sdk/aws-java-sdk-core*.jar /opt/{0}/jars/usr/lib/hadoop" \
                     "/hadoop-aws*.jar /opt/" + \
                     "{0}/jars/usr/share/aws/aws-java-sdk/aws-java-sdk-s3-*.jar /opt/{0}" + \
                     "/jars/usr/lib/hadoop-lzo/lib/hadoop-lzo-*.jar".format(emr_version)
        # fix due to: Multiple py4j files found under ..../spark/python/lib
        # py4j-0.10.7-src.zip still in folder. Versions may varies.
        local('rm /opt/{0}/{1}/spark/python/lib/py4j-src.zip'.format(emr_version, cluster_name))

        local('echo \"Configuring emr path for Zeppelin\"')
        local('sed -i \"s/^export SPARK_HOME.*/export SPARK_HOME=\/opt\/{0}\/{1}\/spark/\" '
              '/opt/zeppelin/conf/zeppelin-env.sh'.format(emr_version, cluster_name))
        local('sed -i "s/^export HADOOP_CONF_DIR.*/export HADOOP_CONF_DIR=' + \
              '\/opt\/{0}\/{1}\/conf/" /opt/{0}/{1}/spark/conf/spark-env.sh'.format(emr_version, cluster_name))
        local('echo \"spark.jars $(ls {0} | tr \'\\n\' \',\')\" >> /opt/{1}/{2}/spark/conf/spark-defaults.conf'
              .format(spark_libs, emr_version, cluster_name))
        local('sed -i "/spark.executorEnv.PYTHONPATH/d" /opt/{0}/{1}/spark/conf/spark-defaults.conf'
              .format(emr_version, cluster_name))
        local('sed -i "/spark.yarn.dist.files/d" /opt/{0}/{1}/spark/conf/spark-defaults.conf'
              .format(emr_version, cluster_name))
        local('sudo chown {0}:{0} -R /opt/zeppelin/'.format(os_user))
        local('sudo systemctl daemon-reload')
        local('sudo service zeppelin-notebook stop')
        local('sudo service zeppelin-notebook start')
        while not zeppelin_restarted:
            local('sleep 5')
            result = local('sudo bash -c "nmap -p 8080 localhost | grep closed > /dev/null" ; echo $?', capture=True)
            result = result[:1]
            if result == '1':
                zeppelin_restarted = True
        local('sleep 5')
        local('echo \"Configuring emr spark interpreter for Zeppelin\"')
        if multiple_emrs == 'true':
            while not port_number_found:
                port_free = local('sudo bash -c "nmap -p ' + str(default_port) +
                                  ' localhost | grep closed > /dev/null" ; echo $?', capture=True)
                port_free = port_free[:1]
                if port_free == '0':
                    livy_port = default_port
                    port_number_found = True
                else:
                    default_port += 1
            local('sudo echo "livy.server.port = {0}" >> {1}conf/livy.conf'.format(str(livy_port), livy_path))
            local('sudo echo "livy.spark.master = yarn" >> {}conf/livy.conf'.format(livy_path))
            if os.path.exists('{}conf/spark-blacklist.conf'.format(livy_path)):
                local('sudo sed -i "s/^/#/g" {}conf/spark-blacklist.conf'.format(livy_path))
            local(''' sudo echo "export SPARK_HOME={0}" >> {1}conf/livy-env.sh'''.format(spark_dir, livy_path))
            local(''' sudo echo "export HADOOP_CONF_DIR={0}" >> {1}conf/livy-env.sh'''.format(yarn_dir, livy_path))
            local(''' sudo echo "export PYSPARK3_PYTHON=python{0}" >> {1}conf/livy-env.sh'''.format(python_version[0:3],
                                                                                                    livy_path))
            template_file = "/tmp/dataengine-service_interpreter.json"
            fr = open(template_file, 'r+')
            text = fr.read()
            text = text.replace('CLUSTER_NAME', cluster_name)
            text = text.replace('SPARK_HOME', spark_dir)
            text = text.replace('ENDPOINTURL', endpoint_url)
            text = text.replace('LIVY_PORT', str(livy_port))
            fw = open(template_file, 'w')
            fw.write(text)
            fw.close()
            for _ in range(5):
                try:
                    local("curl --noproxy localhost -H 'Content-Type: application/json' -X POST -d " +
                          "@/tmp/dataengine-service_interpreter.json http://localhost:8080/api/interpreter/setting")
                    break
                except:
                    local('sleep 5')
            local('sudo cp /opt/livy-server-cluster.service /etc/systemd/system/livy-server-{}.service'
                  .format(str(livy_port)))
            local("sudo sed -i 's|OS_USER|{0}|' /etc/systemd/system/livy-server-{1}.service"
                  .format(os_user, str(livy_port)))
            local("sudo sed -i 's|LIVY_PATH|{0}|' /etc/systemd/system/livy-server-{1}.service"
                  .format(livy_path, str(livy_port)))
            local('sudo chmod 644 /etc/systemd/system/livy-server-{}.service'.format(str(livy_port)))
            local("sudo systemctl daemon-reload")
            local("sudo systemctl enable livy-server-{}".format(str(livy_port)))
            local('sudo systemctl start livy-server-{}'.format(str(livy_port)))
        else:
            template_file = "/tmp/dataengine-service_interpreter.json"
            p_versions = ["2", "{}-dp".format(python_version[:3])]
            for p_version in p_versions:
                fr = open(template_file, 'r+')
                text = fr.read()
                text = text.replace('CLUSTERNAME', cluster_name)
                text = text.replace('PYTHONVERSION', p_version)
                text = text.replace('SPARK_HOME', spark_dir)
                text = text.replace('PYTHONVER_SHORT', p_version[:1])
                text = text.replace('ENDPOINTURL', endpoint_url)
                text = text.replace('DATAENGINE-SERVICE_VERSION', emr_version)
                tmp_file = "/tmp/emr_spark_py{}_interpreter.json".format(p_version)
                fw = open(tmp_file, 'w')
                fw.write(text)
                fw.close()
                for _ in range(5):
                    try:
                        local("curl --noproxy localhost -H 'Content-Type: application/json' -X POST -d " +
                              "@/tmp/emr_spark_py" + p_version +
                              "_interpreter.json http://localhost:8080/api/interpreter/setting")
                        break
                    except:
                        local('sleep 5')
        local('touch /home/' + os_user + '/.ensure_dir/dataengine-service_' + cluster_name + '_interpreter_ensured')
    except:
        sys.exit(1)


def configure_dataengine_spark(cluster_name, jars_dir, cluster_dir, datalake_enabled, spark_configs=''):
    local("jar_list=`find {0} -name '*.jar' | tr '\\n' ',' | sed 's/,$//'` ; echo \"spark.jars $jar_list\" >> \
          /tmp/{1}/notebook_spark-defaults_local.conf".format(jars_dir, cluster_name))
    region = local('curl http://169.254.169.254/latest/meta-data/placement/availability-zone', capture=True)[:-1]
    if region == 'us-east-1':
        endpoint_url = 'https://s3.amazonaws.com'
    elif region == 'cn-north-1':
        endpoint_url = "https://s3.{}.amazonaws.com.cn".format(region)
    else:
        endpoint_url = 'https://s3-' + region + '.amazonaws.com'
    local("""bash -c 'echo "spark.hadoop.fs.s3a.endpoint    """ + endpoint_url +
          """" >> /tmp/{}/notebook_spark-defaults_local.conf'""".format(cluster_name))
    local('echo "spark.hadoop.fs.s3a.server-side-encryption-algorithm   AES256" >> '
          '/tmp/{}/notebook_spark-defaults_local.conf'.format(cluster_name))
    if os.path.exists('{0}spark/conf/spark-defaults.conf'.format(cluster_dir)):
        additional_spark_properties = local('diff --changed-group-format="%>" --unchanged-group-format="" '
                                            '/tmp/{0}/notebook_spark-defaults_local.conf '
                                            '{1}spark/conf/spark-defaults.conf | grep -v "^#"'.format(
            cluster_name, cluster_dir), capture=True)
        for property in additional_spark_properties.split('\n'):
            local('echo "{0}" >> /tmp/{1}/notebook_spark-defaults_local.conf'.format(property, cluster_name))
    local('cp -f /tmp/{0}/notebook_spark-defaults_local.conf  {1}spark/conf/spark-defaults.conf'.format(cluster_name,
                                                                                                        cluster_dir))
    if spark_configs:
        dlab_header = local('cat /tmp/{0}/notebook_spark-defaults_local.conf | grep "^#"'.format(cluster_name),
                            capture=True)
        spark_configurations = ast.literal_eval(spark_configs)
        new_spark_defaults = list()
        spark_defaults = local('cat {0}spark/conf/spark-defaults.conf'.format(cluster_dir), capture=True)
        current_spark_properties = spark_defaults.split('\n')
        for param in current_spark_properties:
            if param.split(' ')[0] != '#':
                for config in spark_configurations:
                    if config['Classification'] == 'spark-defaults':
                        for property in config['Properties']:
                            if property == param.split(' ')[0]:
                                param = property + ' ' + config['Properties'][property]
                            else:
                                new_spark_defaults.append(property + ' ' + config['Properties'][property])
                new_spark_defaults.append(param)
        new_spark_defaults = set(new_spark_defaults)
        local("echo '{0}' > {1}/spark/conf/spark-defaults.conf".format(dlab_header, cluster_dir))
        for prop in new_spark_defaults:
            prop = prop.rstrip()
            local('echo "{0}" >> {1}/spark/conf/spark-defaults.conf'.format(prop, cluster_dir))
        local('sed -i "/^\s*$/d" {0}/spark/conf/spark-defaults.conf'.format(cluster_dir))


def remove_dataengine_kernels(tag_name, notebook_name, os_user, key_path, cluster_name):
    try:
        private = meta_lib.get_instance_private_ip_address(tag_name, notebook_name)
        env.hosts = "{}".format(private)
        env.user = "{}".format(os_user)
        env.key_filename = "{}".format(key_path)
        env.host_string = env.user + "@" + env.hosts
        sudo('rm -rf /home/{}/.local/share/jupyter/kernels/*_{}'.format(os_user, cluster_name))
        if exists('/home/{}/.ensure_dir/dataengine_{}_interpreter_ensured'.format(os_user, cluster_name)):
            if os.environ['notebook_multiple_clusters'] == 'true':
                try:
                    livy_port = sudo("cat /opt/" + cluster_name +
                                     "/livy/conf/livy.conf | grep livy.server.port | tail -n 1 | awk '{printf $3}'")
                    process_number = sudo("netstat -natp 2>/dev/null | grep ':" + livy_port +
                                          "' | awk '{print $7}' | sed 's|/.*||g'")
                    sudo('kill -9 ' + process_number)
                    sudo('systemctl disable livy-server-' + livy_port)
                except:
                    print("Wasn't able to find Livy server for this EMR!")
            sudo(
                'sed -i \"s/^export SPARK_HOME.*/export SPARK_HOME=\/opt\/spark/\" /opt/zeppelin/conf/zeppelin-env.sh')
            sudo("rm -rf /home/{}/.ensure_dir/dataengine_interpreter_ensure".format(os_user))
            zeppelin_url = 'http://' + private + ':8080/api/interpreter/setting/'
            opener = urllib2.build_opener(urllib2.ProxyHandler({}))
            req = opener.open(urllib2.Request(zeppelin_url))
            r_text = req.read()
            interpreter_json = json.loads(r_text)
            interpreter_prefix = cluster_name
            for interpreter in interpreter_json['body']:
                if interpreter_prefix in interpreter['name']:
                    print("Interpreter with ID: {} and name: {} will be removed from zeppelin!".format(
                        interpreter['id'], interpreter['name']))
                    request = urllib2.Request(zeppelin_url + interpreter['id'], data='')
                    request.get_method = lambda: 'DELETE'
                    url = opener.open(request)
                    print(url.read())
            sudo('chown ' + os_user + ':' + os_user + ' -R /opt/zeppelin/')
            sudo('systemctl daemon-reload')
            sudo("service zeppelin-notebook stop")
            sudo("service zeppelin-notebook start")
            zeppelin_restarted = False
            while not zeppelin_restarted:
                sudo('sleep 5')
                result = sudo('nmap -p 8080 localhost | grep "closed" > /dev/null; echo $?')
                result = result[:1]
                if result == '1':
                    zeppelin_restarted = True
            sudo('sleep 5')
            sudo('rm -rf /home/{}/.ensure_dir/dataengine_{}_interpreter_ensured'.format(os_user, cluster_name))
        if exists('/home/{}/.ensure_dir/rstudio_dataengine_ensured'.format(os_user)):
            dlab.fab.remove_rstudio_dataengines_kernel(os.environ['computational_name'], os_user)
        sudo('rm -rf  /opt/' + cluster_name + '/')
        print("Notebook's {} kernels were removed".format(env.hosts))
    except Exception as err:
        logging.info("Unable to remove kernels on Notebook: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to remove kernels on Notebook",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def prepare_disk(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/disk_ensured'):
        try:
            disk_name = sudo("lsblk | grep disk | awk '{print $1}' | sort | tail -n 1")
            sudo('''bash -c 'echo -e "o\nn\np\n1\n\n\nw" | fdisk /dev/{}' '''.format(disk_name))
            sudo('mkfs.ext4 -F /dev/{}1'.format(disk_name))
            sudo('mount /dev/{}1 /opt/'.format(disk_name))
            sudo(''' bash -c "echo '/dev/{}1 /opt/ ext4 errors=remount-ro 0 1' >> /etc/fstab" '''.format(disk_name))
            sudo('touch /home/' + os_user + '/.ensure_dir/disk_ensured')
        except:
            sys.exit(1)


def ensure_local_spark(os_user, spark_link, spark_version, hadoop_version, local_spark_path):
    if not exists('/home/' + os_user + '/.ensure_dir/local_spark_ensured'):
        try:
            sudo('wget ' + spark_link + ' -O /tmp/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz')
            sudo('tar -zxvf /tmp/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz -C /opt/')
            sudo('mv /opt/spark-' + spark_version + '-bin-hadoop' + hadoop_version + ' ' + local_spark_path)
            sudo('chown -R ' + os_user + ':' + os_user + ' ' + local_spark_path)
            sudo('touch /home/' + os_user + '/.ensure_dir/local_spark_ensured')
        except Exception as err:
            print('Error:', str(err))
            sys.exit(1)


def install_dataengine_spark(cluster_name, spark_link, spark_version, hadoop_version, cluster_dir, os_user,
                             datalake_enabled):
    local('wget ' + spark_link + ' -O /tmp/' + cluster_name + '/spark-' + spark_version + '-bin-hadoop' +
          hadoop_version + '.tgz')
    local('tar -zxvf /tmp/' + cluster_name + '/spark-' + spark_version + '-bin-hadoop' + hadoop_version +
          '.tgz -C /opt/' + cluster_name)
    local('mv /opt/' + cluster_name + '/spark-' + spark_version + '-bin-hadoop' + hadoop_version + ' ' +
          cluster_dir + 'spark/')
    local('chown -R ' + os_user + ':' + os_user + ' ' + cluster_dir + 'spark/')


def find_des_jars(all_jars, des_path):
    try:
        default_jars = ['hadoop-aws', 'hadoop-lzo', 'aws-java-sdk']
        for i in default_jars:
            for j in all_jars:
                if i in j:
                    print('Remove default cloud jar: {0}'.format(j))
                    all_jars.remove(j)
        additional_jars = ['hadoop-aws', 'aws-java-sdk-s3', 'hadoop-lzo', 'aws-java-sdk-core']
        aws_filter = '\|'.join(additional_jars)
        aws_jars = sudo('find {0} -name *.jar | grep "{1}"'.format(des_path, aws_filter)).split('\r\n')
        all_jars.extend(aws_jars)
        return all_jars
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)