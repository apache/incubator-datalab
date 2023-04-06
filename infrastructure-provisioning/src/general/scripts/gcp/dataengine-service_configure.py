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
import datalab.notebook_lib
import json
from datalab.logger import logging
import multiprocessing
import os
import sys
import traceback
import subprocess
from fabric import *


def configure_dataengine_service(instance, dataproc_conf):
    dataproc_conf['instance_ip'] = GCPMeta.get_private_ip_address(instance)
    # configuring proxy on Data Engine service
    try:
        logging.info('[CONFIGURE PROXY ON DATAENGINE SERVICE]')
        additional_config = {"proxy_host": dataproc_conf['edge_instance_name'], "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}" \
            .format(dataproc_conf['instance_ip'], dataproc_conf['cluster_name'], dataproc_conf['key_path'],
                    json.dumps(additional_config), dataproc_conf['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_proxy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure proxy.", str(err))
        GCPActions.delete_dataproc_cluster(dataproc_conf['cluster_name'], os.environ['gcp_region'])
        sys.exit(1)

    try:
        logging.info('[CONFIGURE DATAENGINE SERVICE]')
        try:
            global conn
            conn = datalab.fab.init_datalab_connection(dataproc_conf['instance_ip'], dataproc_conf['datalab_ssh_user'], dataproc_conf['key_path'])
            datalab.fab.configure_data_engine_service_livy(dataproc_conf['instance_ip'],
                                                           dataproc_conf['datalab_ssh_user'],
                                                           dataproc_conf['key_path'])
            datalab.notebook_lib.install_os_pkg([['python3-pip', 'N/A']])
            datalab.fab.configure_data_engine_service_pip(dataproc_conf['instance_ip'],
                                                          dataproc_conf['datalab_ssh_user'],
                                                          dataproc_conf['key_path'])
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure dataengine service.", str(err))
        GCPActions.delete_dataproc_cluster(dataproc_conf['cluster_name'], os.environ['gcp_region'])
        sys.exit(1)

    try:
        logging.info('[SETUP EDGE REVERSE PROXY TEMPLATE]')
        slaves = []
        for idx, instance in enumerate(dataproc_conf['cluster_core_instances']):
            slave_ip = GCPMeta.get_private_ip_address(instance)
            slave = {
                'name': 'datanode{}'.format(idx + 1),
                'ip': slave_ip,
                'dns': "{0}.c.{1}.internal".format(instance, os.environ['gcp_project_id'])
            }
            slaves.append(slave)
        additional_info = {
            "computational_name": dataproc_conf['computational_name'],
            "master_ip": dataproc_conf['master_ip'],
            "master_dns": "{0}.c.{1}.internal".format(dataproc_conf['master_name'], os.environ['gcp_project_id']),
            "slaves": slaves,
            "tensor": False
        }
        params = "--edge_hostname {} " \
                 "--keyfile {} " \
                 "--os_user {} " \
                 "--type {} " \
                 "--exploratory_name {} " \
                 "--additional_info '{}'"\
            .format(dataproc_conf['edge_instance_hostname'],
                    dataproc_conf['key_path'],
                    dataproc_conf['datalab_ssh_user'],
                    'dataengine-service',
                    dataproc_conf['exploratory_name'],
                    json.dumps(additional_info))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_reverse_proxy', params), shell=True, check=True)
        except:
            datalab.fab.append_result("Failed edge reverse proxy template")
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure reverse proxy.", str(err))
        GCPActions.delete_dataproc_cluster(dataproc_conf['cluster_name'], os.environ['gcp_region'])
        sys.exit(1)


if __name__ == "__main__":
    try:
        GCPMeta = datalab.meta_lib.GCPMeta()
        GCPActions = datalab.actions_lib.GCPActions()
        logging.info('Generating infrastructure names and tags')
        dataproc_conf = dict()
        if 'exploratory_name' in os.environ:
            dataproc_conf['exploratory_name'] = os.environ['exploratory_name'].replace('_', '-').lower()
        else:
            dataproc_conf['exploratory_name'] = ''
        if 'computational_name' in os.environ:
            dataproc_conf['computational_name'] = os.environ['computational_name'].replace('_', '-').lower()
        else:
            dataproc_conf['computational_name'] = ''
        dataproc_conf['service_base_name'] = (os.environ['conf_service_base_name'])
        dataproc_conf['edge_user_name'] = (os.environ['edge_user_name'])
        dataproc_conf['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
        dataproc_conf['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
        dataproc_conf['key_name'] = os.environ['conf_key_name']
        dataproc_conf['key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        dataproc_conf['region'] = os.environ['gcp_region']
        dataproc_conf['zone'] = os.environ['gcp_zone']
        dataproc_conf['subnet'] = '{0}-{1}-{2}-subnet'.format(dataproc_conf['service_base_name'],
                                                              dataproc_conf['project_name'],
                                                              dataproc_conf['endpoint_name'])
        dataproc_conf['cluster_name'] = '{0}-{1}-{2}-des-{3}'.format(dataproc_conf['service_base_name'],
                                                                     dataproc_conf['project_name'],
                                                                     dataproc_conf['endpoint_name'],
                                                                     dataproc_conf['computational_name'])
        dataproc_conf['cluster_tag'] = '{0}-{1}-{2}-ps'.format(dataproc_conf['service_base_name'],
                                                               dataproc_conf['project_name'],
                                                               dataproc_conf['endpoint_name'])
        dataproc_conf['bucket_name'] = '{0}-{1}-{2}-bucket'.format(dataproc_conf['service_base_name'],
                                                                   dataproc_conf['project_name'],
                                                                   dataproc_conf['endpoint_name'])
        dataproc_conf['release_label'] = os.environ['dataproc_version']
        dataproc_conf['cluster_label'] = {os.environ['notebook_instance_name']: "not-configured"}
        dataproc_conf['dataproc_service_account_name'] = '{0}-{1}-{2}-ps-sa'.format(dataproc_conf['service_base_name'],
                                                                                    dataproc_conf['project_name'],
                                                                                    dataproc_conf['endpoint_name'])
        dataproc_conf['dataproc_unique_index'] = GCPMeta.get_index_by_service_account_name(
            dataproc_conf['dataproc_service_account_name'])
        service_account_email = "{}-{}@{}.iam.gserviceaccount.com".format(dataproc_conf['service_base_name'],
                                                                          dataproc_conf['dataproc_unique_index'],
                                                                          os.environ['gcp_project_id'])

        dataproc_conf['edge_instance_name'] = '{0}-{1}-{2}-edge'.format(dataproc_conf['service_base_name'],
                                                                        dataproc_conf['project_name'],
                                                                        dataproc_conf['endpoint_name'])
        dataproc_conf['edge_instance_hostname'] = GCPMeta.get_private_ip_address(
            dataproc_conf['edge_instance_name'])
        dataproc_conf['datalab_ssh_user'] = os.environ['conf_os_user']
        dataproc_conf['master_name'] = dataproc_conf['cluster_name'] + '-m'
        dataproc_conf['master_ip'] = GCPMeta.get_private_ip_address(dataproc_conf['master_name'])
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        GCPActions.delete_dataproc_cluster(dataproc_conf['cluster_name'], os.environ['gcp_region'])
        sys.exit(1)

    try:
        res = GCPMeta.get_list_instances(os.environ['gcp_zone'], dataproc_conf['cluster_name'])
        dataproc_conf['cluster_instances'] = [i.get('name') for i in res['items']]
    except Exception as err:
        traceback.print_exc()
        raise Exception

    dataproc_conf['cluster_core_instances'] = list()
    for instance in dataproc_conf['cluster_instances']:
        if "{}-w-".format(dataproc_conf['cluster_name']) in instance:
            dataproc_conf['cluster_core_instances'].append(instance)

    try:
        jobs = []
        for instance in dataproc_conf['cluster_instances']:
            p = multiprocessing.Process(target=configure_dataengine_service, args=(instance, dataproc_conf))
            jobs.append(p)
            p.start()
        for job in jobs:
            job.join()
        for job in jobs:
            if job.exitcode != 0:
                raise Exception
    except Exception as err:
        GCPActions.delete_dataproc_cluster(dataproc_conf['cluster_name'], os.environ['gcp_region'])
        datalab.fab.append_result("Failed to configure Dataengine-service", str(err))
        traceback.print_exc()
        raise Exception

    try:
        dataproc_master_access_url = "https://" + dataproc_conf['edge_instance_hostname'] + "/{}/".format(
            dataproc_conf['exploratory_name'] + '_' + dataproc_conf['computational_name'])
        logging.info('[SUMMARY]')
        logging.info("Service base name: {}".format(dataproc_conf['service_base_name']))
        logging.info("Cluster name: {}".format(dataproc_conf['cluster_name']))
        logging.info("Key name: {}".format(dataproc_conf['key_name']))
        logging.info("Region: {}".format(dataproc_conf['region']))
        logging.info("Zone: {}".format(dataproc_conf['zone']))
        logging.info("Subnet: {}".format(dataproc_conf['subnet']))
        logging.info("Dataproc version: {}".format(dataproc_conf['release_label']))
        logging.info("Dataproc master node shape: {}".format(os.environ['dataproc_master_instance_type']))
        logging.info("Dataproc slave node shape: {}".format(os.environ['dataproc_slave_instance_type']))
        logging.info("Master count: {}".format(os.environ['dataproc_master_count']))
        logging.info("Slave count: {}".format(os.environ['dataproc_slave_count']))
        logging.info("Preemptible count: {}".format(os.environ['dataproc_preemptible_count']))
        logging.info("Notebook hostname: {}".format(os.environ['notebook_instance_name']))
        logging.info("Bucket name: {}".format(dataproc_conf['bucket_name']))
        with open("/root/result.json", 'w') as result:
            res = {"hostname": dataproc_conf['cluster_name'],
                   "key_name": dataproc_conf['key_name'],
                   "instance_id": dataproc_conf['cluster_name'],
                   "user_own_bucket_name": dataproc_conf['bucket_name'],
                   "Action": "Create new Dataproc cluster",
                   "computational_url": [
                       {"description": "Dataproc Master",
                        "url": dataproc_master_access_url}
                   ]
                   }
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        GCPActions.delete_dataproc_cluster(dataproc_conf['cluster_name'], os.environ['gcp_region'])
        sys.exit(1)
