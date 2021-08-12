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

export const NAMING_CONVENTION_GCP = {
    'cloud_provider': 'gcp',
    'personal_storage': 'Project bucket',
    'account': '',
    'container': '',
    'image': 'Not available',
    'data_engine_master_instance_size': 'Master machine type',
    'data_engine_slave_instance_size': 'Slave machine type',
    'master_node_shape': 'master_node_shape',
    'slave_node_shape': 'slave_node_shape',
    'total_instance_number': 'total_instance_number',
    'spot_instance': 'Preemptible worker nodes',
    'cluster_version': 'dataproc_version',
    'max_cluster_name_length': 7,
    'billing': {
        'resourceName': 'resource_name',
        'cost': 'cost',
        'costTotal': 'cost_total',
        'currencyCode': 'currency_code',
        'dateFrom': 'from',
        'dateTo': 'to',
        'service': 'product',
        'service_filter_key': 'product',
        'type': 'data_lab_resource_type',
        'resourceType': 'data_lab_resource_type',
        'instance_size': 'shapes',
        'datalabId': 'datalabId'
    },
    'service': 'Product',
    'type': 'Resource',
    'instance_size': 'Type',
    'user_storage_account_name': '',
    'shared_storage_account_name': '',
    'bucket_name': 'user_own_bucket_name',
    'shared_bucket_name': 'shared_bucket_name',
    'datalake_name': '',
    'datalake_user_directory_name': '',
    'datalake_shared_directory_name': '',
    'docker.datalab-dataengine-service': {
        'total_instance_number_min': 'min_instance_count',
        'total_instance_number_max': 'max_instance_count',
        'min_emr_spot_instance_bid_pct': 'min_emr_spot_instance_bid_pct',
        'max_emr_spot_instance_bid_pct': 'max_emr_spot_instance_bid_pct',
        'master_instance_number': 'Master node count',
        'slave_instance_number': 'Worker node count',
        'master_node_shape': 'master_node_shape',
        'slave_node_shape': 'slave_node_shape',
        'total_instance_number': 'total_master_instance_number',
        'total_slave_instance_number': 'total_slave_instance_number',
    },
    'docker.datalab-dataengine': {
        'total_instance_number_min': 'min_spark_instance_count',
        'total_instance_number_max': 'max_spark_instance_count',
        'data_engine_master_instance_size': 'Machine type',
        'master_instance_number': 'Master machine number',
        'master_node_shape': 'master_node_shape',
        'slave_node_shape': 'slave_node_shape',
        'total_instance_number': 'dataengine_instance_count',
    },
};


