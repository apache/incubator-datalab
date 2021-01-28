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


import sys
import traceback
from fabric.api import *
from os.path import exists

src_path = '/opt/datalab/sources/infrastructure-provisioning/src/'
if sys.argv[1] == 'all':
    node = [
        'edge',
        'project',
        'jupyter',
        'jupyterlab',
        'rstudio',
        'zeppelin',
            'tensor',
            'tensor-rstudio',
            'deeplearning',
            'dataengine',
            'dataengine-service',
            'superset'
            ]
else:
    node = sys.argv[1:]

def image_build(src_path, node):
    try:
        if local("cat /etc/lsb-release | grep DISTRIB_ID | awk -F '=' '{print $2}'", capture=True).stdout == 'Ubuntu':
            os_family = 'debian'
        else:
            os_family = 'redhat'
        if local("uname -r | awk -F '-' '{print $3}'", capture=True).stdout == 'aws':
            cloud_provider = 'aws'
        elif local("uname -r | awk -F '-' '{print $3}'", capture=True).stdout == 'azure':
            cloud_provider = 'azure'
            if not exists('{}base/azure_auth.json'.format(src_path)):
                local('cp /home/datalab-user/keys/azure_auth.json {}base/azure_auth.json'.format(src_path))
        else:
            cloud_provider = 'gcp'
        with lcd(src_path):
            local(
                'docker build --build-arg OS={0} --build-arg SRC_PATH= --file general/files/{1}/base_Dockerfile -t docker.datalab-base:latest .'.format(
                    os_family, cloud_provider))
            try:
                for i in range(len(node)):
                    local(
                        'docker build --build-arg OS={0} --file general/files/{1}/{2}_Dockerfile -t docker.datalab-{2} .'.format(
                            os_family, cloud_provider, node[i]))
            except Exception as err:
                print("Failed to build {} image".format(node[i]), str(err))
                raise Exception
    except Exception as err:
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    image_build(src_path, node)