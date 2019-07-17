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
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    notebook_config = dict()
    try:
        notebook_config['exploratory_name'] = (os.environ['exploratory_name']).lower().replace('_', '-')
    except:
        notebook_config['exploratory_name'] = ''
    notebook_config['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    notebook_config['instance_type'] = os.environ['gcp_notebook_instance_size']
    notebook_config['key_name'] = os.environ['conf_key_name']
    notebook_config['edge_user_name'] = (os.environ['edge_user_name']).lower().replace('_', '-')
    notebook_config['project_name'] = (os.environ['project_name']).lower().replace('_', '-')
    notebook_config['instance_name'] = '{0}-{1}-nb-{2}'.format(notebook_config['service_base_name'],
                                                               notebook_config['project_name'],
                                                               notebook_config['exploratory_name'])
    notebook_config['expected_primary_image_name'] = '{}-{}-notebook-primary-image'.format(
                                                        notebook_config['service_base_name'], os.environ['application'])
    notebook_config['expected_secondary_image_name'] = '{}-{}-notebook-secondary-image'.format(
                                                        notebook_config['service_base_name'], os.environ['application'])

    # generating variables regarding EDGE proxy on Notebook instance
    instance_hostname = GCPMeta().get_private_ip_address(notebook_config['instance_name'])
    edge_instance_name = '{0}-{1}-edge'.format(notebook_config['service_base_name'], notebook_config['project_name'])
    edge_instance_hostname = GCPMeta().get_instance_public_ip_by_name(edge_instance_name)
    edge_instance_private_ip = GCPMeta().get_private_ip_address(edge_instance_name)
    notebook_config['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    notebook_config['dlab_ssh_user'] = os.environ['conf_os_user']
    notebook_config['zone'] = os.environ['gcp_zone']
    notebook_config['rstudio_pass'] = id_generator()
    notebook_config['shared_image_enabled'] = os.environ['conf_shared_image_enabled']
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
            (instance_hostname, os.environ['conf_key_dir'] + os.environ['conf_key_name'] + ".pem", initial_user,
             notebook_config['dlab_ssh_user'], sudo_group)

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed creating ssh user 'dlab'.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    # configuring proxy on Notebook instance
    try:
        logging.info('[CONFIGURE PROXY ON TENSORFLOW-RSTUDIO INSTANCE]')
        print('[CONFIGURE PROXY ON TENSORFLOW-RSTUDIO INSTANCE]')
        additional_config = {"proxy_host": edge_instance_name, "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}" \
            .format(instance_hostname, notebook_config['instance_name'], notebook_config['ssh_key_path'],
                    json.dumps(additional_config), notebook_config['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('common_configure_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to configure proxy.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    # updating repositories & installing python packages
    try:
        logging.info('[INSTALLING PREREQUISITES TO TENSORFLOW-RSTUDIO NOTEBOOK INSTANCE]')
        print('[INSTALLING PREREQUISITES TO TENSORFLOW-RSTUDIO NOTEBOOK INSTANCE]')
        params = "--hostname {} --keyfile {} --user {} --region {} --edge_private_ip {}". \
            format(instance_hostname, notebook_config['ssh_key_path'], notebook_config['dlab_ssh_user'],
                   os.environ['gcp_region'], edge_instance_private_ip)
        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing apps: apt & pip.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    # installing and configuring TensorFlow and RSTUDIO and all dependencies
    try:
        logging.info('[CONFIGURE TENSORFLOW-RSTUDIO NOTEBOOK INSTANCE]')
        print('[CONFIGURE TENSORFLOW-RSTUDIO NOTEBOOK INSTANCE]')
        params = "--hostname {}  --keyfile {} " \
                 "--region {} --rstudio_pass {} " \
                 "--rstudio_version {} --os_user {} " \
                 "--r_mirror {} --exploratory_name {}" \
            .format(instance_hostname, notebook_config['ssh_key_path'],
                    os.environ['gcp_region'], notebook_config['rstudio_pass'],
                    os.environ['notebook_rstudio_version'], notebook_config['dlab_ssh_user'],
                    os.environ['notebook_r_mirror'], notebook_config['exploratory_name'])
        try:
            local("~/scripts/{}.py {}".format('configure_tensor-rstudio_node', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to configure tensoflow-rstudio.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    try:
        print('[INSTALLING USERs KEY]')
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": os.environ['project_name'],
                             "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            instance_hostname, notebook_config['ssh_key_path'], json.dumps(additional_config), notebook_config['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_user_key', params))
        except:
            append_result("Failed installing users key")
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing users key.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    try:
        print('[SETUP USER GIT CREDENTIALS]')
        logging.info('[SETUP USER GIT CREDENTIALS]')
        params = '--os_user {} --notebook_ip {} --keyfile "{}"' \
            .format(notebook_config['dlab_ssh_user'], instance_hostname, notebook_config['ssh_key_path'])
        try:
            local("~/scripts/{}.py {}".format('common_download_git_certfile', params))
            local("~/scripts/{}.py {}".format('manage_git_creds', params))
        except:
            append_result("Failed setup git credentials")
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to setup git credentials.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    if notebook_config['shared_image_enabled'] == 'true':
        try:
            print('[CREATING IMAGE]')
            primary_image_id = GCPMeta().get_image_by_name(notebook_config['expected_primary_image_name'])
            if primary_image_id == '':
                print("Looks like it's first time we configure notebook server. Creating images.")
                image_id_list = GCPActions().create_image_from_instance_disks(
                    notebook_config['expected_primary_image_name'], notebook_config['expected_secondary_image_name'],
                    notebook_config['instance_name'], notebook_config['zone'])
                if image_id_list and image_id_list[0] != '':
                    print("Image of primary disk was successfully created. It's ID is {}".format(image_id_list[0]))
                else:
                    print(
                        "Looks like another image creating operation for your template have been started a moment ago.")
                if image_id_list and image_id_list[1] != '':
                    print("Image of secondary disk was successfully created. It's ID is {}".format(image_id_list[1]))
        except Exception as err:
            print('Error: {0}'.format(err))
            append_result("Failed creating image.", str(err))
            GCPActions().remove_instance(notebook_config['instance_name'], notebook_config['zone'])
            GCPActions().remove_image(notebook_config['expected_primary_image_name'])
            GCPActions().remove_image(notebook_config['expected_secondary_image_name'])
            sys.exit(1)

    try:
        print('[SETUP EDGE REVERSE PROXY TEMPLATE]')
        logging.info('[SETUP EDGE REVERSE PROXY TEMPLATE]')
        additional_info = {
            'instance_hostname': instance_hostname,
            'tensor': False
        }
        params = "--edge_hostname {} " \
                 "--keyfile {} " \
                 "--os_user {} " \
                 "--type {} " \
                 "--exploratory_name {} " \
                 "--additional_info '{}'"\
            .format(edge_instance_hostname,
                    notebook_config['ssh_key_path'],
                    notebook_config['dlab_ssh_user'],
                    'rstudio',
                    notebook_config['exploratory_name'],
                    json.dumps(additional_info))
        try:
            local("~/scripts/{}.py {}".format('common_configure_reverse_proxy', params))
        except:
            append_result("Failed edge reverse proxy template")
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to set edge reverse proxy template.", str(err))
        GCPActions().remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    # generating output information
    ip_address = GCPMeta().get_private_ip_address(notebook_config['instance_name'])
    tensorboard_url = "http://" + ip_address + ":6006/"
    rstudio_ip_url = "http://" + ip_address + ":8787/"
    ungit_ip_url = "http://" + ip_address + ":8085/{}-ungit/".format(notebook_config['exploratory_name'])
    rstudio_notebook_acces_url = "http://" + edge_instance_hostname + "/{}/".format(notebook_config['exploratory_name'])
    tensorboard_acces_url = "http://" + edge_instance_hostname + "/{}-tensor/".format(notebook_config['exploratory_name'])
    rstudio_ungit_acces_url = "http://" + edge_instance_hostname + "/{}-ungit/".format(
        notebook_config['exploratory_name'])
    print('[SUMMARY]')
    logging.info('[SUMMARY]')
    print("Instance name: {}".format(notebook_config['instance_name']))
    print("Private IP: {}".format(ip_address))
    print("Instance type: {}".format(notebook_config['instance_type']))
    print("Key name: {}".format(notebook_config['key_name']))
    print("User key name: {}".format(os.environ['project_name']))
    print("TensorBoard URL: {}".format(tensorboard_url))
    print("TensorBoard log dir: /var/log/tensorboard")
    print("Rstudio URL: {}".format(rstudio_ip_url))
    print("Rstudio user: {}".format(notebook_config['dlab_ssh_user']))
    print("Rstudio pass: {}".format(notebook_config['rstudio_pass']))
    print("Ungit URL: {}".format(ungit_ip_url))
    print('SSH access (from Edge node, via IP address): ssh -i {0}.pem {1}@{2}'.format(notebook_config['key_name'],
                                                                                       notebook_config['dlab_ssh_user'],
                                                                                       ip_address))

    with open("/root/result.json", 'w') as result:
        res = {"hostname": ip_address,
               "ip": ip_address,
               "instance_id": notebook_config['instance_name'],
               "master_keyname": os.environ['conf_key_name'],
               "tensorboard_log_dir": "/var/log/tensorboard",
               "notebook_name": notebook_config['instance_name'],
               "Action": "Create new notebook server",
               "exploratory_url": [
                   {"description": "Rstudio",
                    "url": rstudio_notebook_acces_url},
                   {"description": "TensorBoard",
                    "url": tensorboard_acces_url},
                   {"description": "Ungit",
                    "url": rstudio_ungit_acces_url},
                   {"description": "Rstudio (via tunnel)",
                    "url": rstudio_ip_url},
                   {"description": "TensorBoard (via tunnel)",
                    "url": tensorboard_url},
                   {"description": "Ungit (via tunnel)",
                    "url": ungit_ip_url}
               ],
               "exploratory_user": notebook_config['dlab_ssh_user'],
               "exploratory_pass": notebook_config['rstudio_pass']}
        result.write(json.dumps(res))