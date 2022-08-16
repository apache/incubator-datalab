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

if __name__ == "__main__":
    try:
        data_engine['service_base_name'] = os.environ['conf_service_base_name']
        data_engine['resource_group_name'] = os.environ['azure_resource_group_name']
        data_engine['region'] = os.environ['azure_region']
        data_engine['key_name'] = os.environ['conf_key_name']
        data_engine['vpc_name'] = os.environ['azure_vpc_name']
        data_engine['user_name'] = os.environ['edge_user_name']
        data_engine['project_name'] = os.environ['project_name']
        data_engine['project_tag'] = data_engine['project_name']
        data_engine['endpoint_name'] = os.environ['endpoint_name']
        data_engine['endpoint_tag'] = data_engine['endpoint_name']
        data_engine['master_node_name'] = '{}-m'.format(data_engine['cluster_name'])
        data_engine['key_name'] = os.environ['conf_key_name']
        if 'computational_name' in os.environ:
            data_engine['computational_name'] = os.environ['computational_name']
        else:
            data_engine['computational_name'] = ''
        data_engine['cluster_name'] = '{}-{}-{}-des-{}'.format(data_engine['service_base_name'],
                                                              data_engine['project_name'],
                                                              data_engine['endpoint_name'],
                                                              data_engine['computational_name'])
        with open("/root/result.json", 'w') as result:
            res = {"hostname": data_engine['cluster_name'],
                   "instance_id": data_engine['master_node_name'],
                   "key_name": data_engine['key_name'],
                   "Action": "Create new HDInsight cluster",
                   "computational_url": [
                       {"description": "HDInsight cluster",
                        "url": "spark_master_access_url"}
                       # {"description": "Apache Spark Master (via tunnel)",
                       # "url": spark_master_url}
                   ]
                   }
            result.write(json.dumps(res))
    except:
        pass