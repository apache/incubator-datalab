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

import json
import time
from fabric.api import *
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import sys
import os
import logging
import argparse
import multiprocessing


parser = argparse.ArgumentParser()
parser.add_argument('--uuid', type=str, default='')
args = parser.parse_args()


def configure_dataengine_service(instance, dataproc_conf):
#     emr_conf['instance_ip'] = instance.get('PrivateIpAddress')
    # configuring proxy on Data Engine service
    try:
        logging.info('[CONFIGURE PROXY ON DATAENGINE SERVICE]')
#         print('[CONFIGURE PROXY ON DATAENGINE SERVICE]')
#         additional_config = {"proxy_host": emr_conf['edge_instance_hostname'], "proxy_port": "3128"}
#         params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}" \
#             .format(emr_conf['instance_ip'], emr_conf['cluster_name'], emr_conf['key_path'],
#                     json.dumps(additional_config), emr_conf['os_user'])
#         try:
#             local("~/scripts/{}.py {}".format('common_configure_proxy', params))
#         except:
#             traceback.print_exc()
#             raise Exception
#     except Exception as err:
#         append_result("Failed to configure proxy.", str(err))
#         terminate_emr(emr_conf['cluster_id'])
#         sys.exit(1)
#
#     try:
#         logging.info('[CONFIGURE DATAENGINE SERVICE]')
#         print('[CONFIGURE DATAENGINE SERVICE]')
#         try:
#             configure_data_engine_service_pip(emr_conf['instance_ip'], emr_conf['os_user'], emr_conf['key_path'])
#         except:
#             traceback.print_exc()
#             raise Exception
    except Exception as err:
        append_result("Failed to configure dataengine service.", str(err))
        terminate_emr(dataproc_conf['cluster_id'])
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.INFO,
                        filename=local_log_filepath)
    print('Generating infrastructure names and tags')
    dataproc_conf = dict()
    dataproc_conf['uuid'] = str(uuid.uuid4())[:5]
    try:
        dataproc_conf['exploratory_name'] = (os.environ['exploratory_name']).lower().replace('_', '-')
    except:
        dataproc_conf['exploratory_name'] = ''
    try:
        dataproc_conf['computational_name'] = (os.environ['computational_name']).lower().replace('_', '-')
    except:
        dataproc_conf['computational_name'] = ''
    dataproc_conf['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    dataproc_conf['edge_user_name'] = (os.environ['edge_user_name']).lower().replace('_', '-')
    dataproc_conf['key_name'] = os.environ['conf_key_name']
    dataproc_conf['key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    dataproc_conf['region'] = os.environ['gcp_region']
    dataproc_conf['zone'] = os.environ['gcp_zone']
    dataproc_conf['subnet'] = '{0}-{1}-subnet'.format(dataproc_conf['service_base_name'], dataproc_conf['edge_user_name'])
    dataproc_conf['cluster_name'] = '{0}-{1}-dp-{2}-{3}-{4}'.format(dataproc_conf['service_base_name'], dataproc_conf['edge_user_name'],
                                                                    dataproc_conf['exploratory_name'], dataproc_conf['computational_name'], dataproc_conf['uuid'])
    dataproc_conf['cluster_tag'] = '{0}-{1}-dp'.format(dataproc_conf['service_base_name'], dataproc_conf['edge_user_name'])
    dataproc_conf['bucket_name'] = '{}-ssn-bucket'.format(dataproc_conf['service_base_name'])
    dataproc_conf['release_label'] = os.environ['dataproc_version']
    dataproc_conf['cluster_label'] = {os.environ['notebook_instance_name']: "not-configured"}
    dataproc_conf['dataproc_service_account_name'] = dataproc_conf['service_base_name'].lower().replace('_', '-') + \
                                                       "-" + os.environ['edge_user_name'] + '-nb-sa'
    service_account_email = "{}@{}.iam.gserviceaccount.com".format(dataproc_conf['dataproc_service_account_name'],
                                                                   os.environ['gcp_project_id'])

    dataproc_conf['cluster_instances'] = meta_lib.GCPMeta().get_list_instances(os.environ['gcp_zone'], dataproc_conf['cluster_name'])


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
    except:
        traceback.print_exc()
        raise Exception

    try:
        logging.info('[SUMMARY]')
        print('[SUMMARY]')
        print("Service base name: {}".format(dataproc_conf['service_base_name']))
        print("Cluster name: {}".format(dataproc_conf['cluster_name']))
        print("Key name: {}".format(dataproc_conf['key_name']))
        print("Region: {}".format(dataproc_conf['region']))
        print("Zone: {}".format(dataproc_conf['zone']))
        print("Subnet: {}".format(dataproc_conf['subnet']))
        print("Dataproc version: {}".format(dataproc_conf['release_label']))
        print("Dataproc master node shape: {}".format(os.environ['dataproc_master_instance_type']))
        print("Dataproc slave node shape: {}".format(os.environ['dataproc_slave_instance_type']))
        print("Master count: {}".format(os.environ['dataproc_master_count']))
        print("Slave count: {}".format(os.environ['dataproc_slave_count']))
        print("Preemptible count: {}".format(os.environ['dataproc_preemptible_count']))
        print("Notebook hostname: {}".format(os.environ['notebook_instance_name']))
        print("Bucket name: {}".format(dataproc_conf['bucket_name']))
        with open("/root/result.json", 'w') as result:
            res = {"hostname": dataproc_conf['cluster_name'],
                   "key_name": dataproc_conf['key_name'],
                   "user_own_bucket_name": dataproc_conf['bucket_name'],
                   "Action": "Create new Dataproc cluster"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(1)

    sys.exit(0)