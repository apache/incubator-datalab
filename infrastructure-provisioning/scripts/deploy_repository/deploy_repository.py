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
import json
import random
import string
import sys
import time
import traceback
from ConfigParser import ConfigParser
from fabric import *
from patchwork.files import exists
from patchwork import files

parser = argparse.ArgumentParser()
parser.add_argument('--service_base_name', required=True, type=str, default='',
                    help='unique name for repository environment')
parser.add_argument('--aws_access_key', type=str, default='', help='AWS Access Key ID')
parser.add_argument('--aws_secret_access_key', type=str, default='', help='AWS Secret Access Key')
parser.add_argument('--vpc_id', type=str, default='', help='AWS VPC ID')
parser.add_argument('--vpc_cidr', type=str, default='172.31.0.0/16', help='Cidr of VPC')
parser.add_argument('--subnet_id', type=str, default='', help='AWS Subnet ID')
parser.add_argument('--subnet_cidr', type=str, default='172.31.0.0/24', help='Cidr of subnet')
parser.add_argument('--sg_id', type=str, default='', help='AWS VPC ID')
parser.add_argument('--billing_tag', type=str, default='product:datalab', help='Tag in format: "Key1:Value1"')
parser.add_argument('--additional_tags', type=str, default='', help='Tags in format: "Key1:Value1;Key2:Value2"')
parser.add_argument('--tag_resource_id', type=str, default='datalab', help='The name of user tag')
parser.add_argument('--allowed_ip_cidr', type=str, default='', help='Comma-separated CIDR of IPs which will have '
                                                                    'access to the instance')
parser.add_argument('--key_name', type=str, default='', help='Key name (WITHOUT ".pem")')
parser.add_argument('--key_path', type=str, default='', help='Key path')
parser.add_argument('--instance_type', type=str, default='t2.medium', help='Instance shape')
parser.add_argument('--region', required=True, type=str, default='', help='AWS region name')
parser.add_argument('--elastic_ip', type=str, default='', help='Elastic IP address')
parser.add_argument('--network_type', type=str, default='public', help='Network type: public or private')
parser.add_argument('--hosted_zone_name', type=str, default='', help='Name of hosted zone')
parser.add_argument('--hosted_zone_id', type=str, default='', help='ID of hosted zone')
parser.add_argument('--subdomain', type=str, default='', help='Subdomain name')
parser.add_argument('--efs_enabled', type=str, default='False', help="True - use AWS EFS, False - don't use AWS EFS")
parser.add_argument('--efs_id', type=str, default='', help="ID of AWS EFS")
parser.add_argument('--primary_disk_size', type=str, default='30', help="Disk size of primary volume")
parser.add_argument('--additional_disk_size', type=str, default='50', help="Disk size of additional volume")
parser.add_argument('--datalab_conf_file_path', type=str, default='', help="Full path to DataLab conf file")
parser.add_argument('--nexus_admin_password', type=str, default='', help="Password for Nexus admin user")
parser.add_argument('--nexus_service_user_name', type=str, default='datalab-nexus', help="Nexus service user name")
parser.add_argument('--nexus_service_user_password', type=str, default='', help="Nexus service user password")
parser.add_argument('--action', required=True, type=str, default='', help='Action: create or terminate')
args = parser.parse_args()


def id_generator(size=10, with_digits=True):
    if with_digits:
        chars = string.digits + string.ascii_letters
    else:
        chars = string.ascii_letters
    return ''.join(random.choice(chars) for _ in range(size))


def vpc_exist(return_id=False):
    try:
        vpc_created = False
        for vpc in ec2_resource.vpcs.filter(Filters=[{'Name': 'tag-key', 'Values': [tag_name]},
                                                     {'Name': 'tag-value', 'Values': [args.service_base_name]}]):
            if return_id:
                return vpc.id
            vpc_created = True
        return vpc_created
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with getting AWS VPC: {}'.format(str(err)))
        raise Exception


def create_vpc(vpc_cidr):
    try:
        tag = {"Key": tag_name, "Value": args.service_base_name}
        name_tag = {"Key": "Name", "Value": args.service_base_name + '-vpc'}
        vpc = ec2_resource.create_vpc(CidrBlock=vpc_cidr)
        create_tag(vpc.id, tag)
        create_tag(vpc.id, name_tag)
        return vpc.id
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with creating AWS VPC: {}'.format(str(err)))
        raise Exception


def enable_vpc_dns(vpc_id):
    try:
        ec2_client.modify_vpc_attribute(VpcId=vpc_id,
                                        EnableDnsHostnames={'Value': True})
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with modifying AWS VPC attributes: {}'.format(str(err)))
        raise Exception


def create_rt(vpc_id):
    try:
        tag = {"Key": tag_name, "Value": args.service_base_name}
        name_tag = {"Key": "Name", "Value": args.service_base_name + '-rt'}
        route_table = []
        rt = ec2_client.create_route_table(VpcId=vpc_id)
        rt_id = rt.get('RouteTable').get('RouteTableId')
        route_table.append(rt_id)
        print('Created AWS Route-Table with ID: {}'.format(rt_id))
        create_tag(route_table, json.dumps(tag))
        create_tag(route_table, json.dumps(name_tag))
        ig = ec2_client.create_internet_gateway()
        ig_id = ig.get('InternetGateway').get('InternetGatewayId')
        route_table = list()
        route_table.append(ig_id)
        create_tag(route_table, json.dumps(tag))
        create_tag(route_table, json.dumps(name_tag))
        ec2_client.attach_internet_gateway(InternetGatewayId=ig_id, VpcId=vpc_id)
        ec2_client.create_route(DestinationCidrBlock='0.0.0.0/0', RouteTableId=rt_id, GatewayId=ig_id)
        return rt_id
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with creating AWS Route Table: {}'.format(str(err)))
        raise Exception


def remove_vpc(vpc_id):
    try:
        ec2_client.delete_vpc(VpcId=vpc_id)
        print("AWS VPC {} has been removed".format(vpc_id))
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with removing AWS VPC: {}'.format(str(err)))
        raise Exception


def create_tag(resource, tag, with_tag_res_id=True):
    try:
        tags_list = list()
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
                    'Key': args.tag_resource_id,
                    'Value': args.service_base_name + ':' + resource_name
                }
            )
            tags_list.append(
                {
                    'Key': args.billing_tag.split(':')[0],
                    'Value': args.billing_tag.split(':')[1]
                }
            )
        if args.additional_tags:
            for tag in args.additional_tags.split(';'):
                tags_list.append(
                    {
                        'Key': tag.split(':')[0],
                        'Value': tag.split(':')[1]
                    }
                )
        ec2_client.create_tags(
            Resources=resource,
            Tags=tags_list
        )
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with setting tag: {}'.format(str(err)))
        raise Exception


def create_efs_tag():
    try:
        tag = {"Key": tag_name, "Value": args.service_base_name}
        name_tag = {"Key": "Name", "Value": args.service_base_name + '-efs'}
        efs_client.create_tags(
            FileSystemId=args.efs_id,
            Tags=[
                tag,
                name_tag
            ]
        )
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with setting EFS tag: {}'.format(str(err)))
        raise Exception


def create_subnet(vpc_id, subnet_cidr):
    try:
        tag = {"Key": tag_name, "Value": "{}".format(args.service_base_name)}
        name_tag = {"Key": "Name", "Value": "{}-subnet".format(args.service_base_name)}
        subnet = ec2_resource.create_subnet(VpcId=vpc_id, CidrBlock=subnet_cidr)
        create_tag(subnet.id, tag)
        create_tag(subnet.id, name_tag)
        subnet.reload()
        print('AWS Subnet has been created')
        return subnet.id
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with creating AWS Subnet: {}'.format(str(err)))
        raise Exception


