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

import datalab.fab
import datalab.actions_lib
import datalab.meta_lib
import json
from datalab.logger import logging
import os
import sys
import traceback
import subprocess
from fabric import *
import uuid

if __name__ == "__main__":
    try:
        GCPMeta = datalab.meta_lib.GCPMeta()
        GCPActions = datalab.actions_lib.GCPActions()
        notebook_config = dict()
        try:
            notebook_config['exploratory_name'] = (os.environ['exploratory_name']).replace('_', '-').lower()
        except:
            notebook_config['exploratory_name'] = ''
        notebook_config['service_base_name'] = (os.environ['conf_service_base_name'])
        notebook_config['instance_type'] = os.environ['gcp_notebook_instance_size']
        notebook_config['key_name'] = os.environ['conf_key_name']
        notebook_config['edge_user_name'] = (os.environ['edge_user_name'])
        notebook_config['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
        notebook_config['project_tag'] = notebook_config['project_name']
        notebook_config['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
        notebook_config['endpoint_tag'] = notebook_config['endpoint_name']
        notebook_config['instance_name'] = '{0}-{1}-{2}-nb-{3}'.format(notebook_config['service_base_name'],
                                                                       notebook_config['project_name'],
                                                                       notebook_config['endpoint_name'],
                                                                       notebook_config['exploratory_name'])
        notebook_config['image_enabled'] = os.environ['conf_image_enabled']
        notebook_config['shared_image_enabled'] = os.environ['conf_shared_image_enabled']
        if notebook_config['shared_image_enabled'] == 'false':
            notebook_config['expected_primary_image_name'] = '{}-{}-{}-{}-primary-image'.format(
                notebook_config['service_base_name'], notebook_config['project_name'], notebook_config['endpoint_name'],
                os.environ['application'])
            notebook_config['expected_secondary_image_name'] = '{}-{}-{}-{}-secondary-image'.format(
                notebook_config['service_base_name'], notebook_config['project_name'], notebook_config['endpoint_name'],
                os.environ['application'])
            notebook_config['image_labels'] = {"sbn": notebook_config['service_base_name'],
                                               "endpoint_tag": notebook_config['endpoint_tag'],
                                               "project_tag": notebook_config['project_tag'],
                                               os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value']}
        else:
            notebook_config['expected_primary_image_name'] = '{}-{}-{}-primary-image'.format(
                notebook_config['service_base_name'], notebook_config['endpoint_name'], os.environ['application'])
            notebook_config['expected_secondary_image_name'] = '{}-{}-{}-secondary-image'.format(
                notebook_config['service_base_name'], notebook_config['endpoint_name'], os.environ['application'])
            notebook_config['image_labels'] = {"sbn": notebook_config['service_base_name'],
                                               "endpoint_tag": notebook_config['endpoint_tag'],
                                               os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value']}
        # generating variables regarding EDGE proxy on Notebook instance
        instance_hostname = GCPMeta.get_private_ip_address(notebook_config['instance_name'])
        edge_instance_name = '{0}-{1}-{2}-edge'.format(notebook_config['service_base_name'],
                                                       notebook_config['project_name'],
                                                       notebook_config['endpoint_name'])
        edge_instance_hostname = GCPMeta.get_instance_public_ip_by_name(edge_instance_name)
        edge_instance_private_ip = GCPMeta.get_private_ip_address(edge_instance_name)
        notebook_config['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        notebook_config['datalab_ssh_user'] = os.environ['conf_os_user']
        notebook_config['zone'] = os.environ['gcp_zone']
        notebook_config['shared_image_enabled'] = os.environ['conf_shared_image_enabled']
        if "gcp_wrapped_csek" in os.environ:
            notebook_config['gcp_wrapped_csek'] = os.environ['gcp_wrapped_csek']
        else:
            notebook_config['gcp_wrapped_csek'] = ''
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary", str(err))
        GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
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
            instance_hostname, notebook_config['ssh_key_path'], notebook_config['initial_user'],
            notebook_config['datalab_ssh_user'], notebook_config['sudo_group'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('create_ssh_user', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed creating ssh user 'datalab'.", str(err))
        GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    # configuring proxy on Notebook instance
    try:
        logging.info('[CONFIGURE PROXY ON JUPYTER INSTANCE]')
        additional_config = {"proxy_host": edge_instance_name, "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}"\
            .format(instance_hostname, notebook_config['instance_name'], notebook_config['ssh_key_path'],
                    json.dumps(additional_config), notebook_config['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_proxy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure proxy.", str(err))
        GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    # updating repositories & installing python packages
    try:
        logging.info('[INSTALLING PREREQUISITES TO JUPYTER NOTEBOOK INSTANCE]')
        params = "--hostname {} --keyfile {} --user {} --region {} --edge_private_ip {}". \
            format(instance_hostname, notebook_config['ssh_key_path'], notebook_config['datalab_ssh_user'],
                   os.environ['gcp_region'], edge_instance_private_ip)
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_prerequisites', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed installing apps: apt & pip.", str(err))
        GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    # installing and configuring jupiter and all dependencies
    try:
        logging.info('[CONFIGURE JUPYTER NOTEBOOK INSTANCE]')
        params = "--hostname {} --keyfile {} " \
                 "--region {} --spark_version {} " \
                 "--hadoop_version {} --os_user {} " \
                 "--scala_version {} " \
                 "--exploratory_name {} "\
                 "--edge_ip {}".\
            format(instance_hostname, notebook_config['ssh_key_path'],
                   os.environ['gcp_region'], os.environ['notebook_spark_version'],
                   os.environ['notebook_hadoop_version'], notebook_config['datalab_ssh_user'],
                   os.environ['notebook_scala_version'],
                   notebook_config['exploratory_name'], edge_instance_private_ip)
        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_jupyter_node', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure jupyter.", str(err))
        GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    try:
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": os.environ['project_name'],
                             "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            instance_hostname, notebook_config['ssh_key_path'], json.dumps(additional_config),
            notebook_config['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_user_key', params), shell=True, check=True)
        except:
            datalab.fab.append_result("Failed installing users key")
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed installing users key.", str(err))
        GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    try:
        logging.info('[SETUP USER GIT CREDENTIALS]')
        params = '--os_user {} --notebook_ip {} --keyfile "{}"' \
            .format(notebook_config['datalab_ssh_user'], instance_hostname, notebook_config['ssh_key_path'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_download_git_certfile', params), shell=True, check=True)
            subprocess.run("~/scripts/{}.py {}".format('manage_git_creds', params), shell=True, check=True)
        except:
            datalab.fab.append_result("Failed setup git credentials")
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to setup git credentials.", str(err))
        GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    if notebook_config['image_enabled'] == 'true':
        try:
            logging.info('[CREATING IMAGE]')
            primary_image_id = GCPMeta.get_image_by_name(notebook_config['expected_primary_image_name'])
            if primary_image_id == '':
                logging.info("Looks like it's first time we configure notebook server. Creating images.")
                image_id_list = GCPActions.create_image_from_instance_disks(
                    notebook_config['expected_primary_image_name'], notebook_config['expected_secondary_image_name'],
                    notebook_config['instance_name'], notebook_config['zone'], notebook_config['image_labels'],
                    notebook_config['gcp_wrapped_csek'])
                if image_id_list and image_id_list[0] != '':
                    logging.info("Image of primary disk was successfully created. It's ID is {}".format(image_id_list[0]))
                else:
                    logging.info("Looks like another image creating operation for your template have been started a "
                          "moment ago.")
                if image_id_list and image_id_list[1] != '':
                    logging.info("Image of secondary disk was successfully created. It's ID is {}".format(image_id_list[1]))
        except Exception as err:
            datalab.fab.append_result("Failed creating image.", str(err))
            GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
            GCPActions.remove_image(notebook_config['expected_primary_image_name'])
            GCPActions.remove_image(notebook_config['expected_secondary_image_name'])
            sys.exit(1)

    if os.environ['notebook_create_keycloak_client'] == 'True':
        try:
            logging.info('[SETUP KEYCLOAK CLIENT]')
            notebook_config['keycloak_client_name'] = '{}-{}-{}-{}'\
                .format(notebook_config['service_base_name'], notebook_config['project_name'],
                        notebook_config['endpoint_name'], notebook_config['exploratory_name'])
            notebook_config['keycloak_client_secret'] = str(uuid.uuid4())
            keycloak_params = "--service_base_name {} --keycloak_auth_server_url {} --keycloak_realm_name {} " \
                              "--keycloak_user {} --keycloak_user_password {} --keycloak_client_secret {} " \
                              "--project_name {} --endpoint_name {} --exploratory_name {}"\
                .format(notebook_config['service_base_name'], os.environ['keycloak_auth_server_url'],
                        os.environ['keycloak_realm_name'], os.environ['keycloak_user'],
                        os.environ['keycloak_user_password'], notebook_config['keycloak_client_secret'],
                        notebook_config['project_name'], notebook_config['endpoint_name'],
                        notebook_config['exploratory_name'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('configure_keycloak', keycloak_params), shell=True, check=True)
            except:
                datalab.fab.append_result("Failed setup keycloak client")
                raise Exception

            try:
                conn = datalab.fab.init_datalab_connection(instance_hostname, notebook_config['datalab_ssh_user'],
                                                           notebook_config['ssh_key_path'], '', False)
                content = json.loads(conn.sudo("cat /home/{}/.local/share/jupyter/kernels/py3spark_local/kernel.json"
                                               .format(notebook_config['datalab_ssh_user'])).stdout)
                content['env']['KEYCLOAK_CLIENT'] = notebook_config['keycloak_client_name']
                content['env']['KEYCLOAK_SECRET'] = notebook_config['keycloak_client_secret']
                conn.sudo("echo '{}' > /home/{}/.local/share/jupyter/kernels/py3spark_local/kernel.json"
                          .format(json.dumps(content), notebook_config['datalab_ssh_user']))
                conn.sudo('systemctl restart jupyter-notebook')
            except:
                datalab.fab.append_result("Failed to write variables to .bashrc")
                raise Exception

        except Exception as err:
            datalab.fab.append_result("Failed setup keycloak client ", str(err))

    if os.environ['gpu_enabled'] == 'True':
        try:
            logging.info('[INSTALLING GPU DRIVERS]')
            params = "--hostname {} --keyfile {} --os_user {}".format(
                instance_hostname, notebook_config['ssh_key_path'], notebook_config['datalab_ssh_user'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('common_install_gpu', params), shell=True, check=True)
            except:
                datalab.fab.append_result("Failed installing gpu drivers")
                raise Exception

        except Exception as err:
            datalab.fab.append_result("Failed to install GPU drivers.", str(err))
            GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
            sys.exit(1)

    try:
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
            .format(edge_instance_private_ip,
                    notebook_config['ssh_key_path'],
                    notebook_config['datalab_ssh_user'],
                    'jupyter',
                    notebook_config['exploratory_name'],
                    json.dumps(additional_info))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_reverse_proxy', params), shell=True, check=True)
        except:
            datalab.fab.append_result("Failed edge reverse proxy template")
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to set edge reverse proxy template.", str(err))
        GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)

    try:
        # generating output information
        ip_address = GCPMeta.get_private_ip_address(notebook_config['instance_name'])
        jupyter_ip_url = "http://" + ip_address + ":8888/{}/".format(notebook_config['exploratory_name'])
        ungit_ip_url = "http://" + ip_address + ":8085/{}-ungit/".format(notebook_config['exploratory_name'])
        jupyter_notebook_access_url = "https://" + edge_instance_hostname + "/{}/".format(
            notebook_config['exploratory_name'])
        jupyter_ungit_access_url = "https://" + edge_instance_hostname + "/{}-ungit/".format(
            notebook_config['exploratory_name'])
        logging.info('[SUMMARY]')
        logging.info("Instance name: {}".format(notebook_config['instance_name']))
        logging.info("Private IP: {}".format(ip_address))
        logging.info("Instance type: {}".format(notebook_config['instance_type']))
        logging.info("Key name: {}".format(notebook_config['key_name']))
        logging.info("User key name: {}".format(os.environ['project_name']))
        logging.info("Jupyter URL: {}".format(jupyter_ip_url))
        logging.info("Ungit URL: {}".format(ungit_ip_url))
        logging.info("ReverseProxyNotebook".format(jupyter_notebook_access_url))
        logging.info("ReverseProxyUngit".format(jupyter_ungit_access_url))
        logging.info('SSH access (from Edge node, via IP address): ssh -i {0}.pem {1}@{2}'.format(
            notebook_config['key_name'], notebook_config['datalab_ssh_user'], ip_address))

        with open("/root/result.json", 'w') as result:
            res = {"hostname": ip_address,
                   "ip": ip_address,
                   "instance_id": notebook_config['instance_name'],
                   "master_keyname": os.environ['conf_key_name'],
                   "notebook_name": notebook_config['instance_name'],
                   "Action": "Create new notebook server",
                   "exploratory_url": [
                       {"description": "Jupyter",
                        "url": jupyter_notebook_access_url},
                       {"description": "Ungit",
                        "url": jupyter_ungit_access_url}#,
                       #{"description": "Jupyter (via tunnel)",
                       # "url": jupyter_ip_url},
                       #{"description": "Ungit (via tunnel)",
                       # "url": ungit_ip_url}
                   ]}
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Failed to generate output information", str(err))
        GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
        sys.exit(1)
