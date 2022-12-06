/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

export const NAMING_CONVENTION_AZURE = {
    'cloud_provider': 'azure',
    'personal_storage': 'Project bucket',
    'collaboration_storage': 'Shared endpoint bucket',
    'account': 'Account:',
    'container': 'Container:',
    'image': 'image',
    'data_engine_master_instance_size': 'Head node size',
    'data_engine_slave_instance_size': 'Worker node size',
    'master_node_shape': 'azure_dataengine_master_size',
    'slave_node_shape': 'azure_dataengine_slave_size',
    'total_instance_number': 'dataengine_instance_count',
    'spot_instance': 'Low-priority virtual machines',
    'cluster_version': 'hdinsight_version',
    'max_cluster_name_length': 10,
    'billing': {
        'resourceName': 'resourceName',
        'cost': 'cost',
        'costTotal': 'cost_total',
        'currencyCode': 'currencyCode',
        'dateFrom': 'from',
        'dateTo': 'to',
        'service': 'meterCategory',
        'service_filter_key': 'meterCategory',
        'type': '',
        'resourceType': 'resource_type',
        'instance_size': 'size',
        'datalabId': 'datalabId'
    },
    'service': 'Category',
    'type': '',
    'instance_size': 'Size',
    'user_storage_account_name': 'user_storage_account_name',
    'shared_storage_account_name': 'shared_storage_account_name',
    'bucket_name': 'user_container_name',
    'shared_bucket_name': 'shared_container_name',
    'datalake_name': 'datalake_name',
    'datalake_user_directory_name': 'datalake_user_directory_name',
    'datalake_shared_directory_name': 'datalake_shared_directory_name',
    'docker.datalab-dataengine-service': {
        'total_instance_number_min': 'min_hdinsight_instance_count',
        'total_instance_number_max': 'max_hdinsight_instance_count',
        'master_node_shape': 'master_node_shape',
        'slave_node_shape': 'slave_node_shape',
        'total_instance_number': 'total_instance_number',
    },
    'docker.datalab-dataengine': {
        'total_instance_number_min': 'min_spark_instance_count',
        'total_instance_number_max': 'max_spark_instance_count',
        'data_engine_master_instance_size': 'Node size',
        'master_node_shape': 'master_node_shape',
        'slave_node_shape': 'slave_node_shape',
        'total_instance_number': 'dataengine_instance_count'
    },
};

