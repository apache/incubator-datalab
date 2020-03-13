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
import traceback
import dlab.fab
import dlab.actions_lib
import dlab.meta_lib
import os
import argparse
import traceback
from fabric.api import *

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
            notebook_config['exploratory_name'] = os.environ['exploratory_name']
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
        notebook_config['dlab_ssh_user'] = os.environ['conf_os_user']
        notebook_config['ip_address'] = dlab.meta_lib.get_instance_ip_address(
            notebook_config['tag_name'], notebook_config['instance_name']).get('Private')
        notebook_config['region'] = os.environ['aws_region']
        if notebook_config['region'] == 'us-east-1':
            notebook_config['endpoint_url'] = 'https://s3.amazonaws.com'
        elif notebook_config['region'] == 'cn-north-1':
            notebook_config['endpoint_url'] = "https://s3.{}.amazonaws.com.cn".format(notebook_config['region'])
        else:
            notebook_config['endpoint_url'] = 'https://s3-{}.amazonaws.com'.format(notebook_config['region'])
        # generating variables regarding EDGE proxy on Notebook instance
        instance_hostname = dlab.meta_lib.get_instance_hostname(notebook_config['tag_name'],
                                                                notebook_config['instance_name'])
        edge_instance_name = '{}-{}-{}-edge'.format(notebook_config['service_base_name'],
                                                    notebook_config['project_name'], notebook_config['endpoint_name'])
        edge_instance_hostname = dlab.meta_lib.get_instance_hostname(notebook_config['tag_name'], edge_instance_name)
        edge_instance_private_ip = dlab.meta_lib.get_instance_ip_address(notebook_config['tag_name'],
                                                                         edge_instance_name).get('Private')
        notebook_config['edge_instance_hostname'] = dlab.meta_lib.get_instance_hostname(notebook_config['tag_name'],
                                                                                        edge_instance_name)
        keyfile_name = "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        edge_ip = dlab.meta_lib.get_instance_ip_address(notebook_config['tag_name'], edge_instance_name).get('Private')
    except Exception as err:
        dlab.fab.append_result("Failed to generate variables dictionary.", str(err))
        dlab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    try:
        if os.environ['conf_os_family'] == 'debian':
            notebook_config['initial_user'] = 'ubuntu'
            notebook_config['sudo_group'] = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            notebook_config['initial_user'] = 'ec2-user'
            notebook_config['sudo_group'] = 'wheel'

        logging.info('[CREATING DLAB SSH USER]')
        print('[CREATING DLAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format(
            instance_hostname, "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name']),
            notebook_config['initial_user'], notebook_config['dlab_ssh_user'], notebook_config['sudo_group'])

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed creating ssh user 'dlab'.", str(err))
        dlab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # configuring proxy on Notebook instance
    try:
        logging.info('[CONFIGURE PROXY ON ZEPPELIN INSTANCE]')
        print('[CONFIGURE PROXY ON ZEPPELIN INSTANCE]')
        additional_config = {"proxy_host": edge_instance_hostname, "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}" \
            .format(instance_hostname, notebook_config['instance_name'], keyfile_name, json.dumps(additional_config),
                    notebook_config['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('common_configure_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result('Unable to configure proxy on zeppelin notebook. Exception: ' + str(err))
        dlab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # updating repositories & installing python packages
    try:
        logging.info('[INSTALLING PREREQUISITES TO ZEPPELIN NOTEBOOK INSTANCE]')
        print('[INSTALLING PREREQUISITES TO ZEPPELIN NOTEBOOK INSTANCE]')
        params = "--hostname {} --keyfile {} --user {} --region {} --edge_private_ip {}" \
            .format(instance_hostname, keyfile_name, notebook_config['dlab_ssh_user'], os.environ['aws_region'],
                    edge_instance_private_ip)
        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        dlab.fab.append_result("Failed installing apps: apt & pip.", str(err))
        dlab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    # installing and configuring zeppelin and all dependencies
    try:
        logging.info('[CONFIGURE ZEPPELIN NOTEBOOK INSTANCE]')
        print('[CONFIGURE ZEPPELIN NOTEBOOK INSTANCE]')
        additional_config = {"frontend_hostname": edge_instance_hostname,
                             "backend_hostname": dlab.meta_lib.get_instance_hostname(notebook_config['tag_name'],
                                                                                     notebook_config['instance_name']),
                             "backend_port": "8080",
                             "nginx_template_dir": "/root/templates/"}
        params = "--hostname {0} --instance_name {1} " \
                 "--keyfile {2} --region {3} " \
                 "--additional_config '{4}' --os_user {5} " \
                 "--spark_version {6} --hadoop_version {7} " \
                 "--edge_hostname {8} --proxy_port {9} " \
                 "--zeppelin_version {10} --scala_version {11} " \
                 "--livy_version {12} --multiple_clusters {13} " \
                 "--r_mirror {14} --endpoint_url {15} " \
                 "--ip_address {16} --exploratory_name {17} --edge_ip {18}" \
            .format(instance_hostname, notebook_config['instance_name'], keyfile_name, os.environ['aws_region'],
                    json.dumps(additional_config), notebook_config['dlab_ssh_user'], os.environ['notebook_spark_version'],
                    os.environ['notebook_hadoop_version'], edge_instance_hostname, '3128',
                    os.environ['notebook_zeppelin_version'], os.environ['notebook_scala_version'],
                    os.environ['notebook_livy_version'], os.environ['notebook_multiple_clusters'],
                    os.environ['notebook_r_mirror'], notebook_config['endpoint_url'], notebook_config['ip_address'],
                    notebook_config['exploratory_name'], edge_ip)
        try:
            local("~/scripts/{}.py {}".format('configure_zeppelin_node', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed to configure zeppelin.", str(err))
        dlab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    try:
        print('[INSTALLING USERs KEY]')
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": notebook_config['user_keyname'],
                             "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            instance_hostname, keyfile_name, json.dumps(additional_config), notebook_config['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_user_key', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed installing users key.", str(err))
        dlab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    try:
        print('[SETUP USER GIT CREDENTIALS]')
        logging.info('[SETUP USER GIT CREDENTIALS]')
        params = '--os_user {} --notebook_ip {} --keyfile "{}"' \
            .format(notebook_config['dlab_ssh_user'], instance_hostname, keyfile_name)
        try:
            local("~/scripts/{}.py {}".format('manage_git_creds', params))
        except:
            dlab.fab.append_result("Failed setup git credentials")
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed to setup git credentials.", str(err))
        dlab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)
    
    try:
        logging.info('[POST CONFIGURING PROCESS]')
        print('[POST CONFIGURING PROCESS')
        if notebook_config['notebook_image_name'] not in [notebook_config['expected_image_name'], 'None']:
            params = "--hostname {} --keyfile {} --os_user {} --nb_tag_name {} --nb_tag_value {}" \
                .format(instance_hostname, keyfile_name, notebook_config['dlab_ssh_user'],
                        notebook_config['tag_name'], notebook_config['instance_name'])
            try:
                local("~/scripts/{}.py {}".format('common_remove_remote_kernels', params))
            except:
                traceback.print_exc()
                raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed to post configuring instance.", str(err))
        dlab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    try:
        print('[SETUP EDGE REVERSE PROXY TEMPLATE]')
        logging.info('[SETUP EDGE REVERSE PROXY TEMPLATE]')
        additional_info = {
            'instance_hostname': instance_hostname,
            'tensor': False
        }
        params = "--edge_hostname {} --keyfile {} --os_user {} --type {} --exploratory_name {} --additional_info '{}'"\
            .format(edge_instance_hostname, keyfile_name, notebook_config['dlab_ssh_user'], 'zeppelin',
                    notebook_config['exploratory_name'], json.dumps(additional_info))
        try:
            local("~/scripts/{}.py {}".format('common_configure_reverse_proxy', params))
        except:
            dlab.fab.append_result("Failed edge reverse proxy template")
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed to set edge reverse proxy template.", str(err))
        dlab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)

    if notebook_config['image_enabled'] == 'true':
        try:
            print('[CREATING AMI]')
            ami_id = dlab.meta_lib.get_ami_id_by_name(notebook_config['expected_image_name'])
            if ami_id == '' and notebook_config['shared_image_enabled'] == 'false':
                print("Looks like it's first time we configure notebook server. Creating image.")
                try:
                    os.environ['conf_additional_tags'] = '{2};project_tag:{0};endpoint_tag:{1};'.format(
                        os.environ['project_name'], os.environ['endpoint_name'], os.environ['conf_additional_tags'])
                except KeyError:
                    os.environ['conf_additional_tags'] = 'project_tag:{0};endpoint_tag:{1}'.format(
                        os.environ['project_name'], os.environ['endpoint_name'])
                image_id = dlab.actions_lib.create_image_from_instance(
                    tag_name=notebook_config['tag_name'], instance_name=notebook_config['instance_name'],
                    image_name=notebook_config['expected_image_name'])
                if image_id != '':
                    print("Image was successfully created. It's ID is {}".format(image_id))
            else:
                print("Looks like it's first time we configure notebook server. Creating image.")
                try:
                    os.environ['conf_additional_tags'] = '{};ami:shared;endpoint_tag:{};'.format(
                        os.environ['conf_additional_tags'], os.environ['endpoint_name'])
                except KeyError:
                    os.environ['conf_additional_tags'] = 'ami:shared;endpoint_tag:{}'.format(
                        os.environ['endpoint_name'])
                image_id = dlab.actions_lib.create_image_from_instance(
                    tag_name=notebook_config['tag_name'], instance_name=notebook_config['instance_name'],
                    image_name=notebook_config['expected_image_name'])
                if image_id != '':
                    print("Image was successfully created. It's ID is {}".format(image_id))
        except Exception as err:
            dlab.fab.append_result("Failed creating image.", str(err))
            dlab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
            sys.exit(1)
    try:
        # generating output information
        ip_address = dlab.meta_lib.get_instance_ip_address(notebook_config['tag_name'],
                                                           notebook_config['instance_name']).get('Private')
        dns_name = dlab.meta_lib.get_instance_hostname(notebook_config['tag_name'], notebook_config['instance_name'])
        zeppelin_ip_url = "http://" + ip_address + ":8080/"
        zeppelin_dns_url = "http://" + dns_name + ":8080/"
        zeppelin_notebook_access_url = "https://{}/{}/".format(notebook_config['edge_instance_hostname'],
                                                               notebook_config['exploratory_name'])
        zeppelin_ungit_access_url = "https://{}/{}-ungit/".format(notebook_config['edge_instance_hostname'],
                                                                  notebook_config['exploratory_name'])
        ungit_ip_url = "http://" + ip_address + ":8085/{}-ungit/".format(notebook_config['exploratory_name'])
        print('[SUMMARY]')
        logging.info('[SUMMARY]')
        print("Instance name: {}".format(notebook_config['instance_name']))
        print("Private DNS: {}".format(dns_name))
        print("Private IP: {}".format(ip_address))
        print("Instance ID: {}".format(dlab.meta_lib.get_instance_by_name(notebook_config['tag_name'],
                                                                          notebook_config['instance_name'])))
        print("Instance type: {}".format(notebook_config['instance_type']))
        print("Key name: {}".format(notebook_config['key_name']))
        print("User key name: {}".format(notebook_config['user_keyname']))
        print("AMI name: {}".format(notebook_config['notebook_image_name']))
        print("Profile name: {}".format(notebook_config['role_profile_name']))
        print("SG name: {}".format(notebook_config['security_group_name']))
        print("Zeppelin URL: {}".format(zeppelin_ip_url))
        print("Zeppelin URL: {}".format(zeppelin_dns_url))
        print("Ungit URL: {}".format(ungit_ip_url))
        print('SSH access (from Edge node, via IP address): ssh -i {0}.pem {1}@{2}'.
              format(notebook_config['key_name'], notebook_config['dlab_ssh_user'], ip_address))
        print('SSH access (from Edge node, via FQDN): ssh -i {0}.pem {1}@{2}'.
              format(notebook_config['key_name'], notebook_config['dlab_ssh_user'], dns_name))

        with open("/root/result.json", 'w') as result:
            res = {"hostname": dns_name,
                   "ip": ip_address,
                   "instance_id": dlab.meta_lib.get_instance_by_name(notebook_config['tag_name'],
                                                                     notebook_config['instance_name']),
                   "master_keyname": os.environ['conf_key_name'],
                   "notebook_name": notebook_config['instance_name'],
                   "notebook_image_name": notebook_config['notebook_image_name'],
                   "Action": "Create new notebook server",
                   "exploratory_url": [
                       {"description": "Apache Zeppelin",
                        "url": zeppelin_notebook_access_url},
                       {"description": "Ungit",
                        "url": zeppelin_ungit_access_url}#,
                       #{"description": "Apache Zeppelin (via tunnel)",
                       # "url": zeppelin_ip_url},
                       #{"description": "Ungit (via tunnel)",
                       # "url": ungit_ip_url}
                   ]}
            result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Error with writing results.", str(err))
        dlab.actions_lib.remove_ec2(notebook_config['tag_name'], notebook_config['instance_name'])
        sys.exit(1)
