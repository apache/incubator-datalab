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

from pprint import pprint
from googleapiclient.discovery import build
from oauth2client.client import GoogleCredentials
from oauth2client.service_account import ServiceAccountCredentials
from google.cloud import storage
import os


class GCPActions:
    def __init__(self, auth_type='service_account'):

        self.auth_type = auth_type
        self.project = os.environ['gcp_project_id']
        if os.environ['conf_resource'] == 'ssn':
            self.key_file = '/root/service_account.json'
            print('ServiceAccountCredentials')
            credentials = ServiceAccountCredentials.from_json_keyfile_name(
                self.key_file, scopes='https://www.googleapis.com/auth/compute')
            self.service = build('compute', 'v1', credentials=credentials)
        else:
            self.service = build('compute', 'v1')

    def create_vpc(self, network_name):
        network_params = {'name': network_name, 'autoCreateSubnetworks': False}
        request = self.service.networks().insert(project=self.project, body=network_params)
        return request.execute()

    def create_subnet(self, subnet_name, ip_cidr_range, network_name, region):
        subnetwork_params = {
                'name': subnet_name,
                'ipCidrRange': ip_cidr_range,
                'network': network_name

        }
        request = self.service.subnetworks().insert(
                project=self.project, region=region, body=subnetwork_params)
        return request.execute()

    def firewall_add(self, firewall_params):
        request = self.service.firewalls().insert(
            project=self.project, body=firewall_params)
        return request.execute()

    def create_instance(self, instance_params):
        request = self.service.instances().insert(
            project=self.project, zone=os.environ['zone'], body=instance_params)
        return request.execute()

    def create_bucket(self, bucket_name):
        if os.environ['conf_resource'] == 'ssn':
            self.key_file = '/root/service_account.json'
            print('ServiceAccountCredentials')
            credentials = ServiceAccountCredentials.from_json_keyfile_name(
                self.key_file, scopes='https://www.googleapis.com/auth/devstorage.full_control')
            self.service = build('storage', 'v1', credentials=credentials)
        storage_client = storage.Client()
        bucket = storage_client.create_bucket(bucket_name)
        print('Bucket {} created.'.format(bucket.name))