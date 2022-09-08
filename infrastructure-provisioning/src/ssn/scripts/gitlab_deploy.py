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


from ConfigParser import ConfigParser
from fabric import *
import argparse
import boto3
from botocore.client import Config as botoConfig
import sys
import os


parser = argparse.ArgumentParser()
parser.add_argument('--action', required=True, type=str, default='', choices=['create', 'terminate'],
                    help='Available options: create, terminate')
args = parser.parse_args()


def read_ini():
    try:
        head, tail = os.path.split(os.path.realpath(__file__))
        for filename in os.listdir(head):
            if filename.endswith('.ini'):
                config = ConfigParser()
                config.read(os.path.join(head, filename))
                for section in config.sections():
                    for option in config.options(section):
                        var = "{0}_{1}".format(section, option)
                        if var not in os.environ:
                            os.environ[var] = config.get(section, option)
    except Exception as err:
        print('Failed to read conf file.{}'.format(str(err)))
        sys.exit(1)


def create_instance():
    try:
        subprocess.run('mkdir -p ~/.aws', shell=True, check=True)
        subprocess.run('touch ~/.aws/config', shell=True, check=True)
        subprocess.run('echo "[default]" > ~/.aws/config', shell=True, check=True)
        subprocess.run('echo "region = {}" >> ~/.aws/config'.format(os.environ['aws_region']), shell=True, check=True)
        ec2 = boto3.resource('ec2')
        security_groups_ids = []
        ami_id = get_ami_id(os.environ['aws_{}_ami_name'.format(os.environ['conf_os_family'])])
        for chunk in os.environ['aws_sg_ids'].split(','):
            security_groups_ids.append(chunk.strip())
        instances = ec2.create_instances(ImageId=ami_id, MinCount=1, MaxCount=1,
                                         KeyName=os.environ['conf_key_name'],
                                         SecurityGroupIds=security_groups_ids,
                                         InstanceType=os.environ['aws_instance_type'],
                                         SubnetId=os.environ['aws_subnet_id'])
        for instance in instances:
            print('Waiting for instance {} become running.'.format(instance.id))
            instance.wait_until_running()
            node_name = '{0}-{1}'.format(os.environ['conf_service_base_name'], os.environ['conf_node_name'])
            instance.create_tags(Tags=[{'Key': 'Name', 'Value': node_name}])
            return instance.id
        return ''
    except Exception as err:
        print("Failed to create instance.{}".format(str(err)))
        sys.exit(1)


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
        print("Failed to get AMI ID.{}".format(str(err)))


def create_elastic_ip(instance_id):
    try:
        client = boto3.client('ec2')
        response = client.allocate_address(Domain='vpc')
        allocation_id = response.get('AllocationId')
        response = client.associate_address(InstanceId=instance_id, AllocationId=allocation_id)
        print('Association ID: {}'.format(response.get('AssociationId')))
    except Exception as err:
        print('Failed to allocate elastic IP.{}'.format(str(err)))
        sys.exit(1)


def get_ec2_ip(instance_id):
    try:
        ec2 = boto3.resource('ec2')
        instances = ec2.instances.filter(
        Filters=[{'Name': 'instance-id', 'Values': [instance_id]}])
        for instance in instances:
            return getattr(instance, 'public_dns_name')
    except Exception as e:
        print('Failed to get instance IP.{}'.format(str(e)))
        sys.exit(1)


def put_to_bucket(bucket_name, local_file, destination_file):
    try:
        s3 = boto3.client('s3', config=botoConfig(signature_version='s3v4'), region_name=os.environ['aws_region'])
        with open(local_file, 'rb') as data:
            s3.upload_fileobj(data, bucket_name, destination_file, ExtraArgs={'ServerSideEncryption': 'AES256'})
    except Exception as err:
        print('Unable to upload files to S3 bucket.{}'.format(str(err)))
        sys.exit(1)


def terminate_gitlab():
    try:
        ec2 = boto3.resource('ec2')
        client = boto3.client('ec2')
        node_name = '{0}-{1}'.format(os.environ['conf_service_base_name'], os.environ['conf_node_name'])
        print('Terminating "{}" instance...'.format(node_name))
        inst = ec2.instances.filter(
            Filters=[{'Name': 'instance-state-name', 'Values': ['running', 'stopped', 'pending', 'stopping']},
                     {'Name': 'tag:Name', 'Values': ['{}'.format(node_name)]}])
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
                                    client.disassociate_address(AssociationId=association_id)
                                    client.release_address(AllocationId=allocation_id)
                                    print('Releasing Elastic IP: {}'.format(elastic_ip))
                            except:
                                print('There is no such Elastic IP: {}'.format(elastic_ip))
                except Exception as err:
                    print('There is no Elastic IP to disassociate from instance: {}'.format(instance.id), str(err))
                client.terminate_instances(InstanceIds=[instance.id])
                waiter = client.get_waiter('instance_terminated')
                waiter.wait(InstanceIds=[instance.id])
                print('The instance {} has been terminated successfully'.format(instance.id))
        else:
            print('There are no instances with "{}" tag to terminate'.format(node_name))
    except Exception as err:
        print('Failed to terminate gitlab instance. {}'.format(str(err)))


if __name__ == "__main__":
    # Read all configs
    read_ini()

    if args.action == 'create':
        instance_id = create_instance()
        print('Instance {} created.'.format(instance_id))
        create_elastic_ip(instance_id)
        os.environ['instance_id'] = instance_id
        os.environ['instance_hostname'] = get_ec2_ip(instance_id)
        print('Instance hostname: {}'.format(os.environ['instance_hostname']))

        keyfile = '{}'.format('{}{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name']))
        params = '--keyfile {0} --instance_ip {1}'.format(keyfile, os.environ['instance_hostname'])
        head, tail = os.path.split(os.path.realpath(__file__))

        # Main script for configure gitlab
        try:
            subprocess.run('{0}/{1}.py {2}'.format(head, 'configure_gitlab', params), shell=True, check=True)
        except Exception as err:
            print('Failed to configure gitlab. {}'.format(str(err)))
            terminate_gitlab()
            sys.exit(1)

        bucket_name = ('{0}-{1}-{2}-bucket'.format(os.environ['service_base_name'], os.environ['project_name'],
                                                   os.environ['endpoint_name'])).lower().replace('_', '-')
        for filename in os.listdir(head):
            if filename.endswith('.crt'):
                put_to_bucket(bucket_name, os.path.join(head, filename), filename)

    elif args.action == 'terminate':
        terminate_gitlab()

    else:
        print('Unknown action. Try again.')