def remove_subnet():
    try:
        subnets = ec2_resource.subnets.filter(
            Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': [args.service_base_name]}])
        if subnets:
            for subnet in subnets:
                ec2_client.delete_subnet(SubnetId=subnet.id)
                print("The AWS subnet {} has been deleted successfully".format(subnet.id))
        else:
            print("There are no private AWS subnets to delete")
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with removing AWS Subnet: {}'.format(str(err)))
        raise Exception


def get_route_table_by_tag(tag_value):
    try:
        route_tables = ec2_client.describe_route_tables(
            Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': ['{}'.format(tag_value)]}])
        rt_id = route_tables.get('RouteTables')[0].get('RouteTableId')
        return rt_id
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with getting AWS Route tables: {}'.format(str(err)))
        raise Exception


def create_security_group(security_group_name, vpc_id, ingress, egress, tag, name_tag):
    try:
        group = ec2_resource.create_security_group(GroupName=security_group_name, Description='security_group_name',
                                                   VpcId=vpc_id)
        time.sleep(10)
        create_tag(group.id, tag)
        create_tag(group.id, name_tag)
        try:
            group.revoke_egress(IpPermissions=[{"IpProtocol": "-1", "IpRanges": [{"CidrIp": "0.0.0.0/0"}],
                                                "UserIdGroupPairs": [], "PrefixListIds": []}])
        except:
            print("Mentioned rule does not exist")
        for rule in ingress:
            group.authorize_ingress(IpPermissions=[rule])
        for rule in egress:
            group.authorize_egress(IpPermissions=[rule])
        return group.id
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with creating AWS security group: {}'.format(str(err)))
        raise Exception


def get_vpc_cidr_by_id(vpc_id):
    try:
        cidr_list = list()
        for vpc in ec2_client.describe_vpcs(VpcIds=[vpc_id]).get('Vpcs'):
            for cidr_set in vpc.get('CidrBlockAssociationSet'):
                cidr_list.append(cidr_set.get('CidrBlock'))
            return cidr_list
        return ''
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with getting AWS VPC CIDR: {}'.format(str(err)))
        raise Exception


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
        traceback.print_exc(file=sys.stdout)
        print('Error with formating AWS SG rules: {}'.format(str(err)))
        raise Exception


def remove_sgroups():
    try:
        sgs = ec2_resource.security_groups.filter(
            Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': [args.service_base_name]}])
        if sgs:
            for sg in sgs:
                ec2_client.delete_security_group(GroupId=sg.id)
                print("The AWS security group {} has been deleted successfully".format(sg.id))
        else:
            print("There are no AWS security groups to delete")
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with removing AWS SG: {}'.format(str(err)))
        raise Exception


def create_instance():
    try:
        user_data = ''
        ami_id = get_ami_id('ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-amd64-server-20160907.1')
        instances = ec2_resource.create_instances(ImageId=ami_id, MinCount=1, MaxCount=1,
                                                  BlockDeviceMappings=[
                                                      {
                                                          "DeviceName": "/dev/sda1",
                                                          "Ebs":
                                                              {
                                                                  "VolumeSize": int(args.primary_disk_size)
                                                              }
                                                      },
                                                      {
                                                          "DeviceName": "/dev/sdb",
                                                          "Ebs":
                                                              {
                                                                  "VolumeSize": int(args.additional_disk_size)
                                                              }
                                                      }],
                                                  KeyName=args.key_name,
                                                  SecurityGroupIds=[args.sg_id],
                                                  InstanceType=args.instance_type,
                                                  SubnetId=args.subnet_id,
                                                  UserData=user_data)
        for instance in instances:
            print("Waiting for instance {} become running.".format(instance.id))
            instance.wait_until_running()
            tag = {'Key': 'Name', 'Value': args.service_base_name + '-repository'}
            instance_tag = {"Key": tag_name, "Value": args.service_base_name}
            create_tag(instance.id, tag)
            create_tag(instance.id, instance_tag)
            tag_intance_volume(instance.id, args.service_base_name + '-repository', instance_tag)
            return instance.id
        return ''
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with creating AWS EC2 instance: {}'.format(str(err)))
        raise Exception


def tag_intance_volume(instance_id, node_name, instance_tag):
    try:
        volume_list = get_instance_attr(instance_id, 'block_device_mappings')
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
        traceback.print_exc(file=sys.stdout)
        print('Error with tagging AWS EC2 instance volumes: {}'.format(str(err)))
        raise Exception


def get_instance_attr(instance_id, attribute_name):
    try:
        instances = ec2_resource.instances.filter(
            Filters=[{'Name': 'instance-id', 'Values': [instance_id]},
                     {'Name': 'instance-state-name', 'Values': ['running']}])
        for instance in instances:
            return getattr(instance, attribute_name)
        return ''
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with getting AWS EC2 instance attributes: {}'.format(str(err)))
        raise Exception


def get_ami_id(ami_name):
    try:
        image_id = ''
        response = ec2_client.describe_images(
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
            raise Exception("Unable to find AWS AMI id with name: " + ami_name)
        return image_id
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with getting AWS AMI ID: {}'.format(str(err)))
        raise Exception


def remove_route_tables():
    try:
        rtables = ec2_client.describe_route_tables(Filters=[{'Name': 'tag-key', 'Values': [tag_name]}]).get('RouteTables')
        for rtable in rtables:
            if rtable:
                rtable_associations = rtable.get('Associations')
                rtable = rtable.get('RouteTableId')
                for association in rtable_associations:
                    ec2_client.disassociate_route_table(AssociationId=association.get('RouteTableAssociationId'))
                    print("Association {} has been removed".format(association.get('RouteTableAssociationId')))
                ec2_client.delete_route_table(RouteTableId=rtable)
                print("AWS Route table {} has been removed".format(rtable))
            else:
                print("There are no AWS route tables to remove")
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with removing AWS Route Tables: {}'.format(str(err)))
        raise Exception


def remove_ec2():
    try:
        inst = ec2_resource.instances.filter(
            Filters=[{'Name': 'instance-state-name', 'Values': ['running', 'stopped', 'pending', 'stopping']},
                     {'Name': 'tag:{}'.format(tag_name), 'Values': ['{}'.format(args.service_base_name)]}])
        instances = list(inst)
        if instances:
            for instance in instances:
                ec2_client.terminate_instances(InstanceIds=[instance.id])
                waiter = ec2_client.get_waiter('instance_terminated')
                waiter.wait(InstanceIds=[instance.id])
                print("The instance {} has been terminated successfully".format(instance.id))
        else:
            print("There are no instances with '{}' tag to terminate".format(tag_name))
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with removing EC2 instances: {}'.format(str(err)))
        raise Exception


def remove_internet_gateways(vpc_id, tag_value):
    try:
        ig_id = ''
        response = ec2_client.describe_internet_gateways(
            Filters=[
                {'Name': 'tag-key', 'Values': [tag_name]},
                {'Name': 'tag-value', 'Values': [tag_value]}]).get('InternetGateways')
        for i in response:
            ig_id = i.get('InternetGatewayId')
        ec2_client.detach_internet_gateway(InternetGatewayId=ig_id, VpcId=vpc_id)
        print("AWS Internet gateway {0} has been detached from VPC {1}".format(ig_id, vpc_id))
        ec2_client.delete_internet_gateway(InternetGatewayId=ig_id)
        print("AWS Internet gateway {} has been deleted successfully".format(ig_id))
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with removing AWS Internet gateways: {}'.format(str(err)))
        raise Exception


def enable_auto_assign_ip(subnet_id):
    try:
        ec2_client.modify_subnet_attribute(MapPublicIpOnLaunch={'Value': True}, SubnetId=subnet_id)
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with enabling auto-assign of public IP addresses: {}'.format(str(err)))
        raise Exception


def subnet_exist(return_id=False):
    try:
        subnet_created = False
        if args.vpc_id:
            filters = [{'Name': 'tag-key', 'Values': [tag_name]},
                       {'Name': 'tag-value', 'Values': [args.service_base_name]},
                       {'Name': 'vpc-id', 'Values': [args.vpc_id]}]
        else:
            filters = [{'Name': 'tag-key', 'Values': [tag_name]},
                       {'Name': 'tag-value', 'Values': [args.service_base_name]}]
        for subnet in ec2_resource.subnets.filter(Filters=filters):
            if return_id:
                return subnet.id
            subnet_created = True
        return subnet_created
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with getting AWS Subnet: {}'.format(str(err)))
        raise Exception


def sg_exist(return_id=False):
    try:
        sg_created = False
        for security_group in ec2_resource.security_groups.filter(
                Filters=[{'Name': 'group-name', 'Values': [args.service_base_name + "-sg"]}]):
            if return_id:
                return security_group.id
            sg_created = True
        return sg_created
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with getting AWS Security group: {}'.format(str(err)))
        raise Exception


def ec2_exist(return_id=False):
    try:
        ec2_created = False
        instances = ec2_resource.instances.filter(
            Filters=[{'Name': 'tag:Name', 'Values': [args.service_base_name + '-repository']},
                     {'Name': 'instance-state-name', 'Values': ['running', 'pending', 'stopping', 'stopped']}])
        for instance in instances:
            if return_id:
                return instance.id
            ec2_created = True
        return ec2_created
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with getting AWS EC2 instance: {}'.format(str(err)))
        raise Exception


def allocate_elastic_ip():
    try:
        tag = {"Key": tag_name, "Value": "{}".format(args.service_base_name)}
        name_tag = {"Key": "Name", "Value": "{}-eip".format(args.service_base_name)}
        allocation_id = ec2_client.allocate_address(Domain='vpc').get('AllocationId')
        create_tag(allocation_id, tag)
        create_tag(allocation_id, name_tag)
        print('AWS Elastic IP address has been allocated')
        return allocation_id
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with creating AWS Elastic IP: {}'.format(str(err)))
        raise Exception


def release_elastic_ip():
    try:
        allocation_id = elastic_ip_exist(True)
        ec2_client.release_address(AllocationId=allocation_id)
        print("AWS Elastic IP address has been released.")
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with removing AWS Elastic IP: {}'.format(str(err)))
        raise Exception


def associate_elastic_ip(instance_id, allocation_id):
    try:
        ec2_client.associate_address(InstanceId=instance_id, AllocationId=allocation_id).get('AssociationId')
        print("AWS Elastic IP address has been associated.")
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with associating AWS Elastic IP: {}'.format(str(err)))
        raise Exception


def disassociate_elastic_ip(association_id):
    try:
        ec2_client.disassociate_address(AssociationId=association_id)
        print("AWS Elastic IP address has been disassociated.")
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with disassociating AWS Elastic IP: {}'.format(str(err)))
        raise Exception


def elastic_ip_exist(return_id=False, return_parameter='AllocationId'):
    try:
        elastic_ip_created = False
        elastic_ips = ec2_client.describe_addresses(
            Filters=[
                {'Name': 'tag-key', 'Values': [tag_name]},
                {'Name': 'tag-value', 'Values': [args.service_base_name]}
            ]
        ).get('Addresses')
        for elastic_ip in elastic_ips:
            if return_id:
                return elastic_ip.get(return_parameter)
            elastic_ip_created = True
        return elastic_ip_created
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with getting AWS Elastic IP: {}'.format(str(err)))
        raise Exception


def create_route_53_record(hosted_zone_id, hosted_zone_name, subdomain, ip_address):
    try:
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
        traceback.print_exc(file=sys.stdout)
        print('Error with creating AWS Route53 record: {}'.format(str(err)))
        raise Exception


def remove_route_53_record(hosted_zone_id, hosted_zone_name, subdomain):
    try:
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
        print("AWS Route53 record has been removed.")
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with removing AWS Route53 record: {}'.format(str(err)))
        raise Exception


def get_instance_ip_address_by_id(instance_id, ip_address_type):
    try:
        instances = ec2_resource.instances.filter(
            Filters=[{'Name': 'instance-id', 'Values': [instance_id]},
                     {'Name': 'instance-state-name', 'Values': ['running']}])
        for instance in instances:
            return getattr(instance, ip_address_type)
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with getting AWS EC2 instance IP address: {}'.format(str(err)))
        raise Exception


def create_efs():
    try:
        token = id_generator(10, False)
        efs = efs_client.create_file_system(
            CreationToken=token,
            PerformanceMode='generalPurpose',
            Encrypted=True
        )
        while efs_client.describe_file_systems(
                FileSystemId=efs.get('FileSystemId')).get('FileSystems')[0].get('LifeCycleState') != 'available':
            time.sleep(5)
        return efs.get('FileSystemId')
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with creating AWS EFS: {}'.format(str(err)))
        raise Exception


def create_mount_target(efs_sg_id):
    try:
        mount_target_id = efs_client.create_mount_target(
            FileSystemId=args.efs_id,
            SubnetId=args.subnet_id,
            SecurityGroups=[
                efs_sg_id
            ]
        ).get('MountTargetId')
        while efs_client.describe_mount_targets(
                MountTargetId=mount_target_id).get('MountTargets')[0].get('LifeCycleState') != 'available':
            time.sleep(10)
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with creating AWS mount target: {}'.format(str(err)))
        raise Exception


def efs_exist(return_id=False):
    try:
        efs_created = False
        for efs in efs_client.describe_file_systems().get('FileSystems'):
            if efs.get('Name') == args.service_base_name + '-efs':
                if return_id:
                    return efs.get('FileSystemId')
                efs_created = True
        return efs_created
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with getting AWS EFS: {}'.format(str(err)))
        raise Exception


def remove_efs():
    try:
        efs_id = efs_exist(True)
        mount_targets = efs_client.describe_mount_targets(FileSystemId=efs_id).get('MountTargets')
        for mount_target in mount_targets:
            efs_client.delete_mount_target(MountTargetId=mount_target.get('MountTargetId'))
        while efs_client.describe_file_systems(
                FileSystemId=efs_id).get('FileSystems')[0].get('NumberOfMountTargets') != 0:
            time.sleep(5)
        efs_client.delete_file_system(FileSystemId=efs_id)
        while efs_exist():
            time.sleep(5)
        print('AWS EFS has been deleted')
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with removing AWS EFS: {}'.format(str(err)))
        raise Exception


def ensure_ssh_user(initial_user):
    try:
        if not exists(conn,'/home/{}/.ssh_user_ensured'.format(initial_user)):
            conn.sudo('useradd -m -G sudo -s /bin/bash {0}'.format(configuration['conf_os_user']))
            conn.sudo('echo "{} ALL = NOPASSWD:ALL" >> /etc/sudoers'.format(configuration['conf_os_user']))
            conn.sudo('mkdir /home/{}/.ssh'.format(configuration['conf_os_user']))
            conn.sudo('chown -R {0}:{0} /home/{1}/.ssh/'.format(initial_user, configuration['conf_os_user']))
            conn.sudo('cat /home/{0}/.ssh/authorized_keys > /home/{1}/.ssh/authorized_keys'.format(
                initial_user, configuration['conf_os_user']))
            conn.sudo('chown -R {0}:{0} /home/{0}/.ssh/'.format(configuration['conf_os_user']))
            conn.sudo('chmod 700 /home/{0}/.ssh'.format(configuration['conf_os_user']))
            conn.sudo('chmod 600 /home/{0}/.ssh/authorized_keys'.format(configuration['conf_os_user']))
            conn.sudo('mkdir /home/{}/.ensure_dir'.format(configuration['conf_os_user']))
            conn.sudo('touch /home/{}/.ssh_user_ensured'.format(initial_user))
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with creating datalab-user: {}'.format(str(err)))
        raise Exception


def install_java():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/java_ensured'.format(configuration['conf_os_user'])):
            conn.sudo('apt-get update')
            conn.sudo('apt-get install -y default-jdk ')
            conn.sudo('touch /home/{}/.ensure_dir/java_ensured'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with installing Java: {}'.format(str(err)))
        raise Exception


def install_groovy():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/groovy_ensured'.format(configuration['conf_os_user'])):
            conn.sudo('apt-get install -y unzip')
            conn.sudo('mkdir /usr/local/groovy')
            conn.sudo('wget https://bintray.com/artifact/download/groovy/maven/apache-groovy-binary-{0}.zip -O \
                  /tmp/apache-groovy-binary-{0}.zip'.format(groovy_version))
            conn.sudo('unzip /tmp/apache-groovy-binary-{}.zip -d \
                  /usr/local/groovy'.format(groovy_version))
            conn.sudo('ln -s /usr/local/groovy/groovy-{} \
                  /usr/local/groovy/latest'.format(groovy_version))
            conn.sudo('touch /home/{}/.ensure_dir/groovy_ensured'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with installing Groovy: {}'.format(str(err)))
        raise Exception


def nexus_service_waiter():
    nexus_started = False
    checks_count = 0
    with hide('running'):
        while not nexus_started and checks_count < 200:
            print('Waiting nexus to be started...')
            time.sleep(5)
            result = conn.sudo('nmap -p 8443 localhost | grep closed > /dev/null ; echo $?').stdout
            result = result[:1]
            if result == '1':
                nexus_started = True
            else:
                checks_count += 1
    if not nexus_started and checks_count >= 200:
        print('Error: Unable to start Nexus. Aborting...')
        sys.exit(1)

    
def install_nexus():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/nexus_ensured'.format(configuration['conf_os_user'])):
            if args.efs_enabled == 'False':
                mounting_disks()
            else:
                mount_efs()
            conn.sudo('apt-get install -y maven nmap python-pip')
            conn.sudo('pip2 install -UI pip')
            conn.sudo('pip2 install -U fabric==1.14.0')
            conn.sudo('mkdir -p /opt/nexus')
            conn.sudo('wget https://sonatype-download.global.ssl.fastly.net/nexus/{0}/nexus-{1}-unix.tar.gz -O \
                  /opt/nexus-{1}-unix.tar.gz'.format(
                  nexus_version.split('.')[0], nexus_version))
            conn.sudo('tar -zhxvf /opt/nexus-{}-unix.tar.gz -C /opt/'.format(
                  nexus_version))
            conn.sudo('mv /opt/nexus-{}/* /opt/nexus/'.format(nexus_version))
            conn.sudo('mv /opt/nexus-{}/.[!.]* /opt/nexus/'.format(
                  nexus_version))
            conn.sudo('rm -rf /opt/nexus-{}'.format(nexus_version))
            conn.sudo('useradd nexus')
            conn.sudo('echo \"run_as_user="nexus"\" > /opt/nexus/bin/nexus.rc')
            create_keystore()
            conn.put('templates/jetty-https.xml', '/tmp/jetty-https.xml')
            conn.sudo('sed -i "s/KEYSTORE_PASSWORD/{}/g" /tmp/jetty-https.xml'.format(keystore_pass))
            conn.sudo('cp -f /tmp/jetty-https.xml /opt/nexus/etc/jetty/')
            conn.put('templates/nexus.service', '/tmp/nexus.service')
            if args.efs_enabled == 'False':
                conn.sudo('sed -i "s|EFS_SERVICE||g" /tmp/nexus.service')
            else:
                conn.sudo('sed -i "s|EFS_SERVICE|mount-efs-sequentially.service|g" /tmp/nexus.service')
            conn.sudo('cp /tmp/nexus.service /etc/systemd/system/')
            conn.put('files/nexus.properties', '/tmp/nexus.properties')
            conn.sudo('mkdir -p /opt/sonatype-work/nexus3/etc')
            conn.sudo('cp -f /tmp/nexus.properties /opt/sonatype-work/nexus3/etc/nexus.properties')
            conn.sudo('chown -R nexus:nexus /opt/nexus /opt/sonatype-work')
            conn.sudo('systemctl daemon-reload')
            conn.sudo('systemctl start nexus')
            nexus_service_waiter()
            conn.sudo('systemctl enable nexus')
            conn.put('templates/configureNexus.groovy', '/tmp/configureNexus.groovy')
            conn.sudo('sed -i "s/REGION/{}/g" /tmp/configureNexus.groovy'.format(args.region))
            conn.sudo('sed -i "s/ADMIN_PASSWORD/{}/g" /tmp/configureNexus.groovy'.format(args.nexus_admin_password))
            conn.sudo('sed -i "s/SERVICE_USER_NAME/{}/g" /tmp/configureNexus.groovy'.format(args.nexus_service_user_name))
            conn.sudo('sed -i "s/SERVICE_USER_PASSWORD/{}/g" /tmp/configureNexus.groovy'.format(
                args.nexus_service_user_password))
            conn.sudo('wget http://repo.{}.amazonaws.com/2017.09/main/mirror.list -O /tmp/main_mirror.list'.format(
                args.region))
            conn.sudo('wget http://repo.{}.amazonaws.com/2017.09/updates/mirror.list -O /tmp/updates_mirror.list'.format(
                args.region))
            amazon_main_repo = conn.sudo("cat /tmp/main_mirror.list  | grep {} | sed 's/$basearch//g'".format(args.region)).stdout
            amazon_updates_repo = conn.sudo("cat /tmp/updates_mirror.list  | grep {} | sed 's/$basearch//g'".format(
                args.region)).stdout
            conn.sudo('sed -i "s|AMAZON_MAIN_URL|{}|g" /tmp/configureNexus.groovy'.format(amazon_main_repo))
            conn.sudo('sed -i "s|AMAZON_UPDATES_URL|{}|g" /tmp/configureNexus.groovy'.format(amazon_updates_repo))
            conn.sudo('rm -f /tmp/main_mirror.list')
            conn.sudo('rm -f /tmp/updates_mirror.list')
            conn.put('scripts/addUpdateScript.groovy', '/tmp/addUpdateScript.groovy')
            script_executed = False
            while not script_executed:
                try:
                    conn.sudo('/usr/local/groovy/latest/bin/groovy /tmp/addUpdateScript.groovy -u "admin" -p "admin123" \
                          -n "configureNexus" -f "/tmp/configureNexus.groovy" -h "http://localhost:8081"')
                    script_executed = True
                except:
                    time.sleep(10)
                    pass
            conn.sudo('curl -u admin:admin123 -X POST --header \'Content-Type: text/plain\' \
                   http://localhost:8081/service/rest/v1/script/configureNexus/run')
            conn.sudo('systemctl stop nexus')
            conn.sudo('git clone https://github.com/sonatype-nexus-community/nexus-repository-apt')
            conn.sudo('''bash -c 'cd nexus-repository-apt && mvn' ''')
            apt_plugin_version = conn.sudo('find nexus-repository-apt/ -name "nexus-repository-apt-*.jar" '
                                      '-printf "%f\\n" | grep -v "sources"').stdout.replace('nexus-repository-apt-',
                                                                                     '').replace('.jar', '')
            compress_plugin_version = conn.sudo('find /opt/nexus/ -name "commons-compress-*.jar" '
                                           '-printf "%f\\n" ').stdout.replace('commons-compress-', '').replace('.jar', '')
            xz_plugin_version = conn.sudo('find /opt/nexus/ -name "xz-*.jar" '
                                     '-printf "%f\\n" ').stdout.replace('xz-', '').replace('.jar', '')
            conn.sudo('mkdir -p /opt/nexus/system/net/staticsnow/nexus-repository-apt/{0}/'.format(apt_plugin_version))
            apt_plugin_jar_path = conn.sudo('find nexus-repository-apt/ -name "nexus-repository-apt-{0}.jar"'.format(
                apt_plugin_version)).stdout
            conn.sudo('cp -f {0} /opt/nexus/system/net/staticsnow/nexus-repository-apt/{1}/'.format(
                apt_plugin_jar_path, apt_plugin_version
            ))
            conn.sudo('sed -i "$ d" /opt/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/{0}/'
                 'nexus-core-feature-{0}-features.xml'.format(nexus_version))
            conn.sudo('''echo '<feature name="nexus-repository-apt" description="net.staticsnow:nexus-repository-apt" '''
                 '''version="{1}">' >> /opt/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/{0}/'''
                 '''nexus-core-feature-{0}-features.xml'''.format(nexus_version, apt_plugin_version))
            conn.sudo('''echo '<details>net.staticsnow:nexus-repository-apt</details>' >> '''
                 '''/opt/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/{0}/'''
                 '''nexus-core-feature-{0}-features.xml'''.format(nexus_version))
            conn.sudo('''echo '<bundle>mvn:net.staticsnow/nexus-repository-apt/{1}</bundle>' >> '''
                 '''/opt/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/{0}/'''
                 '''nexus-core-feature-{0}-features.xml'''.format(nexus_version, apt_plugin_version))
            conn.sudo('''echo '<bundle>mvn:org.apache.commons/commons-compress/{1}</bundle>' >> '''
                 '''/opt/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/{0}/'''
                 '''nexus-core-feature-{0}-features.xml'''.format(nexus_version, compress_plugin_version))
            conn.sudo('''echo '<bundle>mvn:org.tukaani/xz/{1}</bundle>' >> '''
                 '''/opt/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/{0}/'''
                 '''nexus-core-feature-{0}-features.xml'''.format(nexus_version, xz_plugin_version))
            conn.sudo('''echo '</feature>' >> '''
                 '''/opt/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/{0}/'''
                 '''nexus-core-feature-{0}-features.xml'''.format(nexus_version))
            conn.sudo('''echo '</features>' >> '''
                 '''/opt/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/{0}/'''
                 '''nexus-core-feature-{0}-features.xml'''.format(nexus_version))
            conn.sudo('''sed -i 's|<feature prerequisite=\"true\" dependency=\"false\">wrap</feature>|'''
                 '''<feature prerequisite=\"true\" dependency=\"false\">wrap</feature>\\n'''
                 '''<feature prerequisite=\"false\" dependency=\"false\">nexus-repository-apt</feature>|g' '''
                 '''/opt/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/{0}/nexus-core-feature-'''
                 '''{0}-features.xml'''.format(nexus_version))
            conn.sudo('git clone https://github.com/sonatype-nexus-community/nexus-repository-r.git')
            conn.sudo('''bash -c 'cd nexus-repository-r && mvn clean install' ''')
            r_plugin_version = conn.sudo('find nexus-repository-r/ -name "nexus-repository-r-*.jar" '
                                    '-printf "%f\\n" | grep -v "sources"').stdout.replace('nexus-repository-r-', '').replace(
                '.jar', '')
            conn.sudo('mkdir -p /opt/nexus/system/org/sonatype/nexus/plugins/nexus-repository-r/{}/'.format(
                r_plugin_version))
            r_plugin_jar_path = conn.sudo('find nexus-repository-r/ -name "nexus-repository-r-{0}.jar"'.format(
                r_plugin_version)).stdout
            conn.sudo('cp -f {0} /opt/nexus/system/org/sonatype/nexus/plugins/nexus-repository-r/{1}/'.format(
                r_plugin_jar_path, r_plugin_version
            ))
            conn.sudo('sed -i "$ d" /opt/nexus/system/com/sonatype/nexus/assemblies/nexus-oss-feature/{0}/'
                 'nexus-oss-feature-{0}-features.xml'.format(nexus_version))
            conn.sudo('''echo '<feature name="nexus-repository-r" description="org.sonatype.nexus.plugins:'''
                 '''nexus-repository-r" version="{1}">' >> /opt/nexus/system/com/sonatype/nexus/assemblies/'''
                 '''nexus-oss-feature/{0}/nexus-oss-feature-{0}-features.xml'''.format(nexus_version, r_plugin_version))
            conn.sudo('''echo '<details>org.sonatype.nexus.plugins:nexus-repository-r</details>' >> '''
                 '''/opt/nexus/system/com/sonatype/nexus/assemblies/nexus-oss-feature/{0}/'''
                 '''nexus-oss-feature-{0}-features.xml'''.format(nexus_version))
            conn.sudo('''echo '<bundle>mvn:org.sonatype.nexus.plugins/nexus-repository-r/{1}</bundle>' >> '''
                 '''/opt/nexus/system/com/sonatype/nexus/assemblies/nexus-oss-feature/{0}/'''
                 '''nexus-oss-feature-{0}-features.xml'''.format(nexus_version, r_plugin_version))
            conn.sudo('''echo '</feature>' >> '''
                 '''/opt/nexus/system/com/sonatype/nexus/assemblies/nexus-oss-feature/{0}/'''
                 '''nexus-oss-feature-{0}-features.xml'''.format(nexus_version))
            conn.sudo('''echo '</features>' >> '''
                 '''/opt/nexus/system/com/sonatype/nexus/assemblies/nexus-oss-feature/{0}/'''
                 '''nexus-oss-feature-{0}-features.xml'''.format(nexus_version))
            conn.sudo('''sed -i 's|<feature prerequisite=\"true\" dependency=\"false\">wrap</feature>|'''
                 '''<feature prerequisite=\"true\" dependency=\"false\">wrap</feature>\\n'''
                 '''<feature version=\"{1}\" prerequisite=\"false\" dependency=\"false\">'''
                 '''nexus-repository-r</feature>|g' '''
                 '''/opt/nexus/system/com/sonatype/nexus/assemblies/nexus-oss-feature/{0}/'''
                 '''nexus-oss-feature-{0}-features.xml'''.format(nexus_version, r_plugin_version))
            conn.sudo('chown -R nexus:nexus /opt/nexus')
            conn.sudo('systemctl start nexus')
            nexus_service_waiter()
            conn.put('templates/addCustomRepository.groovy', '/tmp/addCustomRepository.groovy')
            conn.sudo('sed -i "s|REGION|{0}|g" /tmp/addCustomRepository.groovy'.format(args.region))
            script_executed = False
            while not script_executed:
                try:
                    conn.sudo('/usr/local/groovy/latest/bin/groovy /tmp/addUpdateScript.groovy -u "admin" -p "{}" '
                         '-n "addCustomRepository" -f "/tmp/addCustomRepository.groovy" -h '
                         '"http://localhost:8081"'.format(args.nexus_admin_password))
                    script_executed = True
                except:
                    time.sleep(10)
                    pass
            conn.sudo('curl -u admin:{} -X POST --header \'Content-Type: text/plain\' '
                 'http://localhost:8081/service/rest/v1/script/addCustomRepository/run'.format(
                  args.nexus_admin_password))
            conn.sudo('echo "admin:{}" > /opt/nexus/credentials'.format(args.nexus_admin_password))
            conn.sudo('echo "{0}:{1}" >> /opt/nexus/credentials'.format(args.nexus_service_user_name,
                                                                   args.nexus_service_user_password))
            conn.put('templates/updateRepositories.groovy', '/tmp/updateRepositories.groovy')
            conn.sudo('cp /tmp/updateRepositories.groovy /opt/nexus/updateRepositories.groovy')
            conn.put('scripts/update_amazon_repositories.py', '/tmp/update_amazon_repositories.py')
            conn.sudo('cp /tmp/update_amazon_repositories.py /opt/nexus/update_amazon_repositories.py')
            conn.sudo('sed -i "s|NEXUS_PASSWORD|{}|g" /opt/nexus/update_amazon_repositories.py'.format(
                 args.nexus_admin_password))
            conn.sudo('touch /var/log/amazon_repo_update.log')
            conn.sudo('echo "0 0 * * * root /usr/bin/python /opt/nexus/update_amazon_repositories.py --region {} >> '
                 '/var/log/amazon_repo_update.log" >> /etc/crontab'.format(args.region))
            conn.sudo('touch /home/{}/.ensure_dir/nexus_ensured'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with installing Nexus: {}'.format(str(err)))
        raise Exception


def install_nginx():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/nginx_ensured'.format(configuration['conf_os_user'])):
            hostname = conn.sudo('hostname').stdout
            conn.sudo('apt-get install -y nginx')
            conn.sudo('rm -f /etc/nginx/conf.d/* /etc/nginx/sites-enabled/default')
            conn.put('templates/nexus.conf', '/tmp/nexus.conf')
            if args.hosted_zone_id and args.hosted_zone_name and args.subdomain:
                conn.sudo('sed -i "s|SUBDOMAIN|{}|g" /tmp/nexus.conf'.format(args.subdomain))
                conn.sudo('sed -i "s|HOSTZONE|{}|g" /tmp/nexus.conf'.format(args.hosted_zone_name))
            else:
                conn.sudo('sed -i "s|SUBDOMAIN.HOSTZONE|{}|g" /tmp/nexus.conf'.format(hostname))
            conn.sudo('sed -i "s|REGION|{}|g" /tmp/nexus.conf'.format(args.region))
            conn.sudo('cp /tmp/nexus.conf /etc/nginx/conf.d/nexus.conf'.format(args.subdomain, args.hosted_zone_name))
            conn.sudo('systemctl restart nginx')
            conn.sudo('systemctl enable nginx')
            conn.sudo('touch /home/{}/.ensure_dir/nginx_ensured'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Error with installing Nginx: {}'.format(str(err)))
        raise Exception


def mounting_disks():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/additional_disk_mounted'.format(configuration['conf_os_user'])):
            conn.sudo('mkdir -p /opt/sonatype-work')
            disk_name = conn.sudo("lsblk | grep disk | awk '{print $1}' | sort | tail -n 1 | tr '\\n' ',' | sed 's|.$||g'").stdout
            conn.sudo('bash -c \'echo -e "o\nn\np\n1\n\n\nw" | fdisk /dev/{}\' '.format(disk_name))
            conn.sudo('sleep 10')
            partition_name = conn.sudo("lsblk -r | grep part | grep {} | awk {} | sort | tail -n 1 | "
                                  "tr '\\n' ',' | sed 's|.$||g'".format(disk_name, "'{print $1}'")).stdout
            conn.sudo('mkfs.ext4 -F -q /dev/{}'.format(partition_name))
            conn.sudo('mount /dev/{0} /opt/sonatype-work'.format(partition_name))
            conn.sudo('bash -c "echo \'/dev/{} /opt/sonatype-work ext4 errors=remount-ro 0 1\' >> /etc/fstab"'.format(
                partition_name))
            conn.sudo('touch /home/{}/.ensure_dir/additional_disk_mounted'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc(file=sys.stdout)
        print('Failed to mount additional volume: {}'.format(str(err)))
        raise Exception


def mount_efs():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/efs_mounted'.format(configuration['conf_os_user'])):
            conn.sudo('mkdir -p /opt/sonatype-work')
            conn.sudo('apt-get -y install binutils')
            conn.sudo('''bash -c 'cd /tmp/ && git clone https://github.com/aws/efs-utils' ''')
            conn.sudo('''bash -c 'cd /tmp/efs-utils && ./build-deb.sh' ''')
            conn.sudo('''bash -c 'cd /tmp/efs-utils && apt-get -y install ./build/amazon-efs-utils*deb' ''')
            conn.sudo('sed -i "s/stunnel_check_cert_hostname.*/stunnel_check_cert_hostname = false/g" '
                 '/etc/amazon/efs/efs-utils.conf')
            conn.sudo('sed -i "s/stunnel_check_cert_validity.*/stunnel_check_cert_validity = false/g" '
                 '/etc/amazon/efs/efs-utils.conf')
            conn.sudo('mount -t efs -o tls {}:/ /opt/sonatype-work'.format(
                args.efs_id))
            conn.sudo('bash -c "echo \'{}:/ /opt/sonatype-work efs tls,_netdev 0 0\' >> '
                 '/etc/fstab"'.format(args.efs_id))
            conn.put('files/mount-efs-sequentially.service', '/tmp/mount-efs-sequentially.service')
            conn.sudo('cp /tmp/mount-efs-sequentially.service /etc/systemd/system/')
            conn.sudo('systemctl daemon-reload')
            conn.sudo('systemctl enable mount-efs-sequentially.service')
            conn.sudo('touch /home/{}/.ensure_dir/efs_mounted'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc()
        print('Failed to mount additional volume: ', str(err))
        sys.exit(1)


def configure_ssl():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/ssl_ensured'.format(configuration['conf_os_user'])):
            hostname = conn.sudo('hostname').stdout
            private_ip = conn.sudo('curl http://169.254.169.254/latest/meta-data/local-ipv4').stdout
            subject_alt_name = 'subjectAltName = IP:{}'.format(private_ip)
            if args.network_type == 'public':
                public_ip = conn.sudo('curl http://169.254.169.254/latest/meta-data/public-ipv4').stdout
                subject_alt_name += ',IP:{}'.format(public_ip)
            conn.sudo('cp /etc/ssl/openssl.cnf /tmp/openssl.cnf')
            conn.sudo('echo "[ subject_alt_name ]" >> /tmp/openssl.cnf')
            conn.sudo('echo "{}" >> /tmp/openssl.cnf'.format(subject_alt_name))
            conn.sudo('openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout /etc/ssl/certs/repository.key '
                 '-out /etc/ssl/certs/repository.crt -subj "/C=US/ST=US/L=US/O=datalab/CN={}" -config '
                 '/tmp/openssl.cnf -extensions subject_alt_name'.format(hostname))
            conn.sudo('openssl dhparam -out /etc/ssl/certs/dhparam.pem 2048')
            conn.sudo('touch /home/{}/.ensure_dir/ssl_ensured'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc()
        print('Failed to mount additional volume: ', str(err))
        sys.exit(1)


def set_hostname():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/hostname_set'.format(configuration['conf_os_user'])):
            if args.hosted_zone_id and args.hosted_zone_name and args.subdomain:
                hostname = '{0}.{1}'.format(args.subdomain, args.hosted_zone_name)
            else:
                if args.network_type == 'public':
                    hostname = conn.sudo('curl http://169.254.169.254/latest/meta-data/public-hostname').stdout
                else:
                    hostname = conn.sudo('curl http://169.254.169.254/latest/meta-data/hostname').stdout
            conn.sudo('hostnamectl set-hostname {0}'.format(hostname))
            conn.sudo('touch /home/{}/.ensure_dir/hostname_set'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc()
        print('Failed to mount additional volume: ', str(err))
        sys.exit(1)


def create_keystore():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/keystore_created'.format(configuration['conf_os_user'])):
            conn.sudo('openssl pkcs12 -export -in /etc/ssl/certs/repository.crt -inkey /etc/ssl/certs/repository.key '
                 '-out wildcard.p12 -passout pass:{}'.format(keystore_pass))
            conn.sudo('keytool -importkeystore  -deststorepass {0} -destkeypass {0} -srckeystore wildcard.p12 -srcstoretype '
                 'PKCS12 -srcstorepass {0} -destkeystore /opt/nexus/etc/ssl/keystore.jks'.format(keystore_pass))
            conn.sudo('touch /home/{}/.ensure_dir/keystore_created'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc()
        print('Failed to create keystore: ', str(err))
        sys.exit(1)


def download_packages():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/packages_downloaded'.format(configuration['conf_os_user'])):
            packages_urls = [
                'https://pkg.jenkins.io/debian/jenkins-ci.org.key',
                'http://mirrors.sonic.net/apache/maven/maven-{0}/{1}/binaries/apache-maven-{1}-bin.zip'.format(
                    maven_version.split('.')[0], maven_version),
                'https://nodejs.org/dist/v8.15.0/node-v8.15.0.tar.gz',
                'https://github.com/sass/node-sass/releases/download/v4.11.0/linux-x64-57_binding.node',
                'http://nginx.org/download/nginx-{}.tar.gz'.format(configuration['reverse_proxy_nginx_version']),
                'https://www.scala-lang.org/files/archive/scala-{}.deb'.format(configuration['notebook_scala_version']),
                'https://archive.apache.org/dist/spark/spark-{0}/spark-{0}-bin-hadoop{1}.tgz'.format(
                    configuration['notebook_spark_version'], configuration['notebook_hadoop_version']),
                'https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/{0}/hadoop-aws-{0}.jar'.format('2.7.4'),
                'https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk/{0}/aws-java-sdk-{0}.jar'.format('1.7.4'),
                # 'https://maven.twttr.com/com/hadoop/gplcompression/hadoop-lzo/{0}/hadoop-lzo-{0}.jar'.format('0.4.20'),
                'https://repo1.maven.org/maven2/org/scalanlp/breeze_{0}/{1}/breeze_{0}-{1}.jar'.format('2.11', '0.12'),
                'https://repo1.maven.org/maven2/org/scalanlp/breeze-natives_{0}/{1}/breeze-natives_{0}-{1}.jar'.format(
                    '2.11', '0.12'),
                'https://repo1.maven.org/maven2/org/scalanlp/breeze-viz_{0}/{1}/breeze-viz_{0}-{1}.jar'.format(
                    '2.11', '0.12'),
                'https://repo1.maven.org/maven2/org/scalanlp/breeze-macros_{0}/{1}/breeze-macros_{0}-{1}.jar'.format(
                    '2.11', '0.12'),
                'https://repo1.maven.org/maven2/org/scalanlp/breeze-parent_{0}/{1}/breeze-parent_{0}-{1}.jar'.format(
                    '2.11', '0.12'),
                'https://repo1.maven.org/maven2/org/jfree/jfreechart/{0}/jfreechart-{0}.jar'.format('1.0.19'),
                'https://repo1.maven.org/maven2/org/jfree/jcommon/{0}/jcommon-{0}.jar'.format('1.0.24'),
                '--no-check-certificate https://brunelvis.org/jar/spark-kernel-brunel-all-{0}.jar'.format('2.3'),
                'http://archive.apache.org/dist/incubator/toree/0.3.0-incubating/toree-pip/toree-0.3.0.tar.gz',
                'https://download2.rstudio.org/server/trusty/amd64/rstudio-server-{}-amd64.deb'.format(
                    configuration['notebook_rstudio_version']),
                'http://us.download.nvidia.com/XFree86/Linux-x86_64/{0}/NVIDIA-Linux-x86_64-{0}.run'.format(
                    configuration['notebook_nvidia_version']),
                'https://developer.nvidia.com/compute/cuda/{0}/prod/local_installers/{1}'.format(
                    cuda_version_deeplearning, cuda_deeplearingn_file_name),
                'https://developer.nvidia.com/compute/cuda/{0}/prod/local_installers/{1}'.format(
                    configuration['notebook_cuda_version'], configuration['notebook_cuda_file_name']),
                'https://developer.download.nvidia.com/compute/redist/cudnn/v{0}/{1}'.format(
                    cudnn_version_deeplearning, cudnn_file_name_deeplearning),
                'https://developer.download.nvidia.com/compute/redist/cudnn/v{0}/{1}'.format(
                    configuration['notebook_cudnn_version'], configuration['notebook_cudnn_file_name']),
                'https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-{}-cp27-none-'
                'linux_x86_64.whl'.format(tensorflow_version_deeplearning),
                'https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-{}-cp35-cp35m-'
                'linux_x86_64.whl'.format(tensorflow_version_deeplearning),
                'https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-{}-cp27-none-'
                'linux_x86_64.whl'.format(configuration['notebook_tensorflow_version']),
                'https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-{}-cp35-cp35m-'
                'linux_x86_64.whl'.format(configuration['notebook_tensorflow_version']),
                'https://cmake.org/files/v{1}/cmake-{0}.tar.gz'.format(
                    configuration['notebook_cmake_version'],
                    configuration['notebook_cmake_version'].split('.')[0] +
                    "." + configuration['notebook_cmake_version'].split('.')[1]),
                'https://cntk.ai/PythonWheel/GPU/cntk-{}-cp27-cp27mu-linux_x86_64.whl'.format(
                    configuration['notebook_cntk_version']),
                'https://cntk.ai/PythonWheel/GPU/cntk-{}-cp35-cp35m-linux_x86_64.whl'.format(
                    configuration['notebook_cntk_version']),
                'https://www.python.org/ftp/python/{0}/Python-{0}.tgz'.format(python3_version),
                'https://nexus.develop.dlabanalytics.com/repository/packages-public/zeppelin-{}-prebuilt.tar.gz'.format(
                    configuration['notebook_zeppelin_version']),
                'http://archive.cloudera.com/beta/livy/livy-server-{}.zip'.format(
                    configuration['notebook_livy_version']),
                'https://repos.spark-packages.org/tapanalyticstoolkit/spark-tensorflow-connector/'
                '1.0.0-s_2.11/spark-tensorflow-connector-1.0.0-s_2.11.jar',
                'https://archive.apache.org/dist/incubator/toree/0.3.0-incubating/toree/'
                'toree-0.3.0-incubating-bin.tar.gz',
                'https://repo1.maven.org/maven2/org/apache/toree/toree-assembly/0.3.0-incubating/'
                'toree-assembly-0.3.0-incubating.jar',
                'https://cran.r-project.org/src/contrib/Archive/keras/keras_{}.tar.gz'.format(
                    configuration['notebook_keras_version'])
            ]
            packages_list = list()
            for package in packages_urls:
                package_name = package.split('/')[-1]
                packages_list.append({'url': package, 'name': package_name})
            conn.run('mkdir packages')
            for package in packages_list:
                conn.run('cd packages && wget {0}'.format(package['url']))
                conn.run('curl -v -u admin:{2} -F "raw.directory=/" -F '
                        '"raw.asset1=@/home/{0}/packages/{1}" '
                        '-F "raw.asset1.filename={1}"  '
                        '"http://localhost:8081/service/rest/v1/components?repository=packages"'.format(
                         configuration['conf_os_user'], package['name'], args.nexus_admin_password))
            conn.sudo('touch /home/{}/.ensure_dir/packages_downloaded'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc()
        print('Failed to download packages: ', str(err))
        sys.exit(1)


def install_docker():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/docker_installed'.format(configuration['conf_os_user'])):
            conn.sudo('curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -')
            conn.sudo('add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) '
                 'stable"')
            conn.sudo('apt-get update')
            conn.sudo('apt-cache policy docker-ce')
            conn.sudo('apt-get install -y docker-ce=5:{}~3-0~ubuntu-focal'.format(configuration['ssn_docker_version']))
            conn.sudo('usermod -a -G docker ' + configuration['conf_os_user'])
            conn.sudo('update-rc.d docker defaults')
            conn.sudo('update-rc.d docker enable')
            conn.sudo('touch /home/{}/.ensure_dir/docker_installed'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc()
        print('Failed to install docker: ', str(err))
        sys.exit(1)


def prepare_images():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/images_prepared'.format(configuration['conf_os_user'])):
            conn.put('files/Dockerfile', '/tmp/Dockerfile')
            conn.sudo('''bash -c 'cd /tmp/ && docker build --file Dockerfile -t pre-base .' ''')
            conn.sudo('docker login -u {0} -p {1} localhost:8083'.format(args.nexus_service_user_name,
                                                                    args.nexus_service_user_password))
            conn.sudo('docker tag pre-base localhost:8083/datalab-pre-base')
            conn.sudo('docker push localhost:8083/datalab-pre-base')
            conn.sudo('touch /home/{}/.ensure_dir/images_prepared'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc()
        print('Failed to download packages: ', str(err))
        sys.exit(1)


def install_squid():
    try:
        if not exists(conn,'/home/{}/.ensure_dir/squid_installed'.format(configuration['conf_os_user'])):
            conn.sudo('apt-get -y install squid')
            conn.put('templates/squid.conf', '/tmp/')
            conn.sudo('cp -f /tmp/squid.conf /etc/squid/')
            replace_string = ''
            for cidr in get_vpc_cidr_by_id(args.vpc_id):
                replace_string += 'acl AWS_VPC_CIDR src {}\\n'.format(cidr)
            conn.sudo('sed -i "s|VPC_CIDRS|{}|g" /etc/squid/squid.conf'.format(replace_string))
            replace_string = ''
            for cidr in args.allowed_ip_cidr.split(','):
                replace_string += 'acl AllowedCIDRS src {}\\n'.format(cidr)
            conn.sudo('sed -i "s|ALLOWED_CIDRS|{}|g" /etc/squid/squid.conf'.format(replace_string))
            conn.sudo('systemctl enable squid')
            conn.sudo('systemctl restart squid')
            conn.sudo('touch /home/{}/.ensure_dir/squid_installed'.format(configuration['conf_os_user']))
    except Exception as err:
        traceback.print_exc()
        print('Failed to download packages: ', str(err))
        sys.exit(1)


if __name__ == "__main__":
    if args.aws_access_key and args.aws_secret_access_key:
        ec2_resource = boto3.resource('ec2', region_name=args.region, aws_access_key_id=args.aws_access_key,
                                      aws_secret_access_key=args.aws_secret_access_key)
        ec2_client = boto3.client('ec2', region_name=args.region, aws_access_key_id=args.aws_access_key,
                                  aws_secret_access_key=args.aws_secret_access_key)
        efs_client = boto3.client('efs', region_name=args.region, aws_access_key_id=args.aws_access_key,
                                  aws_secret_access_key=args.aws_secret_access_key)
        route53_client = boto3.client('route53', aws_access_key_id=args.aws_access_key,
                                      aws_secret_access_key=args.aws_secret_access_key)
    else:
        ec2_resource = boto3.resource('ec2', region_name=args.region)
        ec2_client = boto3.client('ec2', region_name=args.region)
        efs_client = boto3.client('efs', region_name=args.region)
        route53_client = boto3.client('route53')
    tag_name = args.service_base_name + '-tag'
    pre_defined_vpc = True
    pre_defined_subnet = True
    pre_defined_sg = True
    pre_defined_efs = True
    if args.action != 'terminate' and args.datalab_conf_file_path == '':
        print('Please provide argument --datalab_conf_file_path ! Aborting... ')
        sys.exit(1)
    configuration = dict()
    config = ConfigParser()
    config.read(args.datalab_conf_file_path)
    for section in config.sections():
        for option in config.options(section):
            varname = "{0}_{1}".format(section, option)
            configuration[varname] = config.get(section, option)
    groovy_version = '2.5.1'
    nexus_version = '3.15.2-01'
    maven_version = '3.5.4'
    cuda_version_deeplearning = '8.0'
    cuda_deeplearingn_file_name = 'cuda_8.0.44_linux-run'
    cudnn_version_deeplearning = '6.0'
    cudnn_file_name_deeplearning = 'cudnn-8.0-linux-x64-v6.0.tgz'
    tensorflow_version_deeplearning = '1.4.0'
    python3_version = '3.4.0'
    if args.nexus_admin_password == '':
        args.nexus_admin_password = id_generator()
    if args.nexus_service_user_password == '':
        args.nexus_service_user_password = id_generator()
    keystore_pass = id_generator()
    if args.action == 'terminate':
        if args.hosted_zone_id and args.hosted_zone_name and args.subdomain:
            remove_route_53_record(args.hosted_zone_id, args.hosted_zone_name, args.subdomain)
        if elastic_ip_exist():
            try:
                association_id = elastic_ip_exist(True, 'AssociationId')
                disassociate_elastic_ip(association_id)
            except:
                print("AWS Elastic IP address isn't associated with instance or there is an error "
                      "with disassociating it")
            release_elastic_ip()
        if ec2_exist():
            remove_ec2()
        if efs_exist():
            remove_efs()
        if sg_exist():
            remove_sgroups()
        if subnet_exist():
            remove_subnet()
        if vpc_exist():
            args.vpc_id = vpc_exist(True)
            remove_internet_gateways(args.vpc_id, args.service_base_name)
            remove_route_tables()
            remove_vpc(args.vpc_id)
    elif args.action == 'create':
        if not args.vpc_id and not vpc_exist():
            try:
                print('[CREATING AWS VPC]')
                args.vpc_id = create_vpc(args.vpc_cidr)
                enable_vpc_dns(args.vpc_id)
                rt_id = create_rt(args.vpc_id)
                pre_defined_vpc = False
            except:
                remove_internet_gateways(args.vpc_id, args.service_base_name)
                remove_route_tables()
                remove_vpc(args.vpc_id)
                sys.exit(1)
        elif not args.vpc_id and vpc_exist():
            args.vpc_id = vpc_exist(True)
            pre_defined_vpc = False
        print('AWS VPC ID: {}'.format(args.vpc_id))
        if not args.subnet_id and not subnet_exist():
            try:
                print('[CREATING AWS SUBNET]')
                args.subnet_id = create_subnet(args.vpc_id, args.subnet_cidr)
                if args.network_type == 'public':
                    enable_auto_assign_ip(args.subnet_id)
                print("[ASSOCIATING ROUTE TABLE WITH THE SUBNET]")
                rt = get_route_table_by_tag(args.service_base_name)
                route_table = ec2_resource.RouteTable(rt)
                route_table.associate_with_subnet(SubnetId=args.subnet_id)
                pre_defined_subnet = False
            except:
                try:
                    remove_subnet()
                except:
                    print("AWS Subnet hasn't been created or there is an error with removing it")
                if not pre_defined_vpc:
                    remove_internet_gateways(args.vpc_id, args.service_base_name)
                    remove_route_tables()
                    remove_vpc(args.vpc_id)
                sys.exit(1)
        if not args.subnet_id and subnet_exist():
            args.subnet_id = subnet_exist(True)
            pre_defined_subnet = False
        print('AWS Subnet ID: {}'.format(args.subnet_id))
        if not args.sg_id and not sg_exist():
            try:
                print('[CREATING AWS SECURITY GROUP]')
                allowed_ip_cidr = list()
                for cidr in args.allowed_ip_cidr.split(','):
                    allowed_ip_cidr.append({"CidrIp": cidr.replace(' ', '')})
                allowed_vpc_cidr_ip_ranges = list()
                for cidr in get_vpc_cidr_by_id(args.vpc_id):
                    allowed_vpc_cidr_ip_ranges.append({"CidrIp": cidr})
                ingress = format_sg([
                    {
                        "PrefixListIds": [],
                        "FromPort": 80,
                        "IpRanges": allowed_ip_cidr,
                        "ToPort": 80, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 22,
                        "IpRanges": allowed_ip_cidr,
                        "ToPort": 22, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 443,
                        "IpRanges": allowed_ip_cidr,
                        "ToPort": 443, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 8082,
                        "IpRanges": allowed_ip_cidr,
                        "ToPort": 8082, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 8181,
                        "IpRanges": allowed_ip_cidr,
                        "ToPort": 8181, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 8083,
                        "IpRanges": allowed_ip_cidr,
                        "ToPort": 8083, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 8083,
                        "IpRanges": allowed_vpc_cidr_ip_ranges,
                        "ToPort": 8083, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 3128,
                        "IpRanges": allowed_ip_cidr,
                        "ToPort": 3128, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 3128,
                        "IpRanges": allowed_vpc_cidr_ip_ranges,
                        "ToPort": 3128, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 80,
                        "IpRanges": allowed_vpc_cidr_ip_ranges,
                        "ToPort": 80, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 443,
                        "IpRanges": allowed_vpc_cidr_ip_ranges,
                        "ToPort": 443, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 8082,
                        "IpRanges": allowed_vpc_cidr_ip_ranges,
                        "ToPort": 8082, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 8181,
                        "IpRanges": allowed_vpc_cidr_ip_ranges,
                        "ToPort": 8181, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    }
                ])
                egress = format_sg([
                    {"IpProtocol": "-1", "IpRanges": [{"CidrIp": '0.0.0.0/0'}], "UserIdGroupPairs": [],
                     "PrefixListIds": []}
                ])
                tag = {"Key": tag_name, "Value": args.service_base_name}
                name_tag = {"Key": "Name", "Value": args.service_base_name + "-sg"}
                args.sg_id = create_security_group(args.service_base_name + '-sg', args.vpc_id, ingress, egress, tag,
                                                   name_tag)
                pre_defined_sg = False
            except:
                try:
                    remove_sgroups()
                except:
                    print("AWS Security Group hasn't been created or there is an error with removing it")
                    pass
                if not pre_defined_subnet:
                    remove_subnet()
                if not pre_defined_vpc:
                    remove_internet_gateways(args.vpc_id, args.service_base_name)
                    remove_route_tables()
                    remove_vpc(args.vpc_id)
                sys.exit(1)
        if not args.sg_id and sg_exist():
            args.sg_id = sg_exist(True)
            pre_defined_sg = False
        print('AWS Security Group ID: {}'.format(args.sg_id))

        if args.efs_enabled == 'True':
            if not args.efs_id and not efs_exist():
                try:
                    print('[CREATING AWS EFS]')
                    allowed_ip_cidr = list()
                    for cidr in args.allowed_ip_cidr.split(','):
                        allowed_ip_cidr.append({"CidrIp": cidr.replace(' ', '')})
                    allowed_vpc_cidr_ip_ranges = list()
                    for cidr in get_vpc_cidr_by_id(args.vpc_id):
                        allowed_vpc_cidr_ip_ranges.append({"CidrIp": cidr})
                    ingress = format_sg([
                        {
                            "PrefixListIds": [],
                            "FromPort": 2049,
                            "IpRanges": allowed_ip_cidr,
                            "ToPort": 2049, "IpProtocol": "tcp", "UserIdGroupPairs": []
                        },
                        {
                            "PrefixListIds": [],
                            "FromPort": 2049,
                            "IpRanges": allowed_vpc_cidr_ip_ranges,
                            "ToPort": 2049, "IpProtocol": "tcp", "UserIdGroupPairs": []
                        }
                    ])
                    egress = format_sg([
                        {"IpProtocol": "-1", "IpRanges": [{"CidrIp": '0.0.0.0/0'}], "UserIdGroupPairs": [],
                         "PrefixListIds": []}
                    ])
                    tag = {"Key": tag_name, "Value": args.service_base_name}
                    name_tag = {"Key": "Name", "Value": args.service_base_name + "-efs-sg"}
                    efs_sg_id = create_security_group(args.service_base_name + '-efs-sg', args.vpc_id, ingress, egress,
                                                      tag, name_tag)
                    args.efs_id = create_efs()
                    mount_target_id = create_mount_target(efs_sg_id)
                    pre_defined_efs = False
                    create_efs_tag()
                except:
                    try:
                        remove_efs()
                    except:
                        print("AWS EFS hasn't been created or there is an error with removing it")
                    if not pre_defined_sg:
                        remove_sgroups()
                    if not pre_defined_subnet:
                        remove_subnet()
                    if not pre_defined_vpc:
                        remove_internet_gateways(args.vpc_id, args.service_base_name)
                        remove_route_tables()
                        remove_vpc(args.vpc_id)
                    sys.exit(1)
            if not args.efs_id and efs_exist():
                args.efs_id = efs_exist(True)
                pre_defined_efs = False
            print('AWS EFS ID: {}'.format(args.efs_id))

        if not ec2_exist():
            try:
                print('[CREATING AWS EC2 INSTANCE]')
                ec2_id = create_instance()
            except:
                try:
                    remove_ec2()
                except:
                    print("AWS EC2 instance hasn't been created or there is an error with removing it")
                if not pre_defined_efs:
                    remove_efs()
                if not pre_defined_sg:
                    remove_sgroups()
                if not pre_defined_subnet:
                    remove_subnet()
                if not pre_defined_vpc:
                    remove_internet_gateways(args.vpc_id, args.service_base_name)
                    remove_route_tables()
                    remove_vpc(args.vpc_id)
                sys.exit(1)
        else:
            ec2_id = ec2_exist(True)

        if args.network_type == 'public':
            if not elastic_ip_exist():
                try:
                    print('[ALLOCATING AWS ELASTIC IP ADDRESS]')
                    allocate_elastic_ip()
                except:
                    try:
                        release_elastic_ip()
                    except:
                        print("AWS Elastic IP address hasn't been created or there is an error with removing it")
                    remove_ec2()
                    if not pre_defined_efs:
                        remove_efs()
                    if not pre_defined_sg:
                        remove_sgroups()
                    if not pre_defined_subnet:
                        remove_subnet()
                    if not pre_defined_vpc:
                        remove_internet_gateways(args.vpc_id, args.service_base_name)
                        remove_route_tables()
                        remove_vpc(args.vpc_id)
                    sys.exit(1)
            try:
                print('[ASSOCIATING AWS ELASTIC IP ADDRESS TO EC2 INSTANCE]')
                allocation_id = elastic_ip_exist(True)
                associate_elastic_ip(ec2_id, allocation_id)
                time.sleep(30)
            except:
                try:
                    association_id = elastic_ip_exist(True, 'AssociationId')
                    disassociate_elastic_ip(association_id)
                except:
                    print("AWS Elastic IP address hasn't been associated or there is an error with disassociating it")
                release_elastic_ip()
                remove_ec2()
                if not pre_defined_efs:
                    remove_efs()
                if not pre_defined_sg:
                    remove_sgroups()
                if not pre_defined_subnet:
                    remove_subnet()
                if not pre_defined_vpc:
                    remove_internet_gateways(args.vpc_id, args.service_base_name)
                    remove_route_tables()
                    remove_vpc(args.vpc_id)
                sys.exit(1)

        if args.network_type == 'public':
            ec2_ip_address = get_instance_ip_address_by_id(ec2_id, 'public_ip_address')
        else:
            ec2_ip_address = get_instance_ip_address_by_id(ec2_id, 'private_ip_address')

        if args.hosted_zone_id and args.hosted_zone_name and args.subdomain:
            try:
                print('[CREATING AWS ROUTE53 RECORD]')
                create_route_53_record(args.hosted_zone_id, args.hosted_zone_name, args.subdomain, ec2_ip_address)
            except:
                try:
                    remove_route_53_record(args.hosted_zone_id, args.hosted_zone_name, args.subdomain)
                except:
                    print("AWS Route53 record hasn't been created or there is an error with removing it")
                if args.network_type == 'public':
                    association_id = elastic_ip_exist(True, 'AssociationId')
                    disassociate_elastic_ip(association_id)
                    release_elastic_ip()
                remove_ec2()
                if not pre_defined_efs:
                    remove_efs()
                if not pre_defined_sg:
                    remove_sgroups()
                if not pre_defined_subnet:
                    remove_subnet()
                if not pre_defined_vpc:
                    remove_internet_gateways(args.vpc_id, args.service_base_name)
                    remove_route_tables()
                    remove_vpc(args.vpc_id)
                sys.exit(1)

        print("CONFIGURE CONNECTIONS")
        global conn
        conn = datalab.fab.init_datalab_connection(ec2_ip_address, 'ubuntu', key_filename)
        print("CONFIGURE LOCAL REPOSITORY")
        try:
            print('CREATING DATALAB USER')
            ensure_ssh_user('ubuntu')
            env.host_string = configuration['conf_os_user'] + '@' + ec2_ip_address

            print('SETTING HOSTNAME')
            set_hostname()

            print('INSTALLING JAVA')
            install_java()

            print('INSTALLING GROOVY')
            install_groovy()

            print('CONFIGURING SSL CERTS')
            configure_ssl()

            print('INSTALLING NEXUS')
            install_nexus()

            print('INSTALLING NGINX')
            install_nginx()

            print('DOWNLOADING REQUIRED PACKAGES')
            download_packages()

            print('INSTALLING DOCKER')
            install_docker()

            print('PREPARING DATALAB DOCKER IMAGES')
            prepare_images()

            print('INSTALLING SQUID')
            install_squid()

            if args.hosted_zone_id and args.hosted_zone_name and args.subdomain:
                nexus_host = "{0}.{1}".format(args.subdomain, args.hosted_zone_name)
            else:
                nexus_host = ec2_ip_address

            print('[SUMMARY]')
            print("AWS VPC ID: {0}".format(args.vpc_id))
            print("AWS Subnet ID: {0}".format(args.subnet_id))
            print("AWS Security Group ID: {0}".format(args.sg_id))
            print("AWS EC2 ID: {0}".format(ec2_id))
            print("AWS EC2 IP address: {0}".format(ec2_ip_address))
            print("SSL certificate path: /etc/ssl/certs/repository.crt")
            print("Service user credentials: {0}/{1}".format(args.nexus_service_user_name,
                                                             args.nexus_service_user_password))
            print("PyPi repository URL: https://{0}/repository/pypi".format(nexus_host))
            print("Maven-central repository URL: https://{0}/repository/maven-central".format(nexus_host))
            print("Maven-bintray repository URL: https://{0}/repository/maven-bintray".format(nexus_host))
            print("Docker-internal repository URL: {0}:8083".format(nexus_host))
            print("Docker repository URL: https://{0}/repository/docker".format(nexus_host))
            print("Jenkins repository URL: https://{0}/repository/jenkins".format(nexus_host))
            print("Mongo repository URL: https://{0}/repository/mongo".format(nexus_host))
            print("Packages repository URL: https://{0}/repository/packages".format(nexus_host))
            print("NPM repository URL: https://{0}/repository/npm".format(nexus_host))
            print("Ubuntu repository URL: https://{0}/repository/ubuntu".format(nexus_host))
            print("Ubuntu-security repository URL: https://{0}/repository/ubuntu-security".format(nexus_host))
            print("Ubuntu-bintray repository URL: https://{0}/repository/ubuntu-bintray".format(nexus_host))
            print("Ubuntu-canonical repository URL: https://{0}/repository/ubuntu-canonical".format(nexus_host))
            print("Rrutter repository URL: https://{0}/repository/rrutter".format(nexus_host))
            print("R repository URL: https://{0}/repository/r".format(nexus_host))
            print("Amazon-main repository URL: https://{0}/repository/amazon-main".format(nexus_host))
            print("Amazon-updates repository URL: https://{0}/repository/amazon-updates".format(nexus_host))
            print("Squid proxy: {0}:3128".format(nexus_host))
            if args.efs_id:
                print('AWS EFS ID: {}'.format(args.efs_id))
            if args.hosted_zone_id and args.hosted_zone_name and args.subdomain:
                print("DNS name: {0}".format(args.subdomain + '.' + args.hosted_zone_name))
        except:
            if args.hosted_zone_id and args.hosted_zone_name and args.subdomain:
                remove_route_53_record(args.hosted_zone_id, args.hosted_zone_name, args.subdomain)
            if args.network_type == 'public':
                association_id = elastic_ip_exist(True, 'AssociationId')
                disassociate_elastic_ip(association_id)
                release_elastic_ip()
            remove_ec2()
            if not pre_defined_efs:
                remove_efs()
            if not pre_defined_sg:
                remove_sgroups()
            if not pre_defined_subnet:
                remove_subnet()
            if not pre_defined_vpc:
                remove_internet_gateways(args.vpc_id, args.service_base_name)
                remove_route_tables()
                remove_vpc(args.vpc_id)
            sys.exit(1)
        conn.close()
    else:
        print('Invalid action: {}'.format(args.action))
