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

import argparse
import datalab.fab
import datalab.actions_lib
import datalab.meta_lib
import json
import os
import sys
import traceback
import subprocess
from fabric import *
from datalab.logger import logging

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
    try:
        notebook_config = dict()
        try:
            notebook_config['exploratory_name'] = os.environ['exploratory_name'].lower()
        except:
            notebook_config['exploratory_name'] = ''
        notebook_config['service_base_name'] = os.environ['conf_service_base_name']
        notebook_config['project_name'] = os.environ['project_name']
        notebook_config['endpoint_name'] = os.environ['endpoint_name']
        notebook_config['instance_type'] = os.environ['aws_notebook_instance_type']
        notebook_config['key_name'] = os.environ['conf_key_name']
        notebook_config['user_keyname'] = notebook_config['project_name']
        notebook_config['network_type'] = os.environ['conf_network_type']
        notebook_config['instance_name'] = '{}-{}-{}-nb-{}-{}'.format(notebook_config['service_base_name'],
                                                                      notebook_config['project_name'],
                                                                      notebook_config['endpoint_name'],
                                                                      notebook_config['exploratory_name'], args.uuid)
        notebook_config['image_enabled'] = os.environ['conf_image_enabled']
        notebook_config['shared_image_enabled'] = os.environ['conf_shared_image_enabled']
        if os.environ['conf_shared_image_enabled'] == 'false':
            notebook_config['expected_image_name'] = '{0}-{1}-{2}-{3}-notebook-image'.format(
                notebook_config['service_base_name'],
                notebook_config['project_name'],
                notebook_config['endpoint_name'],
                os.environ['application'])
        else:
            notebook_config['expected_image_name'] = '{0}-{1}-{2}-notebook-image'.format(
                notebook_config['service_base_name'],
                notebook_config['endpoint_name'],
                os.environ['application'])
        notebook_config['notebook_image_name'] = str(os.environ.get('notebook_image_name'))
        notebook_config['role_profile_name'] = '{}-{}-{}-nb-de-profile'.format(
            notebook_config['service_base_name'], notebook_config['project_name'], notebook_config['endpoint_name'])
        notebook_config['security_group_name'] = '{}-{}-{}-nb-sg'.format(notebook_config['service_base_name'],
                                                                         notebook_config['project_name'],
                                                                         notebook_config['endpoint_name'])
        notebook_config['tag_name'] = '{}-tag'.format(notebook_config['service_base_name'])
        notebook_config['datalab_ssh_user'] = os.environ['conf_os_user']
        notebook_config['ip_address'] = datalab.meta_lib.get_instance_ip_address(
            notebook_config['tag_name'], notebook_config['instance_name']).get('Private')

        # generating variables regarding EDGE proxy on Notebook instance
        instance_hostname = datalab.meta_lib.get_instance_hostname(notebook_config['tag_name'],
                                                                   notebook_config['instance_name'])
        edge_instance_name = '{}-{}-{}-edge'.format(notebook_config['service_base_name'],
                                                    notebook_config['project_name'], notebook_config['endpoint_name'])
        edge_instance_hostname = datalab.meta_lib.get_instance_hostname(notebook_config['tag_name'], edge_instance_name)
        edge_instance_private_ip = datalab.meta_lib.get_instance_ip_address(notebook_config['tag_name'],
                                                                            edge_instance_name).get('Private')
        notebook_config['edge_instance_hostname'] = datalab.meta_lib.get_instance_hostname(notebook_config['tag_name'],
                                                                                           edge_instance_name)
        keyfile_name = "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        edge_ip = datalab.meta_lib.get_instance_ip_address(notebook_config['tag_name'], edge_instance_name).get(
            'Private')
        notebook_config['rstudio_pass'] = datalab.fab.id_generator()
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        datalab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    try:
        if os.environ['conf_os_family'] == 'debian':
            notebook_config['initial_user'] = 'ubuntu'
            notebook_config['sudo_group'] = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            notebook_config['initial_user'] = 'ec2-user'
            notebook_config['sudo_group'] = 'wheel'

        logging.info('[CREATING DATALAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format(
            instance_hostname, "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name']),
            notebook_config['initial_user'], notebook_config['datalab_ssh_user'], notebook_config['sudo_group'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('create_ssh_user', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed creating ssh user 'datalab'.", str(err))
        datalab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # configuring proxy on Notebook instance
    try:
        logging.info('[CONFIGURE PROXY ON R_STUDIO INSTANCE]')
        additional_config = {"proxy_host": edge_instance_hostname, "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}" \
            .format(instance_hostname, notebook_config['instance_name'], keyfile_name, json.dumps(additional_config),
                    notebook_config['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_proxy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure proxy.", str(err))
        datalab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # updating repositories & installing python packages
    try:
        logging.info('[INSTALLING PREREQUISITES TO R_STUDIO NOTEBOOK INSTANCE]')
        params = "--hostname {} --keyfile {} --user {} --region {} --edge_private_ip {}". \
            format(instance_hostname, keyfile_name, notebook_config['datalab_ssh_user'], os.environ['aws_region'],
                   edge_instance_private_ip)
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_prerequisites', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed installing apps: apt & pip.", str(err))
        datalab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # installing and configuring R_STUDIO and all dependencies
    try:
        logging.info('[CONFIGURE R_STUDIO NOTEBOOK INSTANCE]')
        params = "--hostname {0}  --keyfile {1} --region {2} --rstudio_pass {3} --rstudio_version {4} " \
                 "--os_user {5} --ip_address {6} --exploratory_name {7} --edge_ip {8}" \
            .format(instance_hostname, keyfile_name,
                    os.environ['aws_region'], notebook_config['rstudio_pass'],
                    os.environ['notebook_rstudio_version'], notebook_config['datalab_ssh_user'],
                    notebook_config['ip_address'], notebook_config['exploratory_name'], edge_ip)
        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_rstudio_node', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure rstudio.", str(err))
        datalab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    try:
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": notebook_config['user_keyname'],
                             "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            instance_hostname, keyfile_name, json.dumps(additional_config), notebook_config['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_user_key', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed installing users key.", str(err))
        datalab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    try:
        logging.info('[SETUP USER GIT CREDENTIALS]')
        params = '--os_user {} --notebook_ip {} --keyfile "{}"' \
            .format(notebook_config['datalab_ssh_user'], instance_hostname, keyfile_name)
        try:
            subprocess.run("~/scripts/{}.py {}".format('manage_git_creds', params), shell=True, check=True)
        except:
            datalab.fab.append_result("Failed setup git credentials")
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to setup git credentials.", str(err))
        datalab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    try:
        logging.info('[POST CONFIGURING PROCESS]')
        if notebook_config['notebook_image_name'] not in [notebook_config['expected_image_name'], 'None', '']:
            params = "--hostname {} --keyfile {} --os_user {} --nb_tag_name {} --nb_tag_value {}" \
                .format(instance_hostname, keyfile_name, notebook_config['datalab_ssh_user'],
                        notebook_config['tag_name'], notebook_config['instance_name'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('common_remove_remote_kernels', params), shell=True, check=True)
            except:
                traceback.print_exc()
                raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to post configuring instance.", str(err))
        datalab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    try:
        logging.info('[SETUP EDGE REVERSE PROXY TEMPLATE]')
        additional_info = {
            'instance_hostname': instance_hostname,
            'tensor': False
        }
        params = "--edge_hostname {} --keyfile {} --os_user {} --type {} --exploratory_name {} --additional_info '{}'" \
            .format(edge_instance_hostname, keyfile_name, notebook_config['datalab_ssh_user'], 'rstudio',
                    notebook_config['exploratory_name'], json.dumps(additional_info))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_reverse_proxy', params), shell=True, check=True)
        except:
            datalab.fab.append_result("Failed edge reverse proxy template")
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to set edge reverse proxy template.", str(err))
        datalab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    if notebook_config['image_enabled'] == 'true':
        try:
            logging.info('[CREATING AMI]')
            ami_id = datalab.meta_lib.get_ami_id_by_name(notebook_config['expected_image_name'])
            if ami_id == '' and notebook_config['shared_image_enabled'] == 'false':
                logging.info("Looks like it's first time we configure notebook server. Creating image.")
                try:
                    os.environ['conf_additional_tags'] = '{2};project_tag:{0};endpoint_tag:{1};'.format(
                        os.environ['project_name'], os.environ['endpoint_name'], os.environ['conf_additional_tags'])
                except KeyError:
                    os.environ['conf_additional_tags'] = 'project_tag:{0};endpoint_tag:{1}'.format(
                        os.environ['project_name'], os.environ['endpoint_name'])
                image_id = datalab.actions_lib.create_image_from_instance(
                    tag_name=notebook_config['tag_name'], instance_name=notebook_config['instance_name'],
                    image_name=notebook_config['expected_image_name'])
                if image_id != '':
                    logging.info("Image was successfully created. It's ID is {}".format(image_id))
            else:
                try:
                    os.environ['conf_additional_tags'] = '{};ami:shared;endpoint_tag:{};'.format(
                        os.environ['conf_additional_tags'], os.environ['endpoint_name'])
                except KeyError:
                    os.environ['conf_additional_tags'] = 'ami:shared;endpoint_tag:{}'.format(
                        os.environ['endpoint_name'])
                image_id = datalab.actions_lib.create_image_from_instance(
                    tag_name=notebook_config['tag_name'], instance_name=notebook_config['instance_name'],
                    image_name=notebook_config['expected_image_name'])
                if image_id != '':
                    logging.info("Image was successfully created. It's ID is {}".format(image_id))
        except Exception as err:
            datalab.fab.append_result("Failed creating image.", str(err))
            datalab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
            sys.exit(1)

    try:
        # generating output information
        ip_address = datalab.meta_lib.get_instance_ip_address(notebook_config['tag_name'],
                                                              notebook_config['instance_name']).get('Private')
        dns_name = datalab.meta_lib.get_instance_hostname(notebook_config['tag_name'], notebook_config['instance_name'])
        rstudio_ip_url = "http://" + ip_address + ":8787/"
        rstudio_dns_url = "http://" + dns_name + ":8787/"
        rstudio_notebook_access_url = "https://{}/{}/".format(notebook_config['edge_instance_hostname'],
                                                              notebook_config['exploratory_name'])
        rstudio_ungit_access_url = "https://{}/{}-ungit/".format(notebook_config['edge_instance_hostname'],
                                                                 notebook_config['exploratory_name'])
        ungit_ip_url = "http://" + ip_address + ":8085/{}-ungit/".format(notebook_config['exploratory_name'])
        logging.info('[SUMMARY]')
        logging.info('[SUMMARY]')
        logging.info("Instance name: {}".format(notebook_config['instance_name']))
        logging.info("Private DNS: {}".format(dns_name))
        logging.info("Private IP: {}".format(ip_address))
        logging.info("Instance ID: {}".format(datalab.meta_lib.get_instance_by_name(notebook_config['tag_name'],
                                                                             notebook_config['instance_name'])))
        logging.info("Instance type: {}".format(notebook_config['instance_type']))
        logging.info("Key name: {}".format(notebook_config['key_name']))
        logging.info("User key name: {}".format(notebook_config['user_keyname']))
        logging.info("AMI name: {}".format(notebook_config['notebook_image_name']))
        logging.info("Profile name: {}".format(notebook_config['role_profile_name']))
        logging.info("SG name: {}".format(notebook_config['security_group_name']))
        logging.info("Rstudio URL: {}".format(rstudio_ip_url))
        logging.info("Rstudio URL: {}".format(rstudio_dns_url))
        logging.info("Rstudio user: {}".format(notebook_config['datalab_ssh_user']))
        logging.info("Rstudio pass: {}".format(notebook_config['rstudio_pass']))
        logging.info("Ungit URL: {}".format(ungit_ip_url))
        logging.info('SSH access (from Edge node, via IP address): ssh -i {0}.pem {1}@{2}'.
              format(notebook_config['key_name'], notebook_config['datalab_ssh_user'], ip_address))
        logging.info('SSH access (from Edge node, via FQDN): ssh -i {0}.pem {1}@{2}'.
              format(notebook_config['key_name'], notebook_config['datalab_ssh_user'], dns_name))

        with open("/root/result.json", 'w') as result:
            res = {"hostname": dns_name,
                   "ip": ip_address,
                   "instance_id": datalab.meta_lib.get_instance_by_name(notebook_config['tag_name'],
                                                                        notebook_config['instance_name']),
                   "master_keyname": os.environ['conf_key_name'],
                   "notebook_name": notebook_config['instance_name'],
                   "notebook_image_name": notebook_config['notebook_image_name'],
                   "Action": "Create new notebook server",
                   "exploratory_url": [
                       {"description": "RStudio",
                        "url": rstudio_notebook_access_url},
                       {"description": "Ungit",
                        "url": rstudio_ungit_access_url}  # ,
                       # {"description": "RStudio (via tunnel)",
                       # "url": rstudio_ip_url},
                       # {"description": "Ungit (via tunnel)",
                       # "url": ungit_ip_url}
                   ],
                   "exploratory_user": notebook_config['datalab_ssh_user'],
                   "exploratory_pass": notebook_config['rstudio_pass']}
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results.", str(err))
        datalab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)
