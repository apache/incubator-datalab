/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

export const NAMING_CONVENTION = {
    'cloud_provider': 'aws',
    'notebook_instance_size': 'Instance shape',
    'personal_storage': 'Data bucket',
    'collaboration_storage': 'Collaboration bucket',
    'account': '',
    'container': '',
    'data_engine': 'Deploy Spark Server / Deploy EMR',

    'data_engine_master_instance_size': 'Master instance shape',
    'data_engine_slave_instance_size': 'Slave instance shape',
    'master_node_shape': 'master_node_shape',
    'slave_node_shape': 'slave_node_shape',
    'total_instance_number': 'total_instance_number',

    'spot_instance': 'Spot instance ',
    'billing': {
        'resourceName': 'resource_name',
        'cost': 'cost',
        'currencyCode': 'currency_code',
        'dateFrom': 'usage_date_start',
        'dateTo': 'usage_date_end',
        'service': 'product',
        'type': 'resource_type',
        'resourceType': 'dlab_resource_type',
        'instance_size': 'shape'
    },
    'service': 'Service',
    'type': 'Type',
    'instance_size': 'Shape',
    'computational_resource': 'Computational resources',
    'user_storage_account_name': '',
    'shared_storage_account_name': '',
    'bucket_name': 'user_own_bicket_name',
    'shared_bucket_name': 'shared_bucket_name',
    'docker.dlab-dataengine-service': {
        'total_instance_number_min': 'min_emr_instance_count',
        'total_instance_number_max': 'max_emr_instance_count',
        'min_emr_spot_instance_bid_pct': 'min_emr_spot_instance_bid_pct',
        'max_emr_spot_instance_bid_pct': 'max_emr_spot_instance_bid_pct',
        'data_engine_master_instance_size': 'Master instance shape',
        'data_engine_slave_instance_size': 'Slave instance shape',
        'instance_number': 'Total instance number',
        'master_node_shape': 'master_node_shape',
        'slave_node_shape': 'slave_node_shape',
        'total_instance_number': 'total_instance_number',
    },
    'docker.dlab-dataengine': {
        'total_instance_number_min': 'min_spark_instance_count',
        'total_instance_number_max': 'max_spark_instance_count',
        'data_engine_master_instance_size': 'Master node shape',
        'data_engine_slave_instance_size': 'Slave node shape',
        'instance_number': 'Total node number',
        'master_node_shape': 'dataengine_master',
        'slave_node_shape': 'dataengine_slave',
        'total_instance_number': 'dataengine_instance_count',
    }
};