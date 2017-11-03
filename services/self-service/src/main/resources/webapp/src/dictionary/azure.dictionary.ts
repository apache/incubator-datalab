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
    'cloud_provider': 'azure',
    'notebook_instance_size': 'Virtual machine size',
    'personal_storage': 'Personal storage',
    'collaboration_storage': 'Collaboration storage',
    'account': 'Account:',
    'container': 'Container:',
    'data_engine': 'Deploy Spark Server / Deploy HDInsight',
    
    'data_engine_master_instance_size': 'Head node size',
    'data_engine_slave_instance_size': 'Worker node size',
    'master_node_shape': 'azure_dataengine_master_size',
    'slave_node_shape': 'azure_dataengine_slave_size',
    'total_instance_number': 'dataengine_instance_count',

    'spot_instance': 'Low-priority virtual machines',
    'billing': {
        'resourceName': 'resourceName',
        'cost': 'costString',
        'currencyCode': 'currencyCode',
        'dateFrom': 'from',
        'dateTo': 'to',
        'service': 'meterCategory',
        'type': '',
        'resourceType': 'resourceType',
        'instance_size': 'size'
    },
    'service': 'Category',
    'type': '',
    'instance_size': 'Size',
    'computational_resource': 'Computational resources',
    'user_storage_account_name': 'user_storage_account_name',
    'shared_storage_account_name': 'shared_storage_account_name',
    'bucket_name': 'user_container_name',
    'shared_bucket_name': 'shared_container_name',
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
        'data_engine_master_instance_size': 'Head node size',
        'data_engine_slave_instance_size': 'Worker node size',
        'instance_number': 'Total node number',
        'master_node_shape': 'dataengine_master',
        'slave_node_shape': 'dataengine_slave',
        'total_instance_number': 'dataengine_instance_count'
    }
};