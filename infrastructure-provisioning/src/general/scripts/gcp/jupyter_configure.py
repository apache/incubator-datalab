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

parser = argparse.ArgumentParser()
parser.add_argument('--uuid', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
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
    notebook_config['instance_type'] = os.environ['gcp_notebook_instance_type']
    notebook_config['key_name'] = os.environ['conf_key_name']
    notebook_config['user_keyname'] = os.environ['edge_user_name']
    notebook_config['instance_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
        'edge_user_name'] + "-nb-" + notebook_config['exploratory_name'] + "-" + args.uuid
    instance_hostname = GCPMeta().get_instance_public_ip_by_name(notebook_config['instance_name'])
    edge_instance_name = os.environ['conf_service_base_name'] + "-" + os.environ['edge_user_name'] + '-edge'
    edge_instance_hostname = GCPMeta().get_instance_public_ip_by_name(edge_instance_name)
    keyfile_name = "/root/keys/{}.pem".format(os.environ['conf_key_name'])
    notebook_config['dlab_ssh_user'] = os.environ['conf_os_user']

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
        GCPActions().remove_instance(notebook_config['instance_name'])
        sys.exit(1)

    # configuring proxy on Notebook instance
    try:
        logging.info('[CONFIGURE PROXY ON JUPYTER INSTANCE]')
        print '[CONFIGURE PROXY ON JUPYTER INSTANCE]'
        additional_config = {"proxy_host": edge_instance_hostname, "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}"\
            .format(instance_hostname, notebook_config['instance_name'], keyfile_name, json.dumps(additional_config),
                    notebook_config['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('notebook_configure_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to configure proxy.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'])
        sys.exit(1)

    # updating repositories & installing python packages
    try:
        logging.info('[INSTALLING PREREQUISITES TO JUPYTER NOTEBOOK INSTANCE]')
        print('[INSTALLING PREREQUISITES TO JUPYTER NOTEBOOK INSTANCE]')
        params = "--hostname {} --keyfile {} --user {} --region {}".\
            format(instance_hostname, keyfile_name, notebook_config['dlab_ssh_user'], os.environ['gcp_region'])
        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing apps: apt & pip.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'])
        sys.exit(1)

    # installing and configuring jupiter and all dependencies
    try:
        logging.info('[CONFIGURE JUPYTER NOTEBOOK INSTANCE]')
        print '[CONFIGURE JUPYTER NOTEBOOK INSTANCE]'
        params = "--hostname {} --keyfile {} --region {} --spark_version {} --hadoop_version {} --os_user {} --scala_version {} --r_mirror {}".\
            format(instance_hostname, keyfile_name, os.environ['gcp_region'], os.environ['notebook_spark_version'],
                   os.environ['notebook_hadoop_version'], notebook_config['dlab_ssh_user'],
                   os.environ['notebook_scala_version'], os.environ['notebook_r_mirror'])
        try:
            local("~/scripts/{}.py {}".format('configure_jupyter_node', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to configure jupyter.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'])
        sys.exit(1)

    # installing python2 and python3 libs
    try:
        logging.info('[CONFIGURE JUPYTER ADDITIONS]')
        print '[CONFIGURE JUPYTER ADDITIONS]'
        params = "--hostname {} --keyfile {} --os_user {}"\
            .format(instance_hostname, keyfile_name, notebook_config['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_jupyter_additions', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to install python libs.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'])
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
            append_result("Failed installing users key")
            raise Exception
    except Exception as err:
        append_result("Failed installing users key.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'])
        sys.exit(1)

    try:
        print '[SETUP USER GIT CREDENTIALS]'
        logging.info('[SETUP USER GIT CREDENTIALS]')
        params = '--os_user {} --notebook_ip {} --keyfile "{}"' \
            .format(notebook_config['dlab_ssh_user'], instance_hostname, keyfile_name)
        try:
            local("~/scripts/{}.py {}".format('manage_git_creds', params))
        except:
            append_result("Failed setup git credentials")
            raise Exception
    except Exception as err:
        append_result("Failed to setup git credentials.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'])
        sys.exit(1)

    # try:
    #     print '[CREATING AMI]'
    #     logging.info('[CREATING AMI]')
    #     ami_id = get_ami_id_by_name(notebook_config['expected_ami_name'])
    #     if ami_id == '':
    #         print "Looks like it's first time we configure notebook server. Creating image."
    #         image_id = create_image_from_instance(tag_name=notebook_config['tag_name'],
    #                                               instance_name=notebook_config['instance_name'],
    #                                               image_name=notebook_config['expected_ami_name'])
    #         if image_id != '':
    #             print "Image was successfully created. It's ID is " + image_id
    # except Exception as err:
    #     append_result("Failed installing users key.", str(err))
    #     GCPActions().remove_instance(notebook_config['instance_name'])
    #     sys.exit(1)

    # generating output information
    ip_address = GCPMeta().get_private_ip_address(notebook_config['instance_name'])
    jupyter_ip_url = "http://" + ip_address + ":8888/"
    ungit_ip_url = "http://" + ip_address + ":8085/"
    print '[SUMMARY]'
    logging.info('[SUMMARY]')
    print "Instance name: " + notebook_config['instance_name']
    print "Private IP: " + ip_address
    print "Instance type: " + notebook_config['instance_type']
    print "Key name: " + notebook_config['key_name']
    print "User key name: " + notebook_config['user_keyname']
    # print "AMI name: " + notebook_config['expected_ami_name']
    # print "Profile name: " + notebook_config['role_profile_name']
    # print "SG name: " + notebook_config['security_group_name']
    print "Jupyter URL: " + jupyter_ip_url
    print "Ungit URL: " + ungit_ip_url
    print 'SSH access (from Edge node, via IP address): ssh -i ' + notebook_config[
        'key_name'] + '.pem ' + notebook_config['dlab_ssh_user'] + '@' + ip_address

    with open("/root/result.json", 'w') as result:
        res = {"hostname": ip_address,
               "ip": ip_address,
               "master_keyname": os.environ['conf_key_name'],
               "notebook_name": notebook_config['instance_name'],
               "Action": "Create new notebook server",
               "exploratory_url": [
                   {"description": "Jupyter",
                    "url": jupyter_ip_url},
                   {"description": "Ungit",
                    "url": ungit_ip_url}]}
        result.write(json.dumps(res))