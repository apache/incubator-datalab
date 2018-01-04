#!/usr/bin/python

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

import sys
from fabric.api import *
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--storage', type=str, default='')
parser.add_argument('--cloud', type=str, default='')
parser.add_argument('--azure_datalake_enable', type=str, default='false')
args = parser.parse_args()

dataset_file = ['airports.csv', 'carriers.csv', '2008.csv.bz2']

def download_dataset():
    local('wget http://stat-computing.org/dataexpo/2009/{0} -O /tmp/{0}'.format(dataset_file[0]))
    local('wget http://stat-computing.org/dataexpo/2009/{0} -O /tmp/{0}'.format(dataset_file[1]))
    local('wget http://stat-computing.org/dataexpo/2009/{0} -O /tmp/{0}'.format(dataset_file[2]))

def upload_aws():
    local('aws s3 cp /tmp/{0} s3://{1}/ --sse AES256'.format(dataset_file[0], args.storage))
    local('aws s3 cp /tmp/{0} s3://{1}/ --sse AES256'.format(dataset_file[1], args.storage))
    local('aws s3 cp /tmp/{0} s3://{1}/ --sse AES256'.format(dataset_file[2], args.storage))

def upload_azure(protocol):
    pass

def upload_gcp():
    local('gsutil -m cp /tmp/{0} gs://{1}/'.format(dataset_file[0], args.storage))
    local('gsutil -m cp /tmp/{0} gs://{1}/'.format(dataset_file[1], args.storage))
    local('gsutil -m cp /tmp/{0} gs://{1}/'.format(dataset_file[2], args.storage))


if __name__ == "__main__":
    download_dataset()
    if args.cloud == 'aws':
        upload_aws()
    elif args.cloud == 'azure':
        if args.azure_datalake_enable == 'true':
            upload_azure('adl')
        else:
            upload_azure('wasbs')
    elif args.cloud == 'gcp':
        upload_gcp()
    else:
        print('Error! Unknown cloud provider.')
        sys.exit(1)

    sys.exit(0)
