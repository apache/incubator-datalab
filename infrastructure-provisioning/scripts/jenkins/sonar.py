#!/usr/bin/python
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
import requests

PROJECT_KEY = 'com.epam.dlab%3Adlab'
TOKEN = 'ca50bcc08f84b90c0324d2e04dcee5f9c5034e67'

response = requests.get('https://localhost:9000/sonar/api/qualitygates/project_status?projectKey=' + PROJECT_KEY,
                        auth=(TOKEN, '')).json()

print('SUCCESS', response['projectStatus']['status'])
