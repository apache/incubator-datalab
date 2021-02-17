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

import os, sys, json
import argparse
import subprocess
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--storage', type=str, default='S3/GCP buckets, Azure Blob container / Datalake folder')
parser.add_argument('--notebook', type=str, default='aws, azure, gcp')
parser.add_argument('--cloud', type=str, default='aws, azure, gcp')
parser.add_argument('--azure_storage_account', type=str, default='')
parser.add_argument('--azure_datalake_account', type=str, default='')
args = parser.parse_args()

dataset_file = ['airports.csv', 'carriers.csv', '2008.csv.bz2']

def download_dataset():
    try:
        for f in dataset_file:
            subprocess.run('wget http://stat-computing.org/dataexpo/2009/{0} -O /tmp/{0}'.format(f), shell=True, check=True)
    except Exception as err:
        print('Failed to download test dataset', str(err))
        sys.exit(1)

def upload_aws():
    try:
        for f in dataset_file:
            subprocess.run('aws s3 cp /tmp/{0} s3://{1}/{2}_dataset/ --sse AES256'.format(f, args.storage, args.notebook), shell=True, check=True)
    except Exception as err:
        print('Failed to upload test dataset to bucket', str(err))
        sys.exit(1)

def upload_azure_datalake():
    try:
        from azure.datalake.store import core, lib, multithread
        sp_creds = json.loads(open(os.environ['AZURE_AUTH_LOCATION']).read())
        dl_filesystem_creds = lib.auth(tenant_id=json.dumps(sp_creds['tenantId']).replace('"', ''),
                                       client_secret=json.dumps(sp_creds['clientSecret']).replace('"', ''),
                                       client_id=json.dumps(sp_creds['clientId']).replace('"', ''),
                                       resource='https://datalake.azure.net/')
        datalake_client = core.AzureDLFileSystem(dl_filesystem_creds, store_name=args.azure_datalake_account)
        for f in dataset_file:
            multithread.ADLUploader(datalake_client,
                                    lpath='/tmp/{0}'.format(f),
                                    rpath='{0}/{1}_dataset/{2}'.format(args.storage, args.notebook, f))
    except Exception as err:
        print('Failed to upload test dataset to datalake store', str(err))
        sys.exit(1)

def upload_azure_blob():
    try:
        from azure.mgmt.storage import StorageManagementClient
        from azure.storage.blob import BlockBlobService
        from azure.common.client_factory import get_client_from_auth_file
        storage_client = get_client_from_auth_file(StorageManagementClient)
        resource_group_name = ''
        for i in storage_client.storage_accounts.list():
            if args.storage.replace('container', 'storage') == str(i.tags.get('Name')):
                resource_group_name = str(i.tags.get('SBN'))
        secret_key = storage_client.storage_accounts.list_keys(resource_group_name, args.azure_storage_account).keys[0].value
        block_blob_service = BlockBlobService(account_name=args.azure_storage_account, account_key=secret_key)
        for f in dataset_file:
            block_blob_service.create_blob_from_path(args.storage, '{0}_dataset/{1}'.format(args.notebook, f), '/tmp/{0}'.format(f))
    except Exception as err:
        print('Failed to upload test dataset to blob storage', str(err))
        sys.exit(1)

def upload_gcp():
    try:
        for f in dataset_file:
            subprocess.run('sudo gsutil -m cp /tmp/{0} gs://{1}/{2}_dataset/'.format(f, args.storage, args.notebook), shell=True, check=True)
    except Exception as err:
        print('Failed to upload test dataset to bucket', str(err))
        sys.exit(1)

if __name__ == "__main__":
    download_dataset()
    if args.cloud == 'aws':
        upload_aws()
    elif args.cloud == 'azure':
        os.environ['AZURE_AUTH_LOCATION'] = '/home/datalab-user/keys/azure_auth.json'
        if args.azure_datalake_account:
            upload_azure_datalake()
        else:
            upload_azure_blob()
    elif args.cloud == 'gcp':
        upload_gcp()
    else:
        print('Error! Unknown cloud provider.')
        sys.exit(1)

    sys.exit(0)
