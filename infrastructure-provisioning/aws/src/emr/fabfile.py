#!/usr/bin/python

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

import json
import time
from fabric.api import *
from dlab.fab import *
from dlab.aws_meta import *
from dlab.aws_actions import *
import sys
import os
import uuid
import logging


def emr_waiter(tag_name):
    if len(get_emr_list(tag_name, 'Value', False, True)) > 0 or os.path.exists('/response/.emr_creating_' + os.environ['exploratory_name']):
        with hide('stderr', 'running', 'warnings'):
            local("echo 'Some EMR cluster is still being created, waiting..'")
        time.sleep(60)
        emr_waiter(tag_name)
    else:
        return True


def run():
    local_log_filename = "{}_{}_{}.log".format(os.environ['resource'], os.environ['edge_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['resource'] +  "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.INFO,
                        filename=local_log_filepath)
    try:
        os.environ['exploratory_name']
    except:
        os.environ['exploratory_name'] = ''
    if os.path.exists('/response/.emr_creating_' + os.environ['exploratory_name']):
        time.sleep(30)
    create_aws_config_files()
    #index = provide_index('EMR', os.environ['conf_service_base_name'] + '-Tag', '{}-{}-emr'.format(os.environ['conf_service_base_name'], os.environ['edge_user_name']))
    #time_stamp = int(time.time())
    print 'Generating infrastructure names and tags'
    emr_conf = dict()
    emr_conf['uuid'] = str(uuid.uuid4())[:5]
    try:
        emr_conf['exploratory_name'] = os.environ['exploratory_name']
    except:
        emr_conf['exploratory_name'] = ''
    try:
        emr_conf['computational_name'] = os.environ['computational_name']
    except:
        emr_conf['computational_name'] = ''
    emr_conf['apps'] = 'Hadoop Hive Hue Spark'
    emr_conf['service_base_name'] = os.environ['conf_service_base_name']
    emr_conf['tag_name'] = emr_conf['service_base_name'] + '-Tag'
    emr_conf['key_name'] = os.environ['creds_key_name']
    emr_conf['region'] = os.environ['creds_region']
    emr_conf['release_label'] = os.environ['emr_version']
    emr_conf['master_instance_type'] = os.environ['emr_master_instance_type']
    emr_conf['slave_instance_type'] = os.environ['emr_slave_instance_type']
    emr_conf['instance_count'] = os.environ['emr_instance_count']
    emr_conf['notebook_ip'] = get_instance_ip_address(os.environ['notebook_name']).get('Private')
    emr_conf['role_service_name'] = os.environ['emr_service_role']
    emr_conf['role_ec2_name'] = os.environ['emr_ec2_role']

    #emr_conf['tags'] = 'Name=' + emr_conf['service_base_name'] + '-' + os.environ['edge_user_name'] + '-emr-' + str(time_stamp) + ', ' \
    #                   + emr_conf['service_base_name'] + '-Tag=' + emr_conf['service_base_name'] + '-' + os.environ['edge_user_name'] + '-emr-' + str(time_stamp)\
    #                   + ', Notebook=' + os.environ['notebook_name']
    emr_conf['tags'] = 'Name=' + emr_conf['service_base_name'] + '-' + os.environ['edge_user_name'] + '-emr-' + emr_conf['exploratory_name'] + '-' + emr_conf['computational_name'] + '-' + emr_conf['uuid'] + ', ' \
                       + emr_conf['service_base_name'] + '-Tag=' + emr_conf['service_base_name'] + '-' + os.environ['edge_user_name'] + '-emr-' + emr_conf['exploratory_name'] + '-' + emr_conf['computational_name'] + '-' + emr_conf['uuid']\
                       + ', Notebook=' + os.environ['notebook_name']
    #emr_conf['cluster_name'] = emr_conf['service_base_name'] + '-' + os.environ['edge_user_name'] + '-emr-' + str(time_stamp)
    emr_conf['cluster_name'] = emr_conf['service_base_name'] + '-' + os.environ['edge_user_name'] + '-emr-' + emr_conf['exploratory_name'] + '-' + emr_conf['computational_name'] + '-' + emr_conf['uuid']
    emr_conf['bucket_name'] = (emr_conf['service_base_name'] + '-ssn-bucket').lower().replace('_', '-')

    tag = {"Key": "{}-Tag".format(emr_conf['service_base_name']), "Value": "{}-{}-subnet".format(emr_conf['service_base_name'], os.environ['edge_user_name'])}
    emr_conf['subnet_cidr'] = get_subnet_by_tag(tag)
    emr_conf['key_path'] = os.environ['creds_key_dir'] + '/' + os.environ['creds_key_name'] + '.pem'

    try:
        emr_conf['emr_timeout'] = os.environ['emr_timeout']
    except:
        emr_conf['emr_timeout'] = "1200"

    try:
        emr_conf['exploratory_name'] = os.environ['exploratory_name']
        emr_conf['computational_name'] = os.environ['computational_name']
    except:
        emr_conf['exploratory_name'] = ''
        emr_conf['computational_name'] = ''

    print "Will create exploratory environment with edge node as access point as following: " + \
          json.dumps(emr_conf, sort_keys=True, indent=4, separators=(',', ': '))
    logging.info(json.dumps(emr_conf))

    try:
        emr_waiter(os.environ['notebook_name'])
        local('touch /response/.emr_creating_' + os.environ['exploratory_name'])
    except:
        with open("/root/result.json", 'w') as result:
            res = {"error": "EMR waiter fail", "conf": emr_conf}
            print json.dumps(res)
            result.write(json.dumps(res))
        sys.exit(1)

    with hide('stderr', 'running', 'warnings'):
        local("echo Waiting for changes to propagate; sleep 10")

    try:
        logging.info('[Creating EMR Cluster]')
        print '[Creating EMR Cluster]'
        params = "--name {} --applications '{}' --master_instance_type {} --slave_instance_type {} --instance_count {} --ssh_key {} --release_label {} --emr_timeout {} " \
                 "--subnet {} --service_role {} --ec2_role {} --nbs_ip {} --nbs_user {} --s3_bucket {} --region {} --tags '{}'".format(
            emr_conf['cluster_name'], emr_conf['apps'], emr_conf['master_instance_type'], emr_conf['slave_instance_type'], emr_conf['instance_count'], emr_conf['key_name'], emr_conf['release_label'], emr_conf['emr_timeout'],
            emr_conf['subnet_cidr'], emr_conf['role_service_name'], emr_conf['role_ec2_name'], emr_conf['notebook_ip'], 'ubuntu', emr_conf['bucket_name'], emr_conf['region'], emr_conf['tags'])
        if not run_routine('create_cluster', params):
            logging.info('Failed creating EMR Cluster')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed to create EMR Cluster", "conf": emr_conf}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)

        cluster_name = emr_conf['cluster_name']
        keyfile_name = "/root/keys/%s.pem" % emr_conf['key_name']
        local('rm /response/.emr_creating_' + os.environ['exploratory_name'])
    except:
        local('rm /response/.emr_creating_' + os.environ['exploratory_name'])
        sys.exit(1)

    try:
        logging.info('[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]')
        print '[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]'
        params = "--bucket {} --cluster_name {} --emr_version {} --keyfile {} --notebook_ip {} --region {}".format(emr_conf['bucket_name'], emr_conf['cluster_name'], emr_conf['release_label'], keyfile_name, emr_conf['notebook_ip'], emr_conf['region'])
        if not run_routine('install_emr_kernels', params):
            logging.info('Failed installing EMR kernels')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed installing EMR kernels", "conf": emr_conf}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)
    except:
        emr_id = get_emr_id_by_name(emr_conf['cluster_name'])
        terminate_emr(emr_id)
        remove_kernels(emr_conf['cluster_name'],emr_conf['tag_name'],os.environ['notebook_name'],'ubuntu',emr_conf['key_path'], emr_conf['release_label'])
        sys.exit(1)

    try:
        logging.info('[SUMMARY]')
        print '[SUMMARY]'
        print "Service base name: " + emr_conf['service_base_name']
        print "Cluster name: " + emr_conf['cluster_name']
        print "Key name: " + emr_conf['key_name']
        print "Region: " + emr_conf['region']
        print "EMR version: " + emr_conf['release_label']
        print "EMR master node shape: " + emr_conf['master_instance_type']
        print "EMR slave node shape: " + emr_conf['slave_instance_type']
        print "Instance count: " + emr_conf['instance_count']
        print "Notebook IP address: " + emr_conf['notebook_ip']
        print "Bucket name: " + emr_conf['bucket_name']
        with open("/root/result.json", 'w') as result:
            res = {"hostname": cluster_name,
                   "key_name": emr_conf['key_name'],
                   "user_own_bucket_name": emr_conf['bucket_name'],
                   "Action": "Create new EMR cluster"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)

    sys.exit(0)


def terminate():
    local_log_filename = "{}_{}_{}.log".format(os.environ['resource'], os.environ['edge_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['resource'] +  "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    create_aws_config_files()
    print 'Generating infrastructure names and tags'
    emr_conf = dict()
    emr_conf['service_base_name'] = os.environ['conf_service_base_name']
    emr_conf['emr_name'] = os.environ['emr_cluster_name']
    emr_conf['notebook_name'] = os.environ['notebook_instance_name']
    emr_conf['bucket_name'] = (emr_conf['service_base_name'] + '-ssn-bucket').lower().replace('_', '-')
    emr_conf['ssh_user'] = os.environ['notebook_ssh_user']
    emr_conf['key_path'] = os.environ['creds_key_dir'] + '/' + os.environ['creds_key_name'] + '.pem'
    emr_conf['tag_name'] = emr_conf['service_base_name'] + '-Tag'

    try:
        logging.info('[TERMINATE EMR CLUSTER]')
        print '[TERMINATE EMR CLUSTER]'
        params = "--emr_name %s --bucket_name %s --key_path %s --ssh_user %s --tag_name %s --nb_tag_value %s" % \
                 (emr_conf['emr_name'], emr_conf['bucket_name'], emr_conf['key_path'], emr_conf['ssh_user'],
                  emr_conf['tag_name'], emr_conf['notebook_name'])
        if not run_routine('terminate_emr', params):
            logging.info('Failed to terminate EMR cluster')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed to terminate EMR cluster", "conf": emr_conf}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"EMR_name": emr_conf['emr_name'],
                   "notebook_name": emr_conf['notebook_name'],
                   "user_own_bucket_name": emr_conf['bucket_name'],
                   "Action": "Terminate EMR cluster"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)
