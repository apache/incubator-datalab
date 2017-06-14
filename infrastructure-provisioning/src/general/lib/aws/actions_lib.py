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
import botocore
from botocore.client import Config
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


def put_to_bucket(bucket_name, local_file, destination_file):
    try:
        s3 = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=os.environ['aws_region'])
        with open(local_file, 'rb') as data:
            s3.upload_fileobj(data, bucket_name, destination_file)
        return True
    except Exception as err:
        logging.info("Unable to upload files to S3 bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to upload files to S3 bucket", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)
        return False


def create_s3_bucket(bucket_name, tag, region):
    try:
        s3 = boto3.resource('s3', config=Config(signature_version='s3v4'))
        if region == "us-east-1":
            bucket = s3.create_bucket(Bucket=bucket_name)
        else:
            bucket = s3.create_bucket(Bucket=bucket_name, CreateBucketConfiguration={'LocationConstraint': region})
        tagging = bucket.Tagging()
        tagging.put(Tagging={'TagSet': [tag, {'Key': os.environ['conf_tag_resource_id'], 'Value': os.environ['conf_service_base_name'] + ':' + bucket_name}]})
        tagging.reload()
        return bucket.name
    except Exception as err:
        logging.info("Unable to create S3 bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create S3 bucket", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_vpc(vpc_cidr, tag):
    try:
        ec2 = boto3.resource('ec2')
        vpc = ec2.create_vpc(CidrBlock=vpc_cidr)
        create_tag(vpc.id, tag)
        return vpc.id
    except Exception as err:
        logging.info("Unable to create VPC: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create VPC", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def enable_vpc_dns(vpc_id):
    try:
        client = boto3.client('ec2')
        client.modify_vpc_attribute(VpcId=vpc_id,
                                    EnableDnsHostnames={'Value': True})
    except Exception as err:
        logging.info("Unable to modify VPC attributes: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to modify VPC attributes", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_vpc(vpc_id):
    try:
        client = boto3.client('ec2')
        client.delete_vpc(VpcId=vpc_id)
        print "VPC " + vpc_id + " has been removed"
    except Exception as err:
        logging.info("Unable to remove VPC: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove VPC", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_tag(resource, tag):
    try:
        ec2 = boto3.client('ec2')
        if type(tag) == dict:
            resource_name = tag.get('Value')
            resource_tag = tag
        else:
            resource_name = json.loads(tag).get('Value')
            resource_tag = json.loads(tag)
        if type(resource) != list:
            resource = [resource]
        ec2.create_tags(
            Resources=resource,
            Tags=[
                resource_tag,
                {
                    'Key': os.environ['conf_tag_resource_id'],
                    'Value': os.environ['conf_service_base_name'] + ':' + resource_name
                }
            ]
        )
    except Exception as err:
        logging.info("Unable to create Tag: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create Tag", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_emr_tag(emr_id, tag):
    try:
        emr = boto3.client('emr')
        emr.remove_tags(ResourceId=emr_id, TagKeys=tag)
    except Exception as err:
        logging.info("Unable to remove Tag: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove Tag", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_rt(vpc_id, infra_tag_name, infra_tag_value):
    try:
        tag = {"Key": infra_tag_name, "Value": infra_tag_value}
        route_table = []
        ec2 = boto3.client('ec2')
        rt = ec2.create_route_table(VpcId=vpc_id)
        rt_id = rt.get('RouteTable').get('RouteTableId')
        route_table.append(rt_id)
        print 'Created Route-Table with ID: {}'.format(rt_id)
        create_tag(route_table, json.dumps(tag))
        ig = ec2.create_internet_gateway()
        ig_id = ig.get('InternetGateway').get('InternetGatewayId')
        route_table = []
        route_table.append(ig_id)
        create_tag(route_table, json.dumps(tag))
        ec2.attach_internet_gateway(InternetGatewayId=ig_id, VpcId=vpc_id)
        ec2.create_route(DestinationCidrBlock='0.0.0.0/0', RouteTableId=rt_id, GatewayId=ig_id)
        return rt_id
    except Exception as err:
        logging.info("Unable to create Route Table: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create Route Table", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_subnet(vpc_id, subnet, tag):
    try:
        ec2 = boto3.resource('ec2')
        subnet = ec2.create_subnet(VpcId=vpc_id, CidrBlock=subnet)
        create_tag(subnet.id, tag)
        subnet.reload()
        return subnet.id
    except Exception as err:
        logging.info("Unable to create Subnet: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create Subnet", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_security_group(security_group_name, vpc_id, security_group_rules, egress, tag):
    ec2 = boto3.resource('ec2')
    group = ec2.create_security_group(GroupName=security_group_name, Description='security_group_name', VpcId=vpc_id)
    time.sleep(10)
    create_tag(group.id, tag)
    try:
        group.revoke_egress(IpPermissions=[{"IpProtocol": "-1", "IpRanges": [{"CidrIp": "0.0.0.0/0"}], "UserIdGroupPairs": [], "PrefixListIds": []}])
    except:
        print "Mentioned rule does not exist"
    for rule in security_group_rules:
        group.authorize_ingress(IpPermissions=[rule])
    for rule in egress:
        group.authorize_egress(IpPermissions=[rule])
    return group.id


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
            print "Waiting for instance " + instance.id + " become running."
            instance.wait_until_running()
            tag = {'Key': 'Name', 'Value': definitions.node_name}
            create_tag(instance.id, tag)
            create_tag(instance.id, instance_tag)
            return instance.id
        return ''
    except Exception as err:
        logging.info("Unable to create EC2: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to create EC2", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_iam_role(role_name, role_profile, region, service='ec2'):
    conn = boto3.client('iam')
    try:
        if region == 'cn-north-1':
            conn.create_role(RoleName=role_name,
                             AssumeRolePolicyDocument='{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":["' + service + '.amazonaws.com.cn"]},"Action":["sts:AssumeRole"]}]}')
        else:
            conn.create_role(RoleName=role_name, AssumeRolePolicyDocument='{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":["' + service + '.amazonaws.com"]},"Action":["sts:AssumeRole"]}]}')
    except botocore.exceptions.ClientError as e_role:
        if e_role.response['Error']['Code'] == 'EntityAlreadyExists':
            print "IAM role already exists. Reusing..."
        else:
            logging.info("Unable to create IAM role: " + str(e_role.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create IAM role", "error_message": str(e_role.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return
    if service == 'ec2':
        try:
            conn.create_instance_profile(InstanceProfileName=role_profile)
            waiter = conn.get_waiter('instance_profile_exists')
            waiter.wait(InstanceProfileName=role_profile)
        except botocore.exceptions.ClientError as e_profile:
            if e_profile.response['Error']['Code'] == 'EntityAlreadyExists':
                print "Instance profile already exists. Reusing..."
            else:
                logging.info("Unable to create Instance Profile: " + str(e_profile.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
                append_result(str({"error": "Unable to create Instance Profile", "error_message": str(e_profile.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
                traceback.print_exc(file=sys.stdout)
                return
        try:
            conn.add_role_to_instance_profile(InstanceProfileName=role_profile, RoleName=role_name)
            time.sleep(30)
        except botocore.exceptions.ClientError as err:
            logging.info("Unable to add IAM role to instance profile: " + str(err.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to add IAM role to instance profile", "error_message": str(err.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)


def attach_policy(role_name, policy_arn):
    try:
        conn = boto3.client('iam')
        conn.attach_role_policy(PolicyArn=policy_arn, RoleName=role_name)
        time.sleep(30)
    except botocore.exceptions.ClientError as err:
        logging.info("Unable to attach Policy: " + str(err.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to attach Policy", "error_message": str(err.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_attach_policy(policy_name, role_name, file_path):
    try:
        conn = boto3.client('iam')
        with open(file_path, 'r') as myfile:
            json_file = myfile.read()
        conn.put_role_policy(RoleName=role_name, PolicyName=policy_name, PolicyDocument=json_file)
    except Exception as err:
        logging.info("Unable to attach Policy: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to attach Policy", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
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
                                    print "Releasing Elastic IP: " + elastic_ip
                            except:
                                print "There is no such Elastic IP: " + elastic_ip
                except Exception as err:
                    print err
                    print "There is no Elastic IP to disassociate from instance: " + instance.id
                client.terminate_instances(InstanceIds=[instance.id])
                waiter = client.get_waiter('instance_terminated')
                waiter.wait(InstanceIds=[instance.id])
                print "The instance " + instance.id + " has been terminated successfully"
        else:
            print "There are no instances with '" + tag_name + "' tag to terminate"
    except Exception as err:
        logging.info("Unable to remove EC2: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to EC2", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
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
            for instance in instances:
                client.stop_instances(InstanceIds=[instance.id])
                waiter = client.get_waiter('instance_stopped')
                waiter.wait(InstanceIds=[instance.id])
                print "The instance " + tag_value + " has been stopped successfully"
        else:
            print "There are no instances with " + tag_value + " name to stop"
    except Exception as err:
        logging.info("Unable to stop EC2: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to stop EC2", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
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
            for instance in instances:
                client.start_instances(InstanceIds=[instance.id])
                waiter = client.get_waiter('instance_status_ok')
                waiter.wait(InstanceIds=[instance.id])
                print "The instance " + tag_value + " has been started successfully"
        else:
            print "There are no instances with " + tag_value + " name to start"
    except Exception as err:
        logging.info("Unable to start EC2: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to start EC2", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_detach_iam_policies(role_name, action=''):
    client = boto3.client('iam')
    try:
        policy_list = client.list_attached_role_policies(RoleName=role_name).get('AttachedPolicies')
        for i in policy_list:
            policy_arn = i.get('PolicyArn')
            client.detach_role_policy(RoleName=role_name, PolicyArn=policy_arn)
            print "The IAM policy " + policy_arn + " has been detached successfully"
            if action == 'delete':
                client.delete_policy(PolicyArn=policy_arn)
                print "The IAM policy " + policy_arn + " has been deleted successfully"
    except Exception as err:
        logging.info("Unable to remove/detach IAM policy: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove/detach IAM policy",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_roles_and_profiles(role_name, role_profile_name):
    client = boto3.client('iam')
    try:
        client.remove_role_from_instance_profile(InstanceProfileName=role_profile_name, RoleName=role_name)
        client.delete_instance_profile(InstanceProfileName=role_profile_name)
        client.delete_role(RoleName=role_name)
        print "The IAM role " + role_name + " and instance profile " + role_profile_name + " have been deleted successfully"
    except Exception as err:
        logging.info("Unable to remove IAM role/profile: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove IAM role/profile",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_all_iam_resources(instance_type, scientist=''):
    try:
        client = boto3.client('iam')
        service_base_name = os.environ['conf_service_base_name'].lower().replace('-', '_')
        roles_list = []
        for item in client.list_roles(MaxItems=250).get("Roles"):
            if item.get("RoleName").startswith(service_base_name + '-'):
                roles_list.append(item.get('RoleName'))
        if roles_list:
            roles_list.sort(reverse=True)
            for iam_role in roles_list:
                if '-ssn-Role' in iam_role:
                    if instance_type == 'ssn' or instance_type == 'all':
                        try:
                            client.delete_role_policy(RoleName=iam_role, PolicyName=service_base_name + '-ssn-Policy')
                        except:
                            print 'There is no policy ' + service_base_name + '-ssn-Policy to delete'
                        role_profiles = client.list_instance_profiles_for_role(RoleName=iam_role).get('InstanceProfiles')
                        if role_profiles:
                            for i in role_profiles:
                                role_profile_name = i.get('InstanceProfileName')
                                if role_profile_name == service_base_name + '-ssn-Profile':
                                    remove_roles_and_profiles(iam_role, role_profile_name)
                        else:
                            print "There is no instance profile for " + iam_role
                            client.delete_role(RoleName=iam_role)
                            print "The IAM role " + iam_role + " has been deleted successfully"
                if '-edge-Role' in iam_role:
                    if instance_type == 'edge' and scientist in iam_role:
                        remove_detach_iam_policies(iam_role, 'delete')
                        role_profile_name = os.environ['conf_service_base_name'].lower().replace('-', '_') + '-' + '{}'.format(scientist) + '-edge-Profile'
                        try:
                            client.get_instance_profile(InstanceProfileName=role_profile_name)
                            remove_roles_and_profiles(iam_role, role_profile_name)
                        except:
                            print "There is no instance profile for " + iam_role
                            client.delete_role(RoleName=iam_role)
                            print "The IAM role " + iam_role + " has been deleted successfully"
                    if instance_type == 'all':
                        remove_detach_iam_policies(iam_role, 'delete')
                        role_profile_name = client.list_instance_profiles_for_role(RoleName=iam_role).get('InstanceProfiles')
                        if role_profile_name:
                            for i in role_profile_name:
                                role_profile_name = i.get('InstanceProfileName')
                                remove_roles_and_profiles(iam_role, role_profile_name)
                        else:
                            print "There is no instance profile for " + iam_role
                            client.delete_role(RoleName=iam_role)
                            print "The IAM role " + iam_role + " has been deleted successfully"
                if '-nb-Role' in iam_role:
                    if instance_type == 'notebook' and scientist in iam_role:
                        remove_detach_iam_policies(iam_role)
                        role_profile_name = os.environ['conf_service_base_name'].lower().replace('-', '_') + '-' + "{}".format(scientist) + '-nb-Profile'
                        try:
                            client.get_instance_profile(InstanceProfileName=role_profile_name)
                            remove_roles_and_profiles(iam_role, role_profile_name)
                        except:
                            print "There is no instance profile for " + iam_role
                            client.delete_role(RoleName=iam_role)
                            print "The IAM role " + iam_role + " has been deleted successfully"
                    if instance_type == 'all':
                        remove_detach_iam_policies(iam_role)
                        role_profile_name = client.list_instance_profiles_for_role(RoleName=iam_role).get('InstanceProfiles')
                        if role_profile_name:
                            for i in role_profile_name:
                                role_profile_name = i.get('InstanceProfileName')
                                remove_roles_and_profiles(iam_role, role_profile_name)
                        else:
                            print "There is no instance profile for " + iam_role
                            client.delete_role(RoleName=iam_role)
                            print "The IAM role " + iam_role + " has been deleted successfully"
        else:
            print "There are no IAM roles to delete. Checking instance profiles..."
        profile_list = []
        for item in client.list_instance_profiles(MaxItems=250).get("InstanceProfiles"):
            if item.get("InstanceProfileName").startswith(service_base_name + '-'):
                profile_list.append(item.get('InstanceProfileName'))
        if profile_list:
            for instance_profile in profile_list:
                if '-ssn-Profile' in instance_profile:
                    if instance_type == 'ssn' or instance_type == 'all':
                        client.delete_instance_profile(InstanceProfileName=instance_profile)
                        print "The instance profile " + instance_profile + " has been deleted successfully"
                if '-edge-Profile' in instance_profile:
                    if instance_type == 'edge' and scientist in instance_profile:
                        client.delete_instance_profile(InstanceProfileName=instance_profile)
                        print "The instance profile " + instance_profile + " has been deleted successfully"
                    if instance_type == 'all':
                        client.delete_instance_profile(InstanceProfileName=instance_profile)
                        print "The instance profile " + instance_profile + " has been deleted successfully"
                if '-nb-Profile' in instance_profile:
                    if instance_type == 'notebook' and scientist in instance_profile:
                        client.delete_instance_profile(InstanceProfileName=instance_profile)
                        print "The instance profile " + instance_profile + " has been deleted successfully"
                    if instance_type == 'all':
                        client.delete_instance_profile(InstanceProfileName=instance_profile)
                        print "The instance profile " + instance_profile + " has been deleted successfully"
        else:
            print "There are no instance profiles to delete"
    except Exception as err:
        logging.info("Unable to remove some of the IAM resources: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove some of the IAM resources", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def s3_cleanup(bucket, cluster_name, user_name):
    s3_res = boto3.resource('s3', config=Config(signature_version='s3v4'))
    client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=os.environ['aws_region'])
    try:
        client.head_bucket(Bucket=bucket)
    except:
        print "There is no bucket " + bucket + " or you do not permission to access it"
        sys.exit(0)
    try:
        resource = s3_res.Bucket(bucket)
        prefix = user_name + '/' + cluster_name + "/"
        for i in resource.objects.filter(Prefix=prefix):
            s3_res.Object(resource.name, i.key).delete()
    except Exception as err:
        logging.info("Unable to clean S3 bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to clean S3 bucket", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_s3(bucket_type='all', scientist=''):
    try:
        client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=os.environ['aws_region'])
        bucket_list = []
        if bucket_type == 'ssn':
            bucket_name = (os.environ['conf_service_base_name'] + '-ssn-bucket').lower().replace('_', '-')
        elif bucket_type == 'edge':
            bucket_name = (os.environ['conf_service_base_name'] + '-' + "{}".format(scientist) + '-bucket').lower().replace('_', '-')
        else:
            bucket_name = (os.environ['conf_service_base_name']).lower().replace('_', '-')
        for item in client.list_buckets().get('Buckets'):
            if bucket_name in item.get('Name'):
                for i in client.get_bucket_tagging(Bucket=item.get('Name')).get('TagSet'):
                    i.get('Key')
                    if i.get('Key') == os.environ['conf_service_base_name'] + '-Tag':
                        bucket_list.append(item.get('Name'))
        for s3bucket in bucket_list:
            if s3bucket:
                list_obj = client.list_objects(Bucket=s3bucket)
                list_obj = list_obj.get('Contents')
                if list_obj is not None:
                    for o in list_obj:
                        list_obj = o.get('Key')
                        client.delete_objects(
                            Bucket=s3bucket,
                            Delete={'Objects': [{'Key': list_obj}]}
                        )
                    print "The S3 bucket " + s3bucket + " has been cleaned"
                client.delete_bucket(Bucket=s3bucket)
                print "The S3 bucket " + s3bucket + " has been deleted successfully"
            else:
                print "There are no buckets to delete"
    except Exception as err:
        logging.info("Unable to remove S3 bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove S3 bucket", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_subnets(tag_value):
    try:
        ec2 = boto3.resource('ec2')
        client = boto3.client('ec2')
        tag_name = os.environ['conf_service_base_name'] + '-Tag'
        subnets = ec2.subnets.filter(
            Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': [tag_value]}])
        if subnets:
            for subnet in subnets:
                client.delete_subnet(SubnetId=subnet.id)
                print "The subnet " + subnet.id + " has been deleted successfully"
        else:
            print "There are no private subnets to delete"
    except Exception as err:
        logging.info("Unable to remove subnet: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove subnet", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
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
                print "The security group " + sg.id + " has been deleted successfully"
        else:
            print "There are no security groups to delete"
    except Exception as err:
        logging.info("Unable to remove SG: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove SG", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
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
            print "The following inbound rule is already exist:"
            print str(rule)
        else:
            logging.info("Unable to add inbound rule to SG: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to add inbound rule to SG", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
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
            print "The following outbound rule is already exist:"
            print str(rule)
        else:
            logging.info("Unable to add outbound rule to SG: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to add outbound rule to SG", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)


def deregister_image(scientist):
    try:
        client = boto3.client('ec2')
        response = client.describe_images(
            Filters=[{'Name': 'name', 'Values': ['{}-{}-*'.format(os.environ['conf_service_base_name'], scientist)]},
                     {'Name': 'tag-value', 'Values': [os.environ['conf_service_base_name']]}])
        images_list = response.get('Images')
        if images_list:
            for i in images_list:
                client.deregister_image(ImageId=i.get('ImageId'))
                print "Notebook AMI " + i.get('ImageId') + " has been deregistered successfully"
        else:
            print "There is no notebook ami to deregister"
    except Exception as err:
        logging.info("Unable to de-register image: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to de-register image", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
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
        append_result(str({"error": "Unable to remove EMR", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def remove_kernels(emr_name, tag_name, nb_tag_value, ssh_user, key_path, emr_version):
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
                if exists('/home/{}/.ensure_dir/emr_{}_interpreter_ensured'.format(ssh_user, emr_name)):
                    if os.environ['notebook_multiple_emrs'] == 'true':
                        try:
                            livy_port = sudo("cat /opt/" + emr_version + "/" + emr_name
                                             + "/livy/conf/livy.conf | grep livy.server.port | tail -n 1 | awk '{printf $3}'")
                            process_number = sudo("netstat -natp 2>/dev/null | grep ':" + livy_port +
                                                  "' | awk '{print $7}' | sed 's|/.*||g'")
                            sudo('kill -9 ' + process_number)
                            sudo('systemctl disable livy-server-' + livy_port)
                        except:
                            print "Wasn't able to find Livy server for this EMR!"
                    sudo('sed -i \"s/^export SPARK_HOME.*/export SPARK_HOME=\/opt\/spark/\" /opt/zeppelin/conf/zeppelin-env.sh')
                    sudo("rm -rf /home/{}/.ensure_dir/emr_interpreter_ensure".format(ssh_user))
                    zeppelin_url = 'http://' + private + ':8080/api/interpreter/setting/'
                    opener = urllib2.build_opener(urllib2.ProxyHandler({}))
                    req = opener.open(urllib2.Request(zeppelin_url))
                    r_text = req.read()
                    interpreter_json = json.loads(r_text)
                    interpreter_prefix = emr_name
                    for interpreter in interpreter_json['body']:
                        if interpreter_prefix in interpreter['name']:
                            print "Interpreter with ID:", interpreter['id'], "and name:", interpreter['name'], \
                                "will be removed from zeppelin!"
                            request = urllib2.Request(zeppelin_url + interpreter['id'], data='')
                            request.get_method = lambda: 'DELETE'
                            url = opener.open(request)
                            print url.read()
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
                    sudo('rm -rf /home/{}/.ensure_dir/emr_{}_interpreter_ensured'.format(ssh_user, emr_name))
                if exists('/home/{}/.ensure_dir/rstudio_emr_ensured'.format(ssh_user)):
                    sudo("sed -i '/" + emr_name + "/d' /home/{}/.Renviron".format(ssh_user))
                    if not sudo("sed -n '/^SPARK_HOME/p' /home/{}/.Renviron".format(ssh_user)):
                        sudo("sed -i '1!G;h;$!d;' /home/{0}/.Renviron; sed -i '1,3s/#//;1!G;h;$!d' /home/{0}/.Renviron".format(ssh_user))
                    sudo("sed -i 's|/opt/" + emr_version + '/' + emr_name + "/spark//R/lib:||g' /home/{}/.bashrc".format(ssh_user))
                sudo('rm -rf  /opt/' + emr_version + '/' + emr_name + '/')
                print "Notebook's " + env.hosts + " kernels were removed"
        else:
            print "There are no notebooks to clean kernels."
    except Exception as err:
        logging.info("Unable to remove kernels on Notebook: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to remove kernels on Notebook", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
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
                        print "Association " + association.get('RouteTableAssociationId') + " has been removed"
                client.delete_route_table(RouteTableId=rtable)
                print "Route table " + rtable + " has been removed"
            else:
                print "There are no route tables to remove"
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
        client.detach_internet_gateway(InternetGatewayId=ig_id,VpcId=vpc_id)
        print "Internet gateway " + ig_id + " has been detached from VPC " + vpc_id
        client.delete_internet_gateway(InternetGatewayId=ig_id)
        print "Internet gateway " + ig_id + " has been deleted successfully"
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
            print "VPC Endpoint " + i.get('VpcEndpointId') + " has been removed successfully"
    except Exception as err:
        logging.info("Unable to remove VPC Endpoint: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Unable to remove VPC Endpoint",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def create_image_from_instance(tag_name='', instance_name='', image_name=''):
    ec2 = boto3.resource('ec2')
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
        #image.create_tags(Tags=[{'Key': 'Name', 'Value': os.environ['conf_service_base_name']}])
        tag = {'Key': 'Name', 'Value': os.environ['conf_service_base_name']}
        create_tag(image.id, tag)
        return image.id
    return ''


def install_emr_spark(args):
    s3_client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=args.region)
    s3_client.download_file(args.bucket, args.user_name + '/' + args.cluster_name + '/spark.tar.gz', '/tmp/spark.tar.gz')
    s3_client.download_file(args.bucket, args.user_name + '/' + args.cluster_name + '/spark-checksum.chk', '/tmp/spark-checksum.chk')
    if 'WARNING' in local('md5sum -c /tmp/spark-checksum.chk', capture=True):
        local('rm -f /tmp/spark.tar.gz')
        s3_client.download_file(args.bucket, args.user_name + '/' + args.cluster_name + '/spark.tar.gz', '/tmp/spark.tar.gz')
        if 'WARNING' in local('md5sum -c /tmp/spark-checksum.chk', capture=True):
            print "The checksum of spark.tar.gz is mismatched. It could be caused by aws network issue."
            sys.exit(1)
    local('sudo tar -zhxvf /tmp/spark.tar.gz -C /opt/' + args.emr_version + '/' + args.cluster_name + '/')


def jars(args, emr_dir):
    print "Downloading jars..."
    s3_client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=args.region)
    s3_client.download_file(args.bucket, 'jars/' + args.emr_version + '/jars.tar.gz', '/tmp/jars.tar.gz')
    s3_client.download_file(args.bucket, 'jars/' + args.emr_version + '/jars-checksum.chk', '/tmp/jars-checksum.chk')
    if 'WARNING' in local('md5sum -c /tmp/jars-checksum.chk', capture=True):
        local('rm -f /tmp/jars.tar.gz')
        s3_client.download_file(args.bucket, 'jars/' + args.emr_version + '/jars.tar.gz', '/tmp/jars.tar.gz')
        if 'WARNING' in local('md5sum -c /tmp/jars-checksum.chk', capture=True):
            print "The checksum of jars.tar.gz is mismatched. It could be caused by aws network issue."
            sys.exit(1)
    local('tar -zhxvf /tmp/jars.tar.gz -C ' + emr_dir)


def yarn(args, yarn_dir):
    print "Downloading yarn configuration..."
    if args.region == 'cn-north-1':
        s3client = boto3.client('s3', config=Config(signature_version='s3v4'),
                                endpoint_url='https://s3.cn-north-1.amazonaws.com.cn', region_name=args.region)
        s3resource = boto3.resource('s3', config=Config(signature_version='s3v4'),
                                    endpoint_url='https://s3.cn-north-1.amazonaws.com.cn', region_name=args.region)
    else:
        s3client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=args.region)
        s3resource = boto3.resource('s3', config=Config(signature_version='s3v4'))
    get_files(s3client, s3resource, args.user_name + '/' + args.cluster_name + '/config/', args.bucket, yarn_dir)
    local('sudo mv ' + yarn_dir + args.user_name + '/' + args.cluster_name + '/config/* ' + yarn_dir)
    local('sudo rm -rf ' + yarn_dir + args.user_name + '/')


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


