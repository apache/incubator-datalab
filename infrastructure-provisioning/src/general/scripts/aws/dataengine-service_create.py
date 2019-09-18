#!/usr/bin/python

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
import argparse
import re
import time
import sys
from fabric.api import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import json
import traceback
import logging
import ast

parser = argparse.ArgumentParser()
# parser.add_argument('--id', type=str, default='')
parser.add_argument('--dry_run', action='store_true', help='Print all variables')
parser.add_argument('--name', type=str, default='', help='Name to be applied to Cluster ( MANDATORY !!! )')
parser.add_argument('--applications', type=str, default='',
                    help='Set of applications to be installed on EMR (Default are: "Hadoop Hive Hue Spark")')
parser.add_argument('--master_instance_type', type=str, default='', help='EC2 instance size for Master-Node (Default: m3.xlarge)')
parser.add_argument('--slave_instance_type', type=str, default='', help='EC2 instance size for Worker-Nodes (Default: m3.xlarge)')
parser.add_argument('--instance_count', type=int, default='',
                    help='Number of nodes the cluster will consist of (Default: 3)')
parser.add_argument('--release_label', type=str, default='', help='EMR release version (Default: "emr-4.8.0")')
parser.add_argument('--steps', type=str, default='')
parser.add_argument('--tags', type=str, default='')
parser.add_argument('--auto_terminate', action='store_true')
parser.add_argument('--service_role', type=str, default='',
                    help='Role name EMR cluster (Default: "EMR_DefaultRole")')
parser.add_argument('--ec2_role', type=str, default='',
                    help='Role name for EC2 instances in cluster (Default: "EMR_EC2_DefaultRole")')
parser.add_argument('--ssh_key', type=str, default='')
parser.add_argument('--availability_zone', type=str, default='')
parser.add_argument('--subnet', type=str, default='', help='Subnet CIDR')
parser.add_argument('--cp_jars_2_s3', action='store_true',
                    help='Copy executable JARS to S3 (Need only once per EMR release version)')
parser.add_argument('--nbs_ip', type=str, default='', help='Notebook server IP cluster should be attached to')
parser.add_argument('--nbs_user', type=str, default='',
                    help='Username to be used for connection to Notebook server')
