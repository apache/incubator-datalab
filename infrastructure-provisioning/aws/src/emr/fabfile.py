#!/usr/bin/python

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

import json
from dlab.fab import *
from dlab.aws_meta import *
import sys


def run():
    local_log_filename = "%s.log" % os.environ['request_id']
    local_log_filepath = "/response/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.INFO,
                        filename=local_log_filepath)

    create_aws_config_files()
    index = provide_index('EMR', os.environ['conf_service_base_name'] + '-Tag')
    print 'Generating infrastructure names and tags'
    emr_conf = dict()
    emr_conf['apps'] = 'Hadoop Hive Hue Spark'
    emr_conf['service_base_name'] = os.environ['conf_service_base_name']
    emr_conf['key_name'] = os.environ['creds_key_name']
    emr_conf['region'] = os.environ['creds_region']
    emr_conf['release_label'] = os.environ['emr_version']
    emr_conf['master_instance_type'] = os.environ['emr_master_instance_type']
    emr_conf['slave_instance_type'] = os.environ['emr_slave_instance_type']
    emr_conf['instance_count'] = os.environ['emr_instance_count']
    emr_conf['notebook_ip'] = get_instance_ip_address(os.environ['notebook_name']).get('Private')
    emr_conf['role_service_name'] = os.environ['emr_service_role']
    emr_conf['role_ec2_name'] = os.environ['emr_ec2_role']

    emr_conf['tags'] = 'Name=' + emr_conf['service_base_name'] + '-' + os.environ['edge_user_name'] + '-emr-' + str(index) + ', ' \
                       + emr_conf['service_base_name'] + '-Tag=' + emr_conf['service_base_name'] + '-' + os.environ['edge_user_name'] + '-emr-' + str(index)\
                       + ', Notebook=' + os.environ['notebook_name']
    emr_conf['cluster_name'] = emr_conf['service_base_name'] + '-' + os.environ['edge_user_name'] + '-emr-' + str(index)
    emr_conf['bucket_name'] = (emr_conf['service_base_name'] + '-ssn-bucket').lower().replace('_', '-')

    tag = {"Key": "{}-Tag".format(emr_conf['service_base_name']), "Value": "{}-{}-subnet".format(emr_conf['service_base_name'], os.environ['edge_user_name'])}
    emr_conf['subnet_cidr'] = get_subnet_by_tag(tag)

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

    with hide('stderr', 'running', 'warnings'):
        local("echo Waitning for changes to propagate; sleep 10")

    try:
        logging.info('[CREATE EMR CLUSTER]')
        print '[CREATE EMR CLUSTER]'
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
    except:
        sys.exit(1)

    try:
        logging.info('[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]')
        print '[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]'
        params = "--bucket {} --cluster_name {} --emr_version {} --keyfile {} --notebook_ip {}".format(emr_conf['bucket_name'], emr_conf['cluster_name'], emr_conf['release_label'], keyfile_name, emr_conf['notebook_ip'])
        if not run_routine('install_emr_kernels', params):
            logging.info('Failed installing EMR kernels')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed installing EMR kernels", "conf": emr_conf}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"hostname": cluster_name,
                   "key_name": emr_conf['key_name'],
                   "user_own_bucket_name": emr_conf['bucket_name'],
                   "exploratory_name": emr_conf['exploratory_name'],
                   "computational_name": emr_conf['computational_name'],
                   "Action": "Create new EMR cluster"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)

    sys.exit(0)


def terminate():
    local_log_filename = "%s.log" % os.environ['request_id']
    local_log_filepath = "/response/" + local_log_filename
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
    emr_conf['key_path'] = os.environ['creds_key_dir'] + os.environ['creds_key_name'] + '.pem'
    emr_conf['tag_name'] = emr_conf['service_base_name'] + '-Tag'

    try:
        emr_conf['exploratory_name'] = os.environ['exploratory_name']
        emr_conf['computational_name'] = os.environ['computational_name']
    except:
        emr_conf['exploratory_name'] = ''
        emr_conf['computational_name'] = ''

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
                   "NBs_name": emr_conf['notebook_name'],
                   "user_own_bucket_name": emr_conf['bucket_name'],
                   "exploratory_name": emr_conf['exploratory_name'],
                   "computational_name": emr_conf['computational_name'],
                   "Action": "Terminate EMR cluster"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)
