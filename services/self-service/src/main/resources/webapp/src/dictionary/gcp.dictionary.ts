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
  'cloud_provider': 'gcp',
  'use_ldap': true,
  'notebook_instance_size': 'Instance type',
  'personal_storage': 'Data bucket',
  'collaboration_storage': 'Collaboration bucket',
  'account': '',
  'container': '',
  'data_engine': 'Deploy Spark Server / Deploy Dataproc',
  'image': 'Not available',
  'data_engine_master_instance_size': 'Master machine type',
  'data_engine_slave_instance_size': 'Slave machine type',

  'master_node_shape': 'master_node_shape',
  'slave_node_shape': 'slave_node_shape',
  'total_instance_number': 'total_instance_number',

  'spot_instance': 'Preemptible worker nodes',
  'billing': {
      'resourceName': 'resource_name',
      'cost': 'cost',
      'costTotal': 'cost_total',
      'currencyCode': 'currency_code',
      'dateFrom': 'usage_date_start',
      'dateTo': 'usage_date_end',
      'service': 'product',
      'service_filter_key': 'product',
      'type': 'resource_type',
      'resourceType': 'dlab_resource_type',
      'instance_size': 'shape',
      'dlabId': 'dlab_id'
  },
  'service': 'Product',
  'type': 'Resource',
  'instance_size': 'Type',
  'computational_resource': 'Computational resources',
  'user_storage_account_name': '',
  'shared_storage_account_name': '',
  'bucket_name': 'user_own_bucket_name',
  'shared_bucket_name': 'shared_bucket_name',
  'datalake_name': '',
  'datalake_user_directory_name': '',
  'datalake_shared_directory_name': '',
  'docker.dlab-dataengine-service': {
      'total_instance_number_min': 'min_instance_count',
      'total_instance_number_max': 'max_instance_count',
      'min_emr_spot_instance_bid_pct': 'min_emr_spot_instance_bid_pct',
      'max_emr_spot_instance_bid_pct': 'max_emr_spot_instance_bid_pct',
      'data_engine_master_instance_size': 'Master machine type',
      'data_engine_slave_instance_size': 'Slave machine type',
      'instance_number': 'Master node count',
      'master_instance_number': 'Master node count',
      'slave_instance_number': 'Worker node count',
      'master_node_shape': 'master_node_shape',
      'slave_node_shape': 'slave_node_shape',
      'total_instance_number': 'total_master_instance_number',
      'total_slave_instance_number': 'total_slave_instance_number',
  },
  'docker.dlab-dataengine': {
      'total_instance_number_min': 'min_spark_instance_count',
      'total_instance_number_max': 'max_spark_instance_count',
      'data_engine_master_instance_size': 'Machine type',
      'instance_number': 'Total machine number',
      'master_node_shape': 'dataengine_instance_shape',
      'total_instance_number': 'dataengine_instance_count',
  }
};

export class ReportingConfigModel {

  static getDefault(): ReportingConfigModel {
      return new ReportingConfigModel([], [], [], [], '', '', '');
  }

  constructor(
      public user: Array<string>,
      public product: Array<string>,
      public resource_type: Array<string>,
      public shape: Array<string>,
      public date_start: string,
      public date_end: string,
      public dlab_id: string
  ) { }

  defaultConfigurations(): void {
      this.user = [];
      this.product = [];
      this.resource_type = [];
      this.shape = [];
      this.date_start = '';
      this.date_end = '';
      this.dlab_id = '';
  }
}