parser.add_argument('--s3_bucket', type=str, default='', help='S3 bucket name to work with')
parser.add_argument('--emr_timeout', type=int)
parser.add_argument('--configurations', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--key_dir', type=str, default='')
parser.add_argument('--project_name', type=str, default='')
parser.add_argument('--slave_instance_spot', type=str, default='False')
parser.add_argument('--bid_price', type=str, default='')
parser.add_argument('--service_base_name', type=str, default='')
parser.add_argument('--additional_emr_sg', type=str, default='')
args = parser.parse_args()

try:
    os.environ['conf_additional_tags'] = os.environ['conf_additional_tags'] + ';project_tag:{0};endpoint_tag:{1};'.format(os.environ['project_name'], os.environ['endpoint_name'])
except KeyError:
    os.environ['conf_additional_tags'] = 'project_tag:{0};endpoint_tag:{1}'.format(os.environ['project_name'], os.environ['endpoint_name'])

if args.region == 'us-east-1':
    endpoint_url = 'https://s3.amazonaws.com'
elif args.region == 'cn-north-1':
    endpoint_url = "https://s3.{}.amazonaws.com.cn".format(args.region)
else:
    endpoint_url = 'https://s3-{}.amazonaws.com'.format(args.region)

cp_config = "Name=CUSTOM_JAR, Args=aws " \
            "s3 cp /etc/hive/conf/hive-site.xml s3://{0}/{4}/{5}/config/hive-site.xml " \
            "--sse AES256 --endpoint-url {6} --region {3}, " \
            "ActionOnFailure=TERMINATE_CLUSTER,Jar=command-runner.jar; " \
            "Name=CUSTOM_JAR, Args=aws " \
            "s3 cp /etc/hadoop/conf/ s3://{0}/{4}/{5}/config/ " \
            "--sse AES256 --recursive --endpoint-url {6} --region {3}, " \
            "ActionOnFailure=TERMINATE_CLUSTER, Jar=command-runner.jar; " \
            "Name=CUSTOM_JAR, Args=sudo -u hadoop hdfs dfs -mkdir /user/{2}, " \
            "ActionOnFailure=TERMINATE_CLUSTER,Jar=command-runner.jar; " \
            "Name=CUSTOM_JAR, Args=aws s3 cp s3://{0}/{4}/{4}.pub /tmp/{4}.pub " \
            "--sse AES256 --endpoint-url {6} --region {3}," \
            " ActionOnFailure=TERMINATE_CLUSTER,Jar=command-runner.jar; " \
            "Name=CUSTOM_JAR, Args=sudo -u " \
            "hadoop hdfs dfs -chown -R {2}:{2} /user/{2}, " \
            "ActionOnFailure=TERMINATE_CLUSTER,Jar=command-runner.jar".\
    format(args.s3_bucket,
           args.name,
           args.nbs_user,
           args.region,
           args.project_name,
           args.name,
           endpoint_url)

cp_jars = "Name=CUSTOM_JAR, Args=aws " \
          "s3 cp s3://{0}/jars_parser.py /tmp/jars_parser.py " \
          "--sse AES256 --endpoint-url {6} --region {2}, " \
          "ActionOnFailure=TERMINATE_CLUSTER,Jar=command-runner.jar;" \
          "Name=CUSTOM_JAR, Args=aws " \
          "s3 cp s3://{0}/key_importer.py /tmp/key_importer.py " \
          "--sse AES256 --endpoint-url {6} --region {2}, " \
          "ActionOnFailure=TERMINATE_CLUSTER,Jar=command-runner.jar;" \
          "Name=CUSTOM_JAR, Args=sudo " \
          "/usr/bin/python /tmp/key_importer.py --user_name {4}, " \
          "ActionOnFailure=TERMINATE_CLUSTER,Jar=command-runner.jar; " \
          "Name=CUSTOM_JAR, Args=/usr/bin/python /tmp/jars_parser.py " \
          "--bucket {0} --emr_version {3} --region {2} --user_name {4} " \
          "--cluster_name {5}, " \
          "ActionOnFailure=TERMINATE_CLUSTER,Jar=command-runner.jar".\
    format(args.s3_bucket,
           args.release_label,
           args.region,
           args.release_label,
           args.project_name,
           args.name,
           endpoint_url)

logfile = '{}_creation.log'.format(args.name)
logpath = '/response/' + logfile
out = open(logpath, 'w')
out.close()


def get_object_count(bucket, prefix):
    try:
        s3_cli = boto3.client('s3', config=Config(signature_version='s3v4'),
                              region_name=args.region)
        content = s3_cli.get_paginator('list_objects')
        file_list = []
        try:
            for i in content.paginate(Bucket=bucket, Delimiter='/',
                                      Prefix=prefix):
                for file in i.get('Contents'):
                    file_list.append(file.get('Key'))
            count = len(file_list)
        except:
            print("{} still not exist. Waiting...".format(prefix))
            count = 0
        return count
    except Exception as err:
        logging.error("Unable to get objects from s3: " +
                     str(err) + "\n Traceback: " +
                     traceback.print_exc(file=sys.stdout))


def upload_jars_parser(args):
    try:
        s3 = boto3.resource('s3', config=Config(signature_version='s3v4'))
        s3.meta.client.upload_file('/root/scripts/dataengine-service_jars_parser.py',
                                   args.s3_bucket, 'jars_parser.py',
                                   ExtraArgs={'ServerSideEncryption': 'AES256'})

    except Exception as err:
        logging.error("Unable to upload jars to s3: " +
                     str(err) + "\n Traceback: " +
                     traceback.print_exc(file=sys.stdout))


def upload_user_key(args):
    try:
        s3 = boto3.resource('s3', config=Config(signature_version='s3v4'))
        s3.meta.client.upload_file(args.key_dir + '/' +
                                   args.project_name + '.pub',
                                   args.s3_bucket, args.project_name +
                                   '/' + args.project_name + '.pub',
                                   ExtraArgs={'ServerSideEncryption': 'AES256'})
        s3.meta.client.upload_file(
            '/root/scripts/dataengine-service_key_importer.py',
            args.s3_bucket,
            'key_importer.py',
            ExtraArgs={'ServerSideEncryption': 'AES256'})

    except Exception as err:
        logging.error("Unable to upload user key to s3: " +
                     str(err) + "\n Traceback: " +
                     traceback.print_exc(file=sys.stdout))


def remove_user_key(args):
    try:
        client = boto3.client('s3',
                              config=Config(signature_version='s3v4'),
                              region_name=args.region)
        client.delete_object(Bucket=args.s3_bucket,
                             Key=args.project_name + '.pub')

    except Exception as err:
        logging.error("Unable to remove user key: " +
                     str(err) + "\n Traceback: " +
                     traceback.print_exc(file=sys.stdout))


def get_instance_by_ip(ip):
    try:
        ec2 = boto3.resource('ec2')
        check = bool(re.match(r"^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$", ip))
        if check:
            instances = ec2.instances.filter(Filters=[{
                'Name': 'private-ip-address', 'Values': [ip]
            }])
        else:
            instances = ec2.instances.filter(Filters=[{
                'Name': 'private-dns-name', 'Values': [ip]
            }])
        for instance in instances:
            return instance

    except Exception as err:
        logging.error("Unable to get instance by ip: " +
                     str(err) + "\n Traceback: " +
                     traceback.print_exc(file=sys.stdout))


def emr_sg(id):
    client = boto3.client('emr')
    emr = client.describe_cluster(ClusterId=id)
    master = emr['Cluster']['Ec2InstanceAttributes']['EmrManagedMasterSecurityGroup']
    slave = emr['Cluster']['Ec2InstanceAttributes']['EmrManagedSlaveSecurityGroup']
    return master, slave


def wait_emr(bucket, cluster_name, timeout, delay=30):
    deadline = time.time() + timeout
    prefix = args.project_name + '/' + cluster_name + "/config/"
    global cluster_id
    while time.time() < deadline:
        state = action_validate(cluster_id)
        if get_object_count(bucket, prefix) > 20 and state[1] == "WAITING":
            return True
        elif state[0] == "False":
            return False
        else:
            time.sleep(delay)
    return False


def parse_steps(step_string):
    try:
        parser = re.split('; |;', step_string)
        steps = []
        for i in parser:
            step_parser = re.split(', |,', i)
            task = {}
            hdp_jar_step = {}
            for j in step_parser:
                key, value = j.split("=")
                if key == "Args":
                    value = value.split(" ")
                    hdp_jar_step.update({key: value})
                elif key == "Jar":
                    hdp_jar_step.update({key: value})
                else:
                    task.update({key: value})
            task.update({"HadoopJarStep": hdp_jar_step})
            steps.append(task)
        return steps
    except Exception as err:
        logging.error("Failed to parse steps: " +
                     str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))


