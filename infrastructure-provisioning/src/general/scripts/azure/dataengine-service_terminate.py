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
import os
import sys
import traceback
from datalab.logger import logging


if __name__ == "__main__":
    try:
        # generating variables dictionary
        logging.info('Generating infrastructure names and tags')
        hdinsight_conf = dict()
        AzureActions = datalab.actions_lib.AzureActions()
        AzureMeta = datalab.meta_lib.AzureMeta()
        if 'exploratory_name' in os.environ:
            hdinsight_conf['exploratory_name'] = os.environ['exploratory_name'].replace('_', '-').lower()
        else:
            hdinsight_conf['exploratory_name'] = ''
        if 'computational_name' in os.environ:
            hdinsight_conf['computational_name'] = os.environ['computational_name'].replace('_', '-').lower()
        else:
            hdinsight_conf['computational_name'] = ''
        hdinsight_conf['service_base_name'] = (os.environ['conf_service_base_name'])
        hdinsight_conf['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
        hdinsight_conf['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
        hdinsight_conf['resource_group_name'] = os.environ['azure_resource_group_name']
        hdinsight_conf['region'] = os.environ['azure_region']
        hdinsight_conf['cluster_name'] = '{}-{}-{}-des-{}'.format(hdinsight_conf['service_base_name'],
                                                                  hdinsight_conf['project_name'],
                                                                  hdinsight_conf['endpoint_name'],
                                                                  hdinsight_conf['computational_name'])
        hdinsight_conf['storage_account_name_tag'] = ('{}-bucket'.format(hdinsight_conf['cluster_name'])).lower()
        hdinsight_conf['container_name'] = ('{}-bucket'.format(hdinsight_conf['cluster_name'])).lower()
        hdinsight_conf['key_path'] = os.environ['conf_key_dir'] + '/' + os.environ['conf_key_name'] + '.pem'
        hdinsight_conf['notebook_instance_name'] = os.environ['notebook_instance_name']
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary. Exception:" + str(err))
        sys.exit(1)

    try:
        logging.info('[TERMINATE HDINSIGHT CLUSTER AND ASSOCIATED RESOURCES]')
        try:
            for cluster in AzureMeta.list_hdinsight_clusters(hdinsight_conf['resource_group_name']):
                if hdinsight_conf['cluster_name'] == cluster.tags["Name"]:
                    hdinsight_conf['full_cluster_name'] = cluster.name
            cluster = AzureMeta.get_hdinsight_cluster(hdinsight_conf['resource_group_name'],
                                                      hdinsight_conf['full_cluster_name'])
            if cluster and cluster.properties.cluster_state == 'Running':
                AzureActions.terminate_hdinsight_cluster(hdinsight_conf['resource_group_name'],
                                                         hdinsight_conf['full_cluster_name'])
            for storage_account in AzureMeta.list_storage_accounts(hdinsight_conf['resource_group_name']):
                if hdinsight_conf['storage_account_name_tag'] == storage_account.tags["Name"]:
                    AzureActions.remove_storage_account(hdinsight_conf['resource_group_name'], storage_account.name)
        except Exception as err:
            traceback.print_exc()
            datalab.fab.append_result("Failed to terminate hdinsight cluster.", str(err))
            raise Exception
    except:
        sys.exit(1)

    logging.info("[REMOVING NOTEBOOK KERNELS]")
    try:
        AzureActions.remove_dataengine_kernels(hdinsight_conf['resource_group_name'],
                                               hdinsight_conf['notebook_instance_name'], os.environ['conf_os_user'],
                                               hdinsight_conf['key_path'], hdinsight_conf['cluster_name'])
    except Exception as err:
        datalab.fab.append_result("Failed to remove dataengine kernels from notebook", str(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"dataengine-service_name": hdinsight_conf['computational_name'],
                   "notebook_name": hdinsight_conf['notebook_instance_name'],
                   "Action": "Terminate HDInsight cluster"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
