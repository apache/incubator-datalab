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

import boto3
import boto
import botocore
import time
import sys
import os
import json
from fabric.api import *


def put_to_bucket(bucket_name, local_file, destination_file):
    try:
        s3 = boto3.client('s3')
        with open(local_file, 'rb') as data:
            s3.upload_fileobj(data, bucket_name, destination_file)
        return True
    except:
        return False


def create_s3_bucket(bucket_name, tag, region):
    s3 = boto3.resource('s3')
    bucket = s3.create_bucket(Bucket=bucket_name,
                              CreateBucketConfiguration={'LocationConstraint': region})
    tagging = bucket.Tagging()
    tagging.put(Tagging={'TagSet': [tag]})
    tagging.reload()
    return bucket.name


def create_vpc(vpc_cidr, tag):
    ec2 = boto3.resource('ec2')
    vpc = ec2.create_vpc(CidrBlock=vpc_cidr)
    vpc.create_tags(Tags=[tag])
    return vpc.id


def create_tag(resource, tag):
    ec2 = boto3.client('ec2')
    try:
        ec2.create_tags(
            Resources = resource,
            Tags = [
                json.loads(tag)
            ]
        )
    except botocore.exceptions.ClientError as err:
        print err.response['Error']['Message']


def create_subnet(vpc_id, subnet, tag):
    ec2 = boto3.resource('ec2')
    subnet = ec2.create_subnet(VpcId=vpc_id, CidrBlock=subnet)
    subnet.create_tags(Tags=[tag])
    subnet.reload()
    return subnet.id


def create_instance(definitions, instance_tag):
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


def create_iam_role(role_name, role_profile):
    conn = boto.connect_iam()
    conn.create_role(role_name)
    conn.create_instance_profile(role_profile)
    conn.add_role_to_instance_profile(role_profile, role_name)
    time.sleep(10)


def attach_policy(policy_arn, role_name):
    conn = boto.connect_iam()
    conn.attach_role_policy(policy_arn, role_name)


def create_attach_policy(policy_name, role_name, file_path):
    conn = boto.connect_iam()
    with open(file_path, 'r') as myfile:
        json = myfile.read()
    conn.put_role_policy(role_name, policy_name, json)


def remove_ec2(tag_name, tag_value):
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
            print "The instance " + tag_value + " has been terminated successfully"
    else:
        print "There are no instances with " + tag_value + " name to terminate"


def stop_ec2(tag_name, tag_value):
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


def start_ec2(tag_name, tag_value):
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


def remove_role(instance_type, scientist=''):
    print "[Removing roles and instance profiles]"
    client = boto3.client('iam')
    if instance_type == "ssn":
        role_name = os.environ['conf_service_base_name'] + '-ssn-Role'
        role_profile_name = os.environ['conf_service_base_name'] + '-ssn-Profile'
    if instance_type == "edge":
        role_name = os.environ['conf_service_base_name'] + '-' + '{}'.format(scientist) + '-edge-Role'
        role_profile_name = os.environ['conf_service_base_name'] + '-' + '{}'.format(scientist) + '-edge-Profile'
    elif instance_type == "notebook":
        role_name = os.environ['conf_service_base_name'] + '-' + "{}".format(scientist) + '-nb-Role'
        role_profile_name = os.environ['conf_service_base_name'] + '-' + "{}".format(scientist) + '-nb-Profile'
    try:
        role = client.get_role(RoleName="{}".format(role_name)).get("Role").get("RoleName")
    except:
        print "Wasn't able to get role!"
        sys.exit(1)
    policy_list = client.list_attached_role_policies(RoleName=role).get('AttachedPolicies')
    for i in policy_list:
        policy_arn = i.get('PolicyArn')
        client.detach_role_policy(RoleName=role, PolicyArn=policy_arn)
    try:
        profile = client.get_instance_profile(InstanceProfileName="{}".format(role_profile_name)).get(
            "InstanceProfile").get("InstanceProfileName")
    except:
        print "Wasn't able to get instance profile!"
        sys.exit(1)
    try:
        client.remove_role_from_instance_profile(InstanceProfileName=profile, RoleName=role)
    except:
        print "\nWasn't able to remove role from instance profile!"
        sys.exit(1)
    try:
        client.delete_instance_profile(InstanceProfileName=profile)
    except:
        print "\nWasn't able to remove instance profile!"
        sys.exit(1)
    try:
        client.delete_role(RoleName=role)
    except:
        print "\nWasn't able to remove role!"
        sys.exit(1)
    print "The IAM role " + role + " has been deleted successfully"


def s3_cleanup(bucket, cluster_name):
    s3_res = boto3.resource('s3')
    try:
        resource = s3_res.Bucket(bucket)
        prefix = "config/" + cluster_name + "/"
        for i in resource.objects.filter(Prefix=prefix):
            s3_res.Object(resource.name, i.key).delete()
    except botocore.exceptions.ClientError as err:
        print err.response['Error']['Message']


def remove_s3(bucket_type, scientist=''):
    print "[Removing S3 buckets]"
    s3 = boto3.resource('s3')
    client = boto3.client('s3')
    if bucket_type == 'ssn':
        bucket_name = (os.environ['conf_service_base_name'] + '-ssn-bucket').lower().replace('_', '-')
    elif bucket_type == 'edge':
        bucket_name = (os.environ['conf_service_base_name'] + '-' + "{}".format(scientist) + '-bucket').lower().replace('_', '-')
    bucket = s3.Bucket("{}".format(bucket_name))
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


def remove_subnets(subnet_id):
    print "[Removing subnets]"
    ec2 = boto3.resource('ec2')
    client = boto3.client('ec2')
    #tag_name = os.environ['conf_service_base_name'] + '-tag'
    #subnets = ec2.subnets.filter(
    #    Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': ['*']}])
    #for subnet in subnets:
    #    print subnet.id
    client.delete_subnet(SubnetId=subnet_id)
    #    print "The subnet " + subnet.id + " has been deleted successfully"


def remove_sgroups(tag_value):
    print "[Removing security groups]"
    ec2 = boto3.resource('ec2')
    client = boto3.client('ec2')
    tag_name = os.environ['conf_service_base_name']
    sgs = ec2.security_groups.filter(
        Filters=[{'Name': 'tag:{}'.format(tag_name), 'Values': [tag_value]}])
    for sg in sgs:
        print sg.id
        client.delete_security_group(GroupId=sg.id)
        print "The security group " + sg.id + " has been deleted successfully"


def deregister_image(scientist):
    print "[De-registering images]"
    client = boto3.client('ec2')
    response = client.describe_images(
        Filters=[{'Name': 'name', 'Values': ['{}-{}-notebook-image'.format(os.environ['conf_service_base_name'], scientist)]}])
    images_list = response.get('Images')
    for i in images_list:
        client.deregister_image(ImageId=i.get('ImageId'))


def terminate_emr(id):
    emr = boto3.client('emr')
    emr.terminate_job_flows(
        JobFlowIds=[id]
    )


def remove_kernels(emr_name, tag_name, nb_tag_value, ssh_user, key_path):
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
            sudo('rm -rf /srv/hadoopconf/config/{}'.format(emr_name))
            sudo('rm -rf /home/{}/.local/share/jupyter/kernels/*_{}'.format(ssh_user, emr_name))
            print "Notebook's " + env.hosts + " kernels were removed"
    else:
        print "There are no notebooks to clean kernels."