def action_validate(id):
    state = get_emr_info(id, 'Status')['State']
    if state in ("TERMINATING", "TERMINATED", "TERMINATED_WITH_ERRORS"):
        print("Cluster is alredy stopped. Bye")
        return ["False", state]
    elif state in ("RUNNING", "WAITING"):
        return ["True", state]
    else:
        print("Cluster is still being built.")
        return ["True", state]


def build_emr_cluster(args):
    try:
        # Parse applications
        apps = args.applications.split(" ")
        names = []
        for i in apps:
            names.append({"Name": i})
    
        # Parse Tags
        parser = re.split('[, ]+', args.tags)
        tags = list()
        for i in parser:
            key, value = i.split("=")
            tags.append({"Value": value, "Key": key})
        tags.append({'Key': os.environ['conf_tag_resource_id'],
                     'Value': '{}:{}'.format(args.service_base_name, args.name)})
        tags.append({'Key': os.environ['conf_billing_tag_key'],
                     'Value': os.environ['conf_billing_tag_value']})
        prefix = "jars/" + args.release_label + "/lib/"
        jars_exist = get_object_count(args.s3_bucket, prefix)
    
        # Parse steps
        if args.steps != '':
            global cp_config
            cp_config = cp_config + "; " + args.steps
        if args.cp_jars_2_s3 or jars_exist == 0:
            steps = parse_steps(cp_config + "; " + cp_jars)
        else:
            steps = parse_steps(cp_config)
    
        if args.dry_run:
            print("Build parameters are:")
            print(args)
            print("\n")
            print("Applications to be installed:")
            print(names)
            print("\n")
            print("Cluster tags:")
            print(tags)
            print("\n")
            print("Cluster Jobs:")
            print(steps)
    
        if not args.dry_run:
            socket = boto3.client('emr')
            if args.slave_instance_spot == 'True':
                result = socket.run_job_flow(
                    Name=args.name,
                    ReleaseLabel=args.release_label,
                    Instances={'Ec2KeyName': args.ssh_key,
                               'KeepJobFlowAliveWhenNoSteps': not args.auto_terminate,
                               'Ec2SubnetId': get_subnet_by_cidr(args.subnet),
                               'InstanceGroups': [
                                   {'Market': 'SPOT',
                                    'BidPrice': args.bid_price[:5],
                                    'InstanceRole': 'CORE',
                                    'InstanceType': args.slave_instance_type,
                                    'InstanceCount': int(
                                        args.instance_count) - 1},
                                   {'Market': 'ON_DEMAND',
                                    'InstanceRole': 'MASTER',
                                    'InstanceType': args.master_instance_type,
                                    'InstanceCount': 1}],
                               'AdditionalMasterSecurityGroups': [
                                   get_security_group_by_name(
                                       args.additional_emr_sg)
                               ],
                               'AdditionalSlaveSecurityGroups': [
                                   get_security_group_by_name(
                                       args.additional_emr_sg)
                               ]
                               },
                    Applications=names,
                    Tags=tags,
                    Steps=steps,
                    VisibleToAllUsers=not args.auto_terminate,
                    JobFlowRole=args.ec2_role,
                    ServiceRole=args.service_role,
                    Configurations=ast.literal_eval(args.configurations))
            else:
                result = socket.run_job_flow(
                    Name=args.name,
                    ReleaseLabel=args.release_label,
                    Instances={'MasterInstanceType': args.master_instance_type,
                               'SlaveInstanceType': args.slave_instance_type,
                               'InstanceCount': args.instance_count,
                               'Ec2KeyName': args.ssh_key,
                               # 'Placement': {'AvailabilityZone': args.availability_zone},
                               'KeepJobFlowAliveWhenNoSteps': not args.auto_terminate,
                               'Ec2SubnetId': get_subnet_by_cidr(args.subnet),
                               'AdditionalMasterSecurityGroups': [
                                   get_security_group_by_name(
                                       args.additional_emr_sg)
                               ],
                               'AdditionalSlaveSecurityGroups': [
                                   get_security_group_by_name(
                                       args.additional_emr_sg)
                               ]
                               },
                    Applications=names,
                    Tags=tags,
                    Steps=steps,
                    VisibleToAllUsers=not args.auto_terminate,
                    JobFlowRole=args.ec2_role,
                    ServiceRole=args.service_role,
                    Configurations=ast.literal_eval(args.configurations))
            print("Cluster_id {}".format(result.get('JobFlowId')))
            return result.get('JobFlowId')
    except Exception as err:
        logging.error("Failed to build EMR cluster: " +
                     str(err) + "\n Traceback: " +
                     traceback.print_exc(file=sys.stdout))


##############
# Run script #
##############

if __name__ == "__main__":

    if args.name == '':
        parser.print_help()
    elif args.dry_run:
        # get_emr_state(args.id)
        upload_jars_parser(args)
        upload_user_key(args)
        build_emr_cluster(args)
    else:
        if not get_role_by_name(args.service_role):
            print("There is no default EMR service role. Creating...")
            create_iam_role(args.service_role,
                            args.service_role,
                            args.region,
                            service='elasticmapreduce')
            attach_policy(args.service_role,
                          policy_arn='arn:aws:iam::aws:policy/service-role/AmazonElasticMapReduceRole')
        if not get_role_by_name(args.ec2_role):
            print("There is no default EMR EC2 role. Creating...")
            create_iam_role(args.ec2_role,
                            args.ec2_role,
                            args.region)
            attach_policy(args.ec2_role,
                          policy_arn='arn:aws:iam::aws:policy/service-role/AmazonElasticMapReduceforEC2Role')
        upload_jars_parser(args)
        upload_user_key(args)
        out = open(logpath, 'a')
        nbs_id = get_instance_by_ip(args.nbs_ip)
        out.write('Notebook server "{}" IP is "{}"\n'.format(nbs_id, args.nbs_ip))
        current_sg = nbs_id.security_groups
        out.write('Current Notebooks SGs: {}\n'.format(current_sg))
        out.write('[BUILDING NEW CLUSTER - {}\n]'.format(args.name))
        cluster_id = build_emr_cluster(args)
        out.write('Cluster ID: {}\n'.format(cluster_id))
        if args.slave_instance_spot == 'True':
            time.sleep(300)
            spot_instances_status = get_spot_instances_status(cluster_id)
            bool_, code, message = spot_instances_status
            if bool_:
                print("Spot instances status: {}, Message:{}".format(code, message))
            else:
                print("SPOT REQUEST WASN'T FULFILLED, BECAUSE: "
                      "STATUS CODE IS {}, MESSAGE IS {}".format(code, message))
                append_result("Error with Spot request. Status code: {}, Message: {}".format(code, message))
                sys.exit(1)
        if wait_emr(args.s3_bucket, args.name, args.emr_timeout):
            # Append Cluster's SGs to the Notebook server to grant access
            sg_list = list()
            for i in current_sg:
                sg_list.append(i['GroupId'])
            sg_master, sg_slave = emr_sg(cluster_id)
            sg_list.extend([sg_master, sg_slave])
            out.write('Updating SGs for Notebook to: {}\n'.format(sg_list))
            nbs_id.modify_attribute(Groups=sg_list)
            out.close()
        else:
            out.write("Timeout of {} seconds reached. "
                      "Please increase timeout period and try again. "
                      "Now terminating the cluster...".format(args.emr_timeout))
            out.close()
            if action_validate(cluster_id)[0] == "True":
                terminate_emr(cluster_id)
            s3_cleanup(args.s3_bucket, args.name, args.project_name)
            sys.exit(1)
