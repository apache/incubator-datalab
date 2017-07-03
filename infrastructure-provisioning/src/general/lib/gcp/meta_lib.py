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
import os


class GcpMeta:
    def __init__(self, auth_type='service_account'):

        self.auth_type = auth_type
        self.project = os.environ['gcp_project_id']
        if os.environ['conf_resource'] == 'ssn':
            self.key_file = '/root/service_account.json'
            print('ServiceAccountCredentials')
            credentials = ServiceAccountCredentials.from_json_keyfile_name(
                self.key_file, scopes='https://www.googleapis.com/auth/compute')
            self.service = build('compute', 'v1', credentials = credentials)
        else:
            self.service = build('compute', 'v1')

    def network_get(self, network_name):
        request = self.service.networks().get(
                    project=self.project, network=network_name)
        return request.execute()