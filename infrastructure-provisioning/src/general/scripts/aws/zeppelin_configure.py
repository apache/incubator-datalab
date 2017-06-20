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
from dlab.meta_lib import *
from dlab.actions_lib import *
import os
import argparse
import traceback

parser = argparse.ArgumentParser()
parser.add_argument('--uuid', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    notebook_config = dict()
    try:
        notebook_config['exploratory_name'] = os.environ['exploratory_name']
    except:
        notebook_config['exploratory_name'] = ''
    notebook_config['service_base_name'] = os.environ['conf_service_base_name']
    notebook_config['instance_type'] = os.environ['aws_notebook_instance_type']
    notebook_config['key_name'] = os.environ['conf_key_name']
    notebook_config['user_keyname'] = os.environ['edge_user_name']
    notebook_config['instance_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
        'edge_user_name'] + "-nb-" + notebook_config['exploratory_name'] + "-" + args.uuid
    if os.environ['application'] == 'zeppelin':
        if os.environ['notebook_multiple_emrs'] == 'true':
            notebook_config['expected_ami_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
                'edge_user_name'] + '-' + os.environ['application'] + '-livy-notebook-image'
        else:
            notebook_config['expected_ami_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
                'edge_user_name'] + '-' + os.environ['application'] + '-spark-notebook-image'
    else:
        notebook_config['expected_ami_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
            'edge_user_name'] + '-' + os.environ['application'] + '-notebook-image'
    notebook_config['role_profile_name'] = os.environ['conf_service_base_name'].lower().replace('-', '_') + "-" + \
                                           os.environ['edge_user_name'] + "-nb-Profile"
    notebook_config['security_group_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
        'edge_user_name'] + "-nb-SG"
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-Tag'
    notebook_config['dlab_ssh_user'] = os.environ['conf_os_user']

    # generating variables regarding EDGE proxy on Notebook instance
    instance_hostname = get_instance_hostname(notebook_config['tag_name'], notebook_config['instance_name'])
    edge_instance_name = os.environ['conf_service_base_name'] + "-" + os.environ['edge_user_name'] + '-edge'
    edge_instance_hostname = get_instance_hostname(notebook_config['tag_name'], edge_instance_name)
    keyfile_name = "/root/keys/{}.pem".format(os.environ['conf_key_name'])

    try:
        if os.environ['conf_os_family'] == 'debian':
            initial_user = 'ubuntu'
            sudo_group = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            initial_user = 'ec2-user'
            sudo_group = 'wheel'

        logging.info('[CREATING DLAB SSH USER]')
        print('[CREATING DLAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format\
            (instance_hostname, "/root/keys/" + os.environ['conf_key_name'] + ".pem", initial_user,
             notebook_config['dlab_ssh_user'], sudo_group)

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed creating ssh user 'dlab'.", str(err))
        remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # configuring proxy on Notebook instance
    try:
        logging.info('[CONFIGURE PROXY ON ZEPPELIN INSTANCE]')
        print '[CONFIGURE PROXY ON ZEPPELIN INSTANCE]'
        additional_config = {"proxy_host": edge_instance_hostname, "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}" \
            .format(instance_hostname, notebook_config['instance_name'], keyfile_name, json.dumps(additional_config),
                    notebook_config['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('notebook_configure_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result('Unable to configure proxy on zeppelin notebook. Exception: ' + str(err))
        remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # updating repositories & installing python packages
    try:
        logging.info('[INSTALLING PREREQUISITES TO ZEPPELIN NOTEBOOK INSTANCE]')
        print('[INSTALLING PREREQUISITES TO ZEPPELIN NOTEBOOK INSTANCE]')
        params = "--hostname {} --keyfile {} --user {} --region {}" \
            .format(instance_hostname, keyfile_name, notebook_config['dlab_ssh_user'], os.environ['aws_region'])
        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing apps: apt & pip.", str(err))
        remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # installing and configuring zeppelin and all dependencies
    try:
        logging.info('[CONFIGURE ZEPPELIN NOTEBOOK INSTANCE]')
        print '[CONFIGURE ZEPPELIN NOTEBOOK INSTANCE]'
        additional_config = {"frontend_hostname": edge_instance_hostname,
                             "backend_hostname": get_instance_hostname(notebook_config['tag_name'], notebook_config['instance_name']),
                             "backend_port": "8080",
                             "nginx_template_dir": "/root/templates/"}
        params = "--hostname {} --instance_name {} --keyfile {} --region {} --additional_config '{}' --os_user {} --spark_version {} --hadoop_version {} --edge_hostname {} --proxy_port {} --zeppelin_version {} --scala_version {} --livy_version {} --multiple_emrs {} --r_mirror {}" \
            .format(instance_hostname, notebook_config['instance_name'], keyfile_name, os.environ['aws_region'],
                    json.dumps(additional_config), notebook_config['dlab_ssh_user'], os.environ['notebook_spark_version'],
                    os.environ['notebook_hadoop_version'], edge_instance_hostname, '3128',
                    os.environ['notebook_zeppelin_version'], os.environ['notebook_scala_version'],
                    os.environ['notebook_livy_version'], os.environ['notebook_multiple_emrs'],
                    os.environ['notebook_r_mirror'])
        try:
            local("~/scripts/{}.py {}".format('configure_zeppelin_node', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to configure zeppelin.", str(err))
        remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # installing python2 and python3 libs
    try:
        logging.info('[CONFIGURE ZEPPELIN ADDITIONS]')
        print '[CONFIGURE ZEPPELIN ADDITIONS]'
        params = "--hostname {} --keyfile {} --os_user {}" \
            .format(instance_hostname, keyfile_name, notebook_config['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_zeppelin_additions', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to install python libs.", str(err))
        remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    try:
        print '[INSTALLING USERs KEY]'
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": notebook_config['user_keyname'],
                             "user_keydir": "/root/keys/"}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            instance_hostname, keyfile_name, json.dumps(additional_config), notebook_config['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_user_key', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing users key.", str(err))
        remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    try:
        print '[CREATING AMI]'
        logging.info('[CREATING AMI]')
        ami_id = get_ami_id_by_name(notebook_config['expected_ami_name'])
        if ami_id == '':
            print "Looks like it's first time we configure notebook server. Creating image."
            image_id = create_image_from_instance(tag_name=notebook_config['tag_name'],
                                                  instance_name=notebook_config['instance_name'],
                                                  image_name=notebook_config['expected_ami_name'])
            if image_id != '':
                print "Image was successfully created. It's ID is " + image_id
    except Exception as err:
        append_result("Failed installing users key.", str(err))
        remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # generating output information
    ip_address = get_instance_ip_address(notebook_config['tag_name'], notebook_config['instance_name']).get('Private')
    dns_name = get_instance_hostname(notebook_config['tag_name'], notebook_config['instance_name'])
    zeppelin_ip_url = "http://" + ip_address + ":8080/"
    zeppelin_dns_url = "http://" + dns_name + ":8080/"
    gitweb_ip_url = "http://" + ip_address + ":8085/"
    print '[SUMMARY]'
    logging.info('[SUMMARY]')
    print "Instance name: " + notebook_config['instance_name']
    print "Private DNS: " + dns_name
    print "Private IP: " + ip_address
    print "Instance ID: " + get_instance_by_name(notebook_config['tag_name'], notebook_config['instance_name'])
    print "Instance type: " + notebook_config['instance_type']
    print "Key name: " + notebook_config['key_name']
    print "User key name: " + notebook_config['user_keyname']
    print "AMI name: " + notebook_config['expected_ami_name']
    print "Profile name: " + notebook_config['role_profile_name']
    print "SG name: " + notebook_config['security_group_name']
    print "Zeppelin URL: " + zeppelin_ip_url
    print "Zeppelin URL: " + zeppelin_dns_url
    print "GitWeb URL: " + gitweb_ip_url
    print 'SSH access (from Edge node, via IP address): ssh -i ' + notebook_config[
        'key_name'] + '.pem ' + notebook_config['dlab_ssh_user'] + '@' + ip_address
    print 'SSH access (from Edge node, via FQDN): ssh -i ' + notebook_config['key_name'] + '.pem ' \
          + notebook_config['dlab_ssh_user'] + '@' + dns_name

    with open("/root/result.json", 'w') as result:
        res = {"hostname": dns_name,
               "ip": ip_address,
               "instance_id": get_instance_by_name(notebook_config['tag_name'], notebook_config['instance_name']),
               "master_keyname": os.environ['conf_key_name'],
               "notebook_name": notebook_config['instance_name'],
               "Action": "Create new notebook server",
               "exploratory_url": [
                   {"description": "Zeppelin",
                    "url": zeppelin_ip_url},
                   {"description": "GitWeb",
                    "url": gitweb_ip_url}]}
        result.write(json.dumps(res))