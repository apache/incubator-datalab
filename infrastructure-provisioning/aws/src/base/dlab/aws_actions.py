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
import time
import sys
import os
import json
from fabric.api import *
from fabric.contrib.files import exists
import logging
from dlab.aws_meta import *
import traceback


def put_to_bucket(bucket_name, local_file, destination_file):
    try:
        s3 = boto3.client('s3')
        with open(local_file, 'rb') as data:
            s3.upload_fileobj(data, bucket_name, destination_file)
        return True
    except Exception as err:
        logging.info("Unable to upload files to S3 bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to upload files to S3 bucket", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)
        return False


def create_s3_bucket(bucket_name, tag, region):
    try:
        s3 = boto3.resource('s3')
        bucket = s3.create_bucket(Bucket=bucket_name,
                                  CreateBucketConfiguration={'LocationConstraint': region})
        tagging = bucket.Tagging()
        tagging.put(Tagging={'TagSet': [tag]})
        tagging.reload()
        return bucket.name
    except Exception as err:
        logging.info("Unable to create S3 bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to create S3 bucket", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def create_vpc(vpc_cidr, tag):
    try:
        ec2 = boto3.resource('ec2')
        vpc = ec2.create_vpc(CidrBlock=vpc_cidr)
        vpc.create_tags(Tags=[tag])
        return vpc.id
    except Exception as err:
        logging.info("Unable to create VPC: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to create VPC", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def create_tag(resource, tag):
    try:
        ec2 = boto3.client('ec2')
        ec2.create_tags(
            Resources = resource,
            Tags = [
                json.loads(tag)
            ]
        )
    except Exception as err:
        logging.info("Unable to create Tag: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to create Tag", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def create_subnet(vpc_id, subnet, tag):
    try:
        ec2 = boto3.resource('ec2')
        subnet = ec2.create_subnet(VpcId=vpc_id, CidrBlock=subnet)
        subnet.create_tags(Tags=[tag])
        subnet.reload()
        return subnet.id
    except Exception as err:
        logging.info("Unable to create Subnet: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to create Subnet", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def create_instance(definitions, instance_tag):
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
            instance.create_tags(Tags=[{'Key': 'Name', 'Value': definitions.node_name}, instance_tag])
            return instance.id
        return ''
    except Exception as err:
        logging.info("Unable to create EC2: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to create EC2", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def create_iam_role(role_name, role_profile):
    conn = boto3.client('iam')
    try:
        conn.create_role(RoleName=role_name, AssumeRolePolicyDocument='{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":["ec2.amazonaws.com"]},"Action":["sts:AssumeRole"]}]}')
        conn.create_instance_profile(InstanceProfileName=role_profile)
        waiter = conn.get_waiter('instance_profile_exists')
        waiter.wait(InstanceProfileName=role_profile)
    except botocore.exceptions.ClientError as e_role:
        if e_role.response['Error']['Code'] == 'EntityAlreadyExists':
            print "Instance profile already exists. Reusing..."
        else:
            logging.info("Unable to create Instance Profile: " + str(e_role.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            with open("/root/result.json", 'w') as result:
                res = {"error": "Unable to create Instance Profile", "error_message": str(e_role.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
                print json.dumps(res)
                result.write(json.dumps(res))
            traceback.print_exc(file=sys.stdout)
            return
    try:
        conn.add_role_to_instance_profile(InstanceProfileName=role_profile, RoleName=role_name)
        time.sleep(30)
    except botocore.exceptions.ClientError as err:
        logging.info("Unable to create IAM role: " + str(err.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to create IAM role", "error_message": str(err.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def attach_policy(policy_arn, role_name):
    try:
        conn = boto3.client('iam')
        conn.attach_role_policy(PolicyArn=policy_arn, RoleName=role_name)
        time.sleep(30)
    except botocore.exceptions.ClientError as err:
        logging.info("Unable to attach Policy: " + str(err.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to attach Policy", "error_message": str(err.response['Error']['Message']) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def create_attach_policy(policy_name, role_name, file_path):
    try:
        conn = boto3.client('iam')
        with open(file_path, 'r') as myfile:
            json_file = myfile.read()
        conn.put_role_policy(RoleName=role_name, PolicyName=policy_name, PolicyDocument=json_file)
    except Exception as err:
        logging.info("Unable to attach Policy: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to attach Policy", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def remove_ec2(tag_name, tag_value):
    try:
        ec2 = boto3.resource('ec2')
        client = boto3.client('ec2')
        inst = ec2.instances.filter(
            Filters=[{'Name': 'instance-state-name', 'Values': ['running', 'stopped', 'pending', 'stopping']},
                     {'Name': 'tag:{}'.format(tag_name), 'Values': ['{}'.format(tag_value)]}])
        instances = list(inst)
        if instances:
            for instance in instances:
                client.terminate_instances(InstanceIds=[instance.id])
                waiter = client.get_waiter('instance_terminated')
                waiter.wait(InstanceIds=[instance.id])
                print "The instance " + instance.id + " has been terminated successfully"
        else:
            print "There are no instances with '" + tag_name + "' tag to terminate"
    except Exception as err:
        logging.info("Unable to remove EC2: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to EC2", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to stop EC2", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to start EC2", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to remove/detach IAM policy",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to remove IAM role/profile",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def remove_all_iam_resources(instance_type, scientist=''):
    try:
        client = boto3.client('iam')
        roles_list = []
        for item in client.list_roles(MaxItems=250).get("Roles"):
            if os.environ['conf_service_base_name'] in item.get("RoleName"):
                roles_list.append(item.get('RoleName'))
        if roles_list:
            roles_list.sort(reverse=True)
            for iam_role in roles_list:
                if '-ssn-Role' in iam_role:
                    if instance_type == 'ssn' or instance_type == 'all':
                        try:
                            client.delete_role_policy(RoleName=iam_role, PolicyName=os.environ['conf_service_base_name'] + '-ssn-Policy')
                        except:
                            print 'There is no policy ' + os.environ['conf_service_base_name'] + '-ssn-Policy to delete'
                        role_profile_name = os.environ['conf_service_base_name'] + '-ssn-Profile'
                        try:
                            client.get_instance_profile(InstanceProfileName=role_profile_name)
                            remove_roles_and_profiles(iam_role, role_profile_name)
                        except:
                            print "There is no instance profile for " + iam_role
                            client.delete_role(RoleName=iam_role)
                            print "The IAM role " + iam_role + " has been deleted successfully"
                if '-edge-Role' in iam_role:
                    if instance_type == 'edge' and scientist in iam_role:
                        remove_detach_iam_policies(iam_role, 'delete')
                        role_profile_name = os.environ['conf_service_base_name'] + '-' + '{}'.format(scientist) + '-edge-Profile'
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
                        role_profile_name = os.environ['conf_service_base_name'] + '-' + "{}".format(scientist) + '-nb-Profile'
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
            print "There are no IAM roles instance profiles and policies to delete"
    except Exception as err:
        logging.info("Unable to remove some of the IAM resources: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to remove some of the IAM resources", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def s3_cleanup(bucket, cluster_name, user_name):
    s3_res = boto3.resource('s3')
    client = boto3.client('s3')
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to clean S3 bucket", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def remove_s3(bucket_type='all', scientist=''):
    try:
        client = boto3.client('s3')
        bucket_list = []
        if bucket_type == 'ssn':
            bucket_name = (os.environ['conf_service_base_name'] + '-ssn-bucket').lower().replace('_', '-')
        elif bucket_type == 'edge':
            bucket_name = (os.environ['conf_service_base_name'] + '-' + "{}".format(scientist) + '-bucket').lower().replace('_', '-')
        else:
            bucket_name = (os.environ['conf_service_base_name']).lower().replace('_', '-')
        for item in client.list_buckets().get('Buckets'):
            if bucket_name in item.get('Name'):
                bucket_list.append(item.get('Name'))
        for s3bucket in bucket_list:
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
        print "There are no more buckets to delete"
    except Exception as err:
        logging.info("Unable to remove S3 bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to remove S3 bucket", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to remove subnet", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to remove SG", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to de-register image", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to remove EMR", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
                sudo('rm -rf  /opt/' + emr_version + '/' + emr_name + '/')
                sudo('rm -rf /home/{}/.local/share/jupyter/kernels/*_{}'.format(ssh_user, emr_name))
                if exists('/home/ubuntu/.ensure_dir/rstudio_emr_ensured'):
                    sudo("sed -i '/" + emr_name + "/d' /home/ubuntu/.Renviron")
                    sudo("sed -i 's|/opt/" + emr_version + '/' + emr_name + "/spark//R/lib:||g' /home/ubuntu/.bashrc")
                print "Notebook's " + env.hosts + " kernels were removed"
        else:
            print "There are no notebooks to clean kernels."
    except Exception as err:
        logging.info("Unable to remove kernels on Notebook: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to remove kernels on Notebook", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def remove_route_tables(tag_name):
    try:
        client = boto3.client('ec2')
        rtables = client.describe_route_tables(Filters=[{'Name': 'tag-key', 'Values': [tag_name]}]).get('RouteTables')
        for rtable in rtables:
            if rtable:
                rtable = rtable.get('RouteTableId')
                client.delete_route_table(RouteTableId=rtable)
                print "Route table " + rtable + " has been removed"
            else:
                print "There are no route tables to remove"
    except Exception as err:
        logging.info("Unable to remove route table: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to remove route table",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def remove_apt_lock():
    try:
        sudo('rm -f /var/lib/apt/lists/lock')
        sudo('rm -f /var/cache/apt/archives/lock')
        sudo('rm -f /var/lib/dpkg/lock')
    except:
        sys.exit(1)
