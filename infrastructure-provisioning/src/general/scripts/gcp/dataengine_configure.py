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

import datalab.actions_lib
import datalab.fab
import datalab.meta_lib
import json
from datalab.logger import logging
import multiprocessing
import os
import sys
import traceback
import subprocess
from fabric import *


def configure_slave(slave_number, data_engine):
    slave_name = data_engine['slave_node_name'] + '{}'.format(slave_number + 1)
    slave_hostname = GCPMeta.get_private_ip_address(slave_name)
    try:
        logging.info('[CREATING DATALAB SSH USER ON SLAVE NODE]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format \
            (slave_hostname, os.environ['conf_key_dir'] + data_engine['key_name'] + ".pem", initial_user,
             data_engine['datalab_ssh_user'], sudo_group)

        try:
            subprocess.run("~/scripts/{}.py {}".format('create_ssh_user', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to create ssh user on slave.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING USERs KEY ON SLAVE NODE]')
        additional_config = {"user_keyname": data_engine['project_name'],
                             "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            slave_hostname, os.environ['conf_key_dir'] + data_engine['key_name'] + ".pem", json.dumps(
                additional_config), data_engine['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_user_key', params), shell=True, check=True)
        except:
            datalab.fab.append_result("Failed installing users key")
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to install ssh user key on slave.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE PROXY ON SLAVE NODE]')
        additional_config = {"proxy_host": edge_instance_name, "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}"\
            .format(slave_hostname, slave_name, keyfile_name, json.dumps(additional_config),
                    data_engine['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_proxy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to configure proxy on slave.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES ON SLAVE NODE]')
        params = "--hostname {} --keyfile {} --user {} --region {} --edge_private_ip {}". \
            format(slave_hostname, keyfile_name, data_engine['datalab_ssh_user'], data_engine['region'],
                   edge_instance_private_ip)
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_prerequisites', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to install prerequisites on slave.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SLAVE NODE {}]'.format(slave + 1))
        params = "--hostname {} --keyfile {} --region {} --spark_version {} --hadoop_version {} --os_user {} " \
                 "--scala_version {} --master_ip {} --node_type {}". \
            format(slave_hostname, keyfile_name, data_engine['region'], os.environ['notebook_spark_version'],
                   os.environ['notebook_hadoop_version'], data_engine['datalab_ssh_user'],
                   os.environ['notebook_scala_version'], master_node_hostname, 'slave')
        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_dataengine', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to configure slave node.", str(err))
        sys.exit(1)

    if 'slave_gpu_type' in os.environ:
        try:
            logging.info('[INSTALLING GPU DRIVERS ON MASTER NODE]')
            params = "--hostname {} --keyfile {} --os_user {}".format(
                slave_hostname, keyfile_name, data_engine['datalab_ssh_user'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('common_install_gpu', params), shell=True, check=True)
            except:
                datalab.fab.append_result("Failed installing gpu drivers")
                raise Exception

        except Exception as err:
            datalab.fab.append_result("Failed to install GPU drivers.", str(err))
            GCPActions.remove_instance(notebook_config['instance_name'], notebook_config['zone'])
            sys.exit(1)


def clear_resources():
    for i in range(data_engine['instance_count'] - 1):
        slave_name = data_engine['slave_node_name'] + '{}'.format(i + 1)
        GCPActions.remove_instance(slave_name, data_engine['zone'])
    GCPActions.remove_instance(data_engine['master_node_name'], data_engine['zone'])


if __name__ == "__main__":
    try:
        GCPMeta = datalab.meta_lib.GCPMeta()
        GCPActions = datalab.actions_lib.GCPActions()
        logging.info('Generating infrastructure names and tags')
        data_engine = dict()
        data_engine['service_base_name'] = (os.environ['conf_service_base_name'])
        data_engine['edge_user_name'] = (os.environ['edge_user_name'])
        data_engine['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
        data_engine['endpoint_name'] = os.environ['endpoint_name'].replace('_', '-').lower()
        data_engine['endpoint_tag'] = data_engine['endpoint_name']
        data_engine['region'] = os.environ['gcp_region']
        data_engine['zone'] = os.environ['gcp_zone']
        try:
            if os.environ['gcp_vpc_name'] == '':
                raise KeyError
            else:
                data_engine['vpc_name'] = os.environ['gcp_vpc_name']
        except KeyError:
            data_engine['vpc_name'] = '{}-vpc'.format(data_engine['service_base_name'])
        if 'exploratory_name' in os.environ:
            data_engine['exploratory_name'] = os.environ['exploratory_name'].replace('_', '-').lower()
        else:
            data_engine['exploratory_name'] = ''
        if 'computational_name' in os.environ:
            data_engine['computational_name'] = os.environ['computational_name'].replace('_', '-').lower()
        else:
            data_engine['computational_name'] = ''

        data_engine['subnet_name'] = '{0}-{1}-{2}-subnet'.format(data_engine['service_base_name'],
                                                                 data_engine['project_name'],
                                                                 data_engine['endpoint_name'])
        data_engine['master_size'] = os.environ['gcp_dataengine_master_size']
        data_engine['slave_size'] = os.environ['gcp_dataengine_slave_size']
        data_engine['key_name'] = os.environ['conf_key_name']
        data_engine['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], data_engine['key_name'])
        data_engine['dataengine_service_account_name'] = '{}-{}-{}-ps-sa'.format(data_engine['service_base_name'],
                                                                                 data_engine['project_name'],
                                                                                 data_engine['endpoint_name'])

        if os.environ['conf_os_family'] == 'debian':
            initial_user = 'ubuntu'
            sudo_group = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            initial_user = 'ec2-user'
            sudo_group = 'wheel'
        data_engine['cluster_name'] = "{}-{}-{}-de-{}".format(data_engine['service_base_name'],
                                                              data_engine['project_name'],
                                                              data_engine['endpoint_name'],
                                                              data_engine['computational_name'])
        data_engine['master_node_name'] = data_engine['cluster_name'] + '-m'
        data_engine['slave_node_name'] = data_engine['cluster_name'] + '-s'
        data_engine['instance_count'] = int(os.environ['dataengine_instance_count'])
        data_engine['notebook_name'] = os.environ['notebook_instance_name']
        data_engine['gpu_accelerator_type'] = 'None'
        if os.environ['application'] in ('tensor', 'deeplearning'):
            data_engine['gpu_accelerator_type'] = os.environ['gcp_gpu_accelerator_type']
        data_engine['network_tag'] = '{0}-{1}-{2}-ps'.format(data_engine['service_base_name'],
                                                             data_engine['project_name'],
                                                             data_engine['endpoint_name'])
        master_node_hostname = GCPMeta.get_private_ip_address(data_engine['master_node_name'])
        edge_instance_name = '{0}-{1}-{2}-edge'.format(data_engine['service_base_name'],
                                                       data_engine['project_name'], data_engine['endpoint_tag'])
        edge_instance_hostname = GCPMeta.get_instance_public_ip_by_name(edge_instance_name)
        edge_instance_private_ip = GCPMeta.get_private_ip_address(edge_instance_name)
        data_engine['datalab_ssh_user'] = os.environ['conf_os_user']
        keyfile_name = "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        sys.exit(1)

    try:
        logging.info('[CREATING DATALAB SSH USER ON MASTER NODE]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format \
            (master_node_hostname, os.environ['conf_key_dir'] + data_engine['key_name'] + ".pem", initial_user,
             data_engine['datalab_ssh_user'], sudo_group)

        try:
            subprocess.run("~/scripts/{}.py {}".format('create_ssh_user', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to create ssh user on master.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING USERs KEY ON MASTER NODE]')
        additional_config = {"user_keyname": data_engine['project_name'],
                             "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            master_node_hostname, os.environ['conf_key_dir'] + data_engine['key_name'] + ".pem",
            json.dumps(additional_config), data_engine['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_user_key', params), shell=True, check=True)
        except:
            datalab.fab.append_result("Failed installing users key")
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to install ssh user on master.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE PROXY ON MASTER NODE]')
        additional_config = {"proxy_host": edge_instance_name, "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}"\
            .format(master_node_hostname, data_engine['master_node_name'], keyfile_name, json.dumps(additional_config),
                    data_engine['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_proxy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to configure proxy on master.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES ON MASTER NODE]')
        params = "--hostname {} --keyfile {} --user {} --region {} --edge_private_ip {}". \
            format(master_node_hostname, keyfile_name, data_engine['datalab_ssh_user'], data_engine['region'],
                   edge_instance_private_ip)
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_prerequisites', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to install prerequisites on master.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE MASTER NODE]')
        params = "--hostname {} --keyfile {} --region {} --spark_version {} --hadoop_version {} --os_user {} " \
                 "--scala_version {} --master_ip {} --node_type {}".\
            format(master_node_hostname, keyfile_name, data_engine['region'], os.environ['notebook_spark_version'],
                   os.environ['notebook_hadoop_version'], data_engine['datalab_ssh_user'],
                   os.environ['notebook_scala_version'], master_node_hostname,
                   'master')
        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_dataengine', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure master node", str(err))
        clear_resources()
        sys.exit(1)

    if 'master_gpu_type' in os.environ:
        try:
            logging.info('[INSTALLING GPU DRIVERS ON MASTER NODE]')
            params = "--hostname {} --keyfile {} --os_user {}".format(
                master_node_hostname, keyfile_name, data_engine['datalab_ssh_user'])
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
        jobs = []
        for slave in range(data_engine['instance_count'] - 1):
            p = multiprocessing.Process(target=configure_slave, args=(slave, data_engine))
            jobs.append(p)
            p.start()
        for job in jobs:
            job.join()
        for job in jobs:
            if job.exitcode != 0:
                raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure slave nodes", str(err))
        clear_resources()
        sys.exit(1)

    try:
        logging.info('[SETUP EDGE REVERSE PROXY TEMPLATE]')
        notebook_instance_ip = GCPMeta.get_private_ip_address(data_engine['notebook_name'])
        additional_info = {
            "computational_name": data_engine['computational_name'],
            "master_node_hostname": master_node_hostname,
            "notebook_instance_ip": notebook_instance_ip,
            "instance_count": data_engine['instance_count'],
            "master_node_name": data_engine['master_node_name'],
            "slave_node_name": data_engine['slave_node_name'],
            "tensor": False
        }
        params = "--edge_hostname {} " \
                 "--keyfile {} " \
                 "--os_user {} " \
                 "--type {} " \
                 "--exploratory_name {} " \
                 "--additional_info '{}'"\
            .format(edge_instance_private_ip,
                    keyfile_name,
                    data_engine['datalab_ssh_user'],
                    'spark',
                    data_engine['exploratory_name'],
                    json.dumps(additional_info))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_reverse_proxy', params), shell=True, check=True)
        except:
            datalab.fab.append_result("Failed edge reverse proxy template")
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure reverse proxy", str(err))
        clear_resources()
        sys.exit(1)

    try:
        ip_address = GCPMeta.get_private_ip_address(data_engine['master_node_name'])
        spark_master_url = "http://" + ip_address + ":8080"
        spark_master_access_url = "https://" + edge_instance_hostname + "/{}/".format(
            data_engine['exploratory_name'] + '_' + data_engine['computational_name'])
        logging.info('[SUMMARY]')
        logging.info("Service base name: {}".format(data_engine['service_base_name']))
        logging.info("Region: {}".format(data_engine['region']))
        logging.info("Cluster name: {}".format(data_engine['cluster_name']))
        logging.info("Master node shape: {}".format(data_engine['master_size']))
        logging.info("Slave node shape: {}".format(data_engine['slave_size']))
        logging.info("Instance count: {}".format(str(data_engine['instance_count'])))
        with open("/root/result.json", 'w') as result:
            res = {"hostname": data_engine['cluster_name'],
                   "instance_id": data_engine['master_node_name'],
                   "key_name": data_engine['key_name'],
                   "Action": "Create new Data Engine",
                   "computational_url": [
                       {"description": "Apache Spark Master",
                        "url": spark_master_access_url},
                       # {"description": "Apache Spark Master (via tunnel)",
                       # "url": spark_master_url}
                   ]
                   }
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        clear_resources()
        sys.exit(1)
