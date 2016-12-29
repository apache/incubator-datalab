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
import logging
import json
import sys
from dlab.fab import *
from dlab.aws_meta import *
from dlab.aws_actions import *
import os
import uuid


# Function for creating AMI from already provisioned notebook
def create_image_from_instance(instance_name='', image_name=''):
    ec2 = boto3.resource('ec2')
    instances = ec2.instances.filter(
        Filters=[{'Name': 'tag:Name', 'Values': [instance_name]},
                 {'Name': 'instance-state-name', 'Values': ['running']}])
    for instance in instances:
        image = instance.create_image(Name=image_name,
                                      Description='Automatically created image for notebook server',
                                      NoReboot=True)
        image.load()
        while image.state != 'available':
            local("echo Waiting for image creation; sleep 20")
            image.load()
        image.create_tags(Tags=[{'Key': 'Name', 'Value': os.environ['conf_service_base_name']}])
        return image.id
    return ''


# Main function for provisioning notebook server
def run():
    # enable debug level for boto3
    logging.getLogger('botocore').setLevel(logging.DEBUG)
    logging.getLogger('boto3').setLevel(logging.DEBUG)

    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['resource'], os.environ['notebook_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['resource'] +  "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    create_aws_config_files()
    print 'Generating infrastructure names and tags'
    notebook_config = dict()
    notebook_config['uuid'] = str(uuid.uuid4())[:5]
    try:
        notebook_config['exploratory_name'] = os.environ['exploratory_name']
    except:
        notebook_config['exploratory_name'] = ''
    notebook_config['service_base_name'] = os.environ['conf_service_base_name']
    notebook_config['instance_type'] = os.environ['notebook_instance_type']
    notebook_config['key_name'] = os.environ['creds_key_name']
    notebook_config['user_keyname'] = os.environ['notebook_user_name']
    # notebook_config['instance_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
    #     'notebook_user_name'] + '-nb-' + str(provide_index('EC2', '{}-Tag'.format(os.environ['conf_service_base_name']), '{}-{}-nb'.format(os.environ['conf_service_base_name'], os.environ['notebook_user_name'])))
    notebook_config['instance_name'] = os.environ['conf_service_base_name'] + "-" + os.environ['notebook_user_name'] + "-nb-" + notebook_config['exploratory_name'] + "-" + notebook_config['uuid']
    notebook_config['expected_ami_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
        'notebook_user_name'] + '-notebook-image'
    notebook_config['role_profile_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
        'notebook_user_name'] + "-nb-Profile"
    notebook_config['security_group_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
        'notebook_user_name'] + "-nb-SG"
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-Tag'

    print 'Searching preconfigured images'
    ami_id = get_ami_id_by_name(notebook_config['expected_ami_name'], 'available')
    if ami_id != '':
        print 'Preconfigured image found. Using: ' + ami_id
        notebook_config['ami_id'] = ami_id
    else:
        print 'No preconfigured image found. Using default one: ' + get_ami_id(os.environ['notebook_ami_name'])
        notebook_config['ami_id'] = get_ami_id(os.environ['notebook_ami_name'])

    tag = {"Key": notebook_config['tag_name'], "Value": "{}-{}-subnet".format(notebook_config['service_base_name'], os.environ['notebook_user_name'])}
    notebook_config['subnet_cidr'] = get_subnet_by_tag(tag)

    # launching instance for notebook server
    try:
        logging.info('[CREATE JUPYTER NOTEBOOK INSTANCE]')
        print '[CREATE JUPYTER NOTEBOOK INSTANCE]'
        params = "--node_name %s --ami_id %s --instance_type %s --key_name %s --security_group_ids %s " \
                 "--subnet_id %s --iam_profile %s --infra_tag_name %s --infra_tag_value %s --instance_class %s --instance_disk_size %s" % \
                 (notebook_config['instance_name'], notebook_config['ami_id'], notebook_config['instance_type'],
                  notebook_config['key_name'], get_security_group_by_name(notebook_config['security_group_name']),
                  get_subnet_by_cidr(notebook_config['subnet_cidr']), notebook_config['role_profile_name'],
                  notebook_config['tag_name'], notebook_config['instance_name'], instance_class, os.environ['notebook_disk_size'])
        if not run_routine('create_instance', params):
            logging.info('Failed to create instance')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed to create instance", "conf": notebook_config}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)
    except:
        sys.exit(1)

    # generating variables regarding EDGE proxy on Notebook instance
    instance_hostname = get_instance_hostname(notebook_config['instance_name'])
    edge_instance_name = os.environ['conf_service_base_name'] + "-" + os.environ['notebook_user_name'] + '-edge'
    edge_instance_hostname = get_instance_hostname(edge_instance_name)
    keyfile_name = "/root/keys/%s.pem" % os.environ['creds_key_name']

    # configuring proxy on Notebook instance
    try:
        logging.info('[CONFIGURE PROXY ON JUPYTER INSTANCE]')
        print '[CONFIGURE PROXY ON JUPYTER INSTANCE]'
        additional_config = {"proxy_host": edge_instance_hostname, "proxy_port": "3128"}
        params = "--hostname %s --instance_name %s --keyfile %s --additional_config '%s'" % \
                 (instance_hostname, notebook_config['instance_name'], keyfile_name, json.dumps(additional_config))
        if not run_routine('configure_proxy', params):
            logging.info('Failed to configure proxy')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed to configure proxy", "conf": notebook_config}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)
    except:
        remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # updating repositories & installing python packages
    try:
        logging.info('[INSTALLING PREREQUISITES TO JUPYTER NOTEBOOK INSTANCE]')
        print('[INSTALLING PREREQUISITES TO JUPYTER NOTEBOOK INSTANCE]')
        params = "--hostname %s --keyfile %s " % (instance_hostname, keyfile_name)
        if not run_routine('install_prerequisites', params):
            logging.info('Failed installing apps: apt & pip')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed installing apps: apt & pip", "conf": notebook_config}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)
    except:
        remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # installing and configuring jupiter and all dependencies
    try:
        logging.info('[CONFIGURE JUPYTER NOTEBOOK INSTANCE]')
        print '[CONFIGURE JUPYTER NOTEBOOK INSTANCE]'
        additional_config = {"frontend_hostname": edge_instance_hostname,
                             "backend_hostname": get_instance_hostname(notebook_config['instance_name']),
                             "backend_port": "8888",
                             "nginx_template_dir": "/root/templates/"}
        params = "--hostname %s --instance_name %s --keyfile %s --region %s --additional_config '%s'" % \
                 (instance_hostname, notebook_config['instance_name'], keyfile_name, os.environ['creds_region'], json.dumps(additional_config))
        if not run_routine('configure_jupyter_node', params):
            logging.info('Failed to configure jupiter')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed to configure jupyter", "conf": notebook_config}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)
    except:
        remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # installing python2 and python3 libs
    try:
        logging.info('[CONFIGURE JUPYTER ADDITIONS]')
        print '[CONFIGURE JUPYTER ADDITIONS]'
        params = "--hostname %s --keyfile %s" % (instance_hostname, keyfile_name)
        if not run_routine('install_jupyter_additions', params):
            logging.info('Failed to install python libs')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed to install python libs", "conf": notebook_config}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)
    except:
        remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    try:
        print '[INSTALLING USERs KEY]'
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": notebook_config['user_keyname'],
                             "user_keydir": "/root/keys/"}
        params = "--hostname {} --keyfile {} --additional_config '{}'".format(
            instance_hostname, keyfile_name, json.dumps(additional_config))
        if not run_routine('install_user_key', params):
            logging.info('Failed installing user key')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed installing users key", "conf": params}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)
    except:
        sys.exit(1)

    # checking the need for image creation
    ami_id = get_ami_id_by_name(notebook_config['expected_ami_name'])
    if ami_id == '':
        print "Looks like it's first time we configure notebook server. Creating image."
        image_id = create_image_from_instance(instance_name=notebook_config['instance_name'],
                                              image_name=notebook_config['expected_ami_name'])
        if image_id != '':
            print "Image was successfully created. It's ID is " + image_id

    # generating output information
    ip_address = get_instance_ip_address(notebook_config['instance_name']).get('Private')
    dns_name = get_instance_hostname(notebook_config['instance_name'])
    jupyter_ip_url = "http://" + ip_address + ":8888/" + notebook_config['instance_name'] + "/"
    jupyter_dns_url = "http://" + dns_name + ":8888/" + notebook_config['instance_name'] + "/"
    print '[SUMMARY]'
    logging.info('[SUMMARY]')
    print "Instance name: " + notebook_config['instance_name']
    print "Private DNS: " + dns_name
    print "Private IP: " + ip_address
    print "Instance type: " + notebook_config['instance_type']
    print "Key name: " + notebook_config['key_name']
    print "User key name: " + notebook_config['user_keyname']
    print "AMI name: " + notebook_config['expected_ami_name']
    print "Profile name: " + notebook_config['role_profile_name']
    print "SG name: " + notebook_config['security_group_name']
    print "Jupyter URL: " + jupyter_ip_url
    print "Jupyter URL: " + jupyter_dns_url
    print 'SSH access (from Edge node, via IP address): ssh -i ' + notebook_config[
        'key_name'] + '.pem ubuntu@' + ip_address
    print 'SSH access (from Edge node, via FQDN): ssh -i ' + notebook_config['key_name'] + '.pem ubuntu@' + dns_name

    with open("/root/result.json", 'w') as result:
        res = {"hostname": dns_name,
               "ip": ip_address,
               "master_keyname": os.environ['creds_key_name'],
               "notebook_name": notebook_config['instance_name'],
               "Action": "Create new notebook server",
               "exploratory_url": jupyter_ip_url}
        result.write(json.dumps(res))


