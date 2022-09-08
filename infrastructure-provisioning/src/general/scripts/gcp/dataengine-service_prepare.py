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
import time
import traceback
import subprocess
from Crypto.PublicKey import RSA
from fabric import *
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--uuid', type=str, default='')
parser.add_argument('--access_password', type=str, default='')
args = parser.parse_args()

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
        if os.path.exists('/response/.dataproc_creating_{}'.format(dataproc_conf['exploratory_name'])):
            time.sleep(30)
        dataproc_conf['service_base_name'] = (os.environ['conf_service_base_name'])
        dataproc_conf['edge_user_name'] = (os.environ['edge_user_name'])
        dataproc_conf['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
        dataproc_conf['project_tag'] = dataproc_conf['project_name']
        dataproc_conf['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
        dataproc_conf['endpoint_tag'] = dataproc_conf['endpoint_name']
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
        additional_tags = os.environ['tags'].replace("': '", ":").replace("', '", ",").replace("{'", "").replace(
            "'}", "").lower()

        dataproc_conf['cluster_labels'] = {
            os.environ['notebook_instance_name']: "not-configured",
            "name": dataproc_conf['cluster_name'],
            "sbn": dataproc_conf['service_base_name'],
            "notebook_name": os.environ['notebook_instance_name'],
            "product": "datalab",
            "computational_name": dataproc_conf['computational_name']
        }

        for tag in additional_tags.split(','):
            label_key = tag.split(':')[0]
            label_value = tag.split(':')[1].replace('_', '-')
            if '@' in label_value:
                label_value = label_value[:label_value.find('@')]
            if label_value != '':
                dataproc_conf['cluster_labels'].update({label_key: label_value})
        dataproc_conf['dataproc_service_account_name'] = '{0}-{1}-{2}-ps-sa'.format(dataproc_conf['service_base_name'],
                                                                                    dataproc_conf['project_name'],
                                                                                    dataproc_conf['endpoint_name'])
        dataproc_conf['dataproc_unique_index'] = GCPMeta.get_index_by_service_account_name(
            dataproc_conf['dataproc_service_account_name'])
        service_account_email = "{}-{}@{}.iam.gserviceaccount.com".format(dataproc_conf['service_base_name'],
                                                                          dataproc_conf['dataproc_unique_index'],
                                                                          os.environ['gcp_project_id'])
        dataproc_conf['edge_instance_hostname'] = '{0}-{1}-{2}-edge'.format(dataproc_conf['service_base_name'],
                                                                            dataproc_conf['project_name'],
                                                                            dataproc_conf['endpoint_name'])
        dataproc_conf['datalab_ssh_user'] = os.environ['conf_os_user']
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary. Exception:" + str(err))
        sys.exit(1)

    edge_status = GCPMeta.get_instance_status(dataproc_conf['edge_instance_hostname'])
    if edge_status != 'RUNNING':
        logging.info('ERROR: Edge node is unavailable! Aborting...')
        logging.info('ERROR: Edge node is unavailable! Aborting...')
        ssn_hostname = GCPMeta.get_private_ip_address(dataproc_conf['service_base_name'] + '-ssn')
        datalab.fab.put_resource_status('edge', 'Unavailable', os.environ['ssn_datalab_path'],
                                        os.environ['conf_os_user'],
                                        ssn_hostname)
        datalab.fab.append_result("Edge node is unavailable")
        sys.exit(1)

    logging.info("Will create exploratory environment with edge node as access point as following: ".format(
        json.dumps(dataproc_conf, sort_keys=True, indent=4, separators=(',', ': '))))
    logging.info(json.dumps(dataproc_conf))

    try:
        GCPMeta.dataproc_waiter(dataproc_conf['cluster_labels'])
        subprocess.run('touch /response/.dataproc_creating_{}'.format(os.environ['exploratory_name']), shell=True, check=True)
    except Exception as err:
        traceback.print_exc()
        datalab.fab.append_result("Dataproc waiter fail.", str(err))
        sys.exit(1)

    subprocess.run("echo Waiting for changes to propagate; sleep 10", shell=True, check=True)

    if 'master_gpu_count' in os.environ:
        dataproc_cluster = json.loads(open('/root/templates/dataengine-service_cluster_with_gpu.json').read())
        dataproc_cluster['config']['masterConfig']['accelerators'][0]['acceleratorCount'] = int(os.environ['master_gpu_count'])
        dataproc_cluster['config']['masterConfig']['accelerators'][0]['acceleratorTypeUri'] = os.environ['master_gpu_type']
        dataproc_cluster['config']['workerConfig']['accelerators'][0]['acceleratorCount'] = int(os.environ['slave_gpu_count'])
        dataproc_cluster['config']['workerConfig']['accelerators'][0]['acceleratorTypeUri'] = os.environ['slave_gpu_type']
        gpu_driver = 'gs://goog-dataproc-initialization-actions-{}/gpu/install_gpu_driver.sh'.format(dataproc_conf['region'])
        dataproc_cluster['config']['initializationActions'][0]['executableFile'] = gpu_driver

    else:
        dataproc_cluster = json.loads(open('/root/templates/dataengine-service_cluster.json').read())

    dataproc_cluster['projectId'] = os.environ['gcp_project_id']
    dataproc_cluster['clusterName'] = dataproc_conf['cluster_name']
    dataproc_cluster['labels'] = dataproc_conf['cluster_labels']
    dataproc_cluster['config']['configBucket'] = dataproc_conf['bucket_name']
    dataproc_cluster['config']['gceClusterConfig']['serviceAccount'] = service_account_email
    dataproc_cluster['config']['gceClusterConfig']['zoneUri'] = dataproc_conf['zone']
    dataproc_cluster['config']['gceClusterConfig']['subnetworkUri'] = dataproc_conf['subnet']
    dataproc_cluster['config']['masterConfig']['machineTypeUri'] = os.environ['dataproc_master_instance_type']
    dataproc_cluster['config']['workerConfig']['machineTypeUri'] = os.environ['dataproc_slave_instance_type']
    dataproc_cluster['config']['masterConfig']['numInstances'] = int(os.environ['dataproc_master_count'])
    dataproc_cluster['config']['workerConfig']['numInstances'] = int(os.environ['dataproc_slave_count'])
    if int(os.environ['dataproc_preemptible_count']) != 0:
        dataproc_cluster['config']['secondaryWorkerConfig']['numInstances'] = int(
            os.environ['dataproc_preemptible_count'])
    else:
        del dataproc_cluster['config']['secondaryWorkerConfig']
    dataproc_cluster['config']['softwareConfig']['imageVersion'] = dataproc_conf['release_label']
    ssh_user_pubkey = open('{}{}.pub'.format(os.environ['conf_key_dir'], dataproc_conf['project_name'])).read()
    key = RSA.importKey(open(dataproc_conf['key_path'], 'rb').read())
    ssh_admin_pubkey = key.publickey().exportKey("OpenSSH").decode('UTF-8')
    dataproc_cluster['config']['gceClusterConfig']['metadata']['ssh-keys'] = '{0}:{1}{0}:{2}'.format(
        dataproc_conf['datalab_ssh_user'], ssh_user_pubkey, ssh_admin_pubkey)
    dataproc_cluster['config']['gceClusterConfig']['tags'][0] = dataproc_conf['cluster_tag']
    with open('/root/result.json', 'w') as f:
        data = {"hostname": dataproc_conf['cluster_name'], "error": ""}
        json.dump(data, f)

    try:
        logging.info('[Creating Dataproc Cluster]')
        params = "--region {0} --bucket {1} --params '{2}'".format(dataproc_conf['region'],
                                                                   dataproc_conf['bucket_name'],
                                                                   json.dumps(dataproc_cluster))

        try:
            subprocess.run("~/scripts/{}.py {}".format('dataengine-service_create', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception

        keyfile_name = "/root/keys/{}.pem".format(dataproc_conf['key_name'])
        subprocess.run('rm /response/.dataproc_creating_{}'.format(os.environ['exploratory_name']), shell=True, check=True)
    except Exception as err:
        datalab.fab.append_result("Failed to create Dataproc Cluster.", str(err))
        subprocess.run('rm /response/.dataproc_creating_{}'.format(os.environ['exploratory_name']), shell=True, check=True)
        sys.exit(1)
