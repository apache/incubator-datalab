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
import time
import logging
import traceback
import sys
import random
import string

def get_instance_hostname(instance_name):
    try:
        public = ''
        private = ''
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
        if public == '' and private == '':
            raise Exception("Unable to find instance hostname with instance name: " + instance_name)
    except Exception as err:
        logging.error("Error with finding instance hostname with instance name: " + instance_name + " : " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with finding instance hostname", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting VPC Endpoints", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting Route tables", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def get_bucket_by_name(bucket_name):
    try:
        s3 = boto3.resource('s3')
        for bucket in s3.buckets.all():
            if bucket.name == bucket_name:
                return bucket.name
        return ''
    except Exception as err:
        logging.error("Error with getting bucket by name: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting bucket by name", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def get_instance_ip_address(instance_name):
    try:
        ec2 = boto3.resource('ec2')
        instances = ec2.instances.filter(
            Filters=[{'Name': 'tag:Name', 'Values': [instance_name]},
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
        logging.error("Error with getting bucket by name: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting bucket by name", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def get_ami_id_by_name(ami_name, state="*"):
    ec2 = boto3.resource('ec2')
    try:
        for image in ec2.images.filter(Filters=[{'Name': 'name', 'Values': [ami_name]}, {'Name': 'state', 'Values': [state]}]):
            return image.id
    except Exception as err:
        logging.error("Error with getting AMI ID by name: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting AMI ID by name",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting Security Group ID by name",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting instance attribute",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def get_instance_by_name(instance_name):
    try:
        ec2 = boto3.resource('ec2')
        instances = ec2.instances.filter(
            Filters=[{'Name': 'tag:Name', 'Values': [instance_name]},
                     {'Name': 'instance-state-name', 'Values': ['running','pending','stopping','stopped']}])
        for instance in instances:
            return instance.id
        return ''
    except Exception as err:
        logging.error("Error with getting instance ID by name: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting instance ID by name",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting role by name",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def get_subnet_by_cidr(cidr):
    try:
        ec2 = boto3.resource('ec2')
        for subnet in ec2.subnets.filter(Filters=[{'Name': 'cidrBlock', 'Values': [cidr]}]):
            return subnet.id
        return ''
    except Exception as err:
        logging.error("Error with getting Subnet ID by CIDR: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting Subnet ID by CIDR",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def get_subnet_by_tag(tag):
    try:
        ec2 = boto3.resource('ec2')
        for subnet in ec2.subnets.filter(Filters=[
            {'Name': 'tag-key', 'Values': [tag.get('Key')]},
            {'Name': 'tag-value', 'Values': [tag.get('Value')]}
        ]):
            return subnet.cidr_block
        return ''
    except Exception as err:
        logging.error("Error with getting Subnet CIDR block by tag: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting Subnet CIDR block by tag",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting VPC ID by CIDR",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def get_emr_info(id, key=''):
    try:
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
    except Exception as err:
        logging.error("Error with getting EMR information: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting EMR information",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
            tag = response.get('Cluster').get('Tags')
            for j in tag:
                if tag_name in j.get(type):
                    clusters_list.append(i.get('Id'))
        return clusters_list
    except Exception as err:
        logging.error("Error with getting EMR list: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting EMR list",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
            if response.get('Cluster').get('Name') == name:
                cluster_id = i.get('Id')
        if cluster_id == '':
            raise Exception("Unable to find EMR cluster by name: " + name)
        return cluster_id
    except Exception as err:
        logging.error("Error with getting EMR ID by name: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting EMR ID by name",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting EC2 list",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
    except Exception as err:
        logging.error("Error with providing index: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with providing index",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with getting Route table by tag",
                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


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
        with open("/root/result.json", 'w') as result:
            res = {"error": "Unable to find AMI", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def get_iam_profile(profile_name, count=0):
    client = boto3.client('iam')
    iam_profile = ''
    try:
        if count < 10:
            response = client.get_instance_profile(InstanceProfileName=profile_name)
            iam_profile = response.get('InstanceProfile').get('InstanceProfileName')
            time.sleep(5)
            print 'IAM profile checked. Creating instance...'
        else:
            print "Unable to find IAM profile by name: " + profile_name
            return False
    except:
        count += 1
        print 'IAM profile is not available yet. Waiting...'
        time.sleep(5)
        get_iam_profile(profile_name, count)
    print iam_profile
    return iam_profile


def check_security_group(security_group_name, count=0):
    try:
        ec2 = boto3.resource('ec2')
        if count < 20:
            for security_group in ec2.security_groups.filter(Filters=[{'Name': 'group-name', 'Values': [security_group_name]}]):
                while security_group.id == '':
                    count = count + 1
                    time.sleep(10)
                    print "Security group is not available yet. Waiting..."
                    check_security_group(security_group_name, count)
                if security_group.id == '':
                    raise Exception("Unable to check Security group by name: " + security_group_name)
                return security_group.id
    except Exception as err:
        logging.error("Error with checking Security group by name: " + security_group_name + " : " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        with open("/root/result.json", 'w') as result:
            res = {"error": "Error with checking Security group by name", "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}
            print json.dumps(res)
            result.write(json.dumps(res))
        traceback.print_exc(file=sys.stdout)


def id_generator(size=10, chars=string.digits + string.ascii_letters):
    return ''.join(random.choice(chars) for _ in range(size))