# Main function for terminating exploratory environment
def terminate():
    local_log_filename = "{}_{}_{}.log".format(os.environ['resource'], os.environ['notebook_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['resource'] +  "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    create_aws_config_files()
    print 'Generating infrastructure names and tags'
    notebook_config = dict()
    notebook_config['service_base_name'] = os.environ['conf_service_base_name']
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['bucket_name'] = (notebook_config['service_base_name'] + '-ssn-bucket').lower().replace('_', '-')
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-Tag'

    try:
        logging.info('[TERMINATE NOTEBOOK]')
        print '[TERMINATE NOTEBOOK]'
        params = "--bucket_name %s --tag_name %s --nb_tag_value %s" % \
                 (notebook_config['bucket_name'], notebook_config['tag_name'], notebook_config['notebook_name'])
        if not run_routine('terminate_notebook', params):
            logging.info('Failed to terminate notebook')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed to terminate notebook", "conf": notebook_config}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Tag_name": notebook_config['tag_name'],
                   "user_own_bucket_name": notebook_config['bucket_name'],
                   "Action": "Terminate notebook server"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)


# Main function for stopping notebook server
def stop():
    local_log_filename = "{}_{}_{}.log".format(os.environ['resource'], os.environ['notebook_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['resource'] +  "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    create_aws_config_files()
    print 'Generating infrastructure names and tags'
    notebook_config = dict()
    notebook_config['service_base_name'] = os.environ['conf_service_base_name']
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['bucket_name'] = (notebook_config['service_base_name'] + '-ssn-bucket').lower().replace('_', '-')
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-Tag'
    notebook_config['ssh_user'] = os.environ['notebook_ssh_user']
    notebook_config['key_path'] = os.environ['creds_key_dir'] + '/' + os.environ['creds_key_name'] + '.pem'

    try:
        logging.info('[STOP NOTEBOOK]')
        print '[STOP NOTEBOOK]'
        params = "--bucket_name %s --tag_name %s --nb_tag_value %s --ssh_user %s --key_path %s" % \
                 (notebook_config['bucket_name'], notebook_config['tag_name'], notebook_config['notebook_name'], notebook_config['ssh_user'], notebook_config['key_path'])
        if not run_routine('stop_notebook', params):
            logging.info('Failed to stop notebook')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed to stop notebook", "conf": notebook_config}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Tag_name": notebook_config['tag_name'],
                   "user_own_bucket_name": notebook_config['bucket_name'],
                   "Action": "Stop notebook server"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)


# Main function for starting notebook server
def start():
    local_log_filename = "{}_{}_{}.log".format(os.environ['resource'], os.environ['notebook_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['resource'] +  "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    create_aws_config_files()
    print 'Generating infrastructure names and tags'
    notebook_config = dict()
    notebook_config['service_base_name'] = os.environ['conf_service_base_name']
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-Tag'

    try:
        logging.info('[START NOTEBOOK]')
        print '[START NOTEBOOK]'
        params = "--tag_name %s --nb_tag_value %s" % \
                 (notebook_config['tag_name'], notebook_config['notebook_name'])
        if not run_routine('start_notebook', params):
            logging.info('Failed to start notebook')
            with open("/root/result.json", 'w') as result:
                res = {"error": "Failed to start notebook", "conf": notebook_config}
                print json.dumps(res)
                result.write(json.dumps(res))
            sys.exit(1)
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Tag_name": notebook_config['tag_name'],
                   "Action": "Start up notebook server"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)