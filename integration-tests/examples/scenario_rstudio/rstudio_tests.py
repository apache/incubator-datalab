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
from fabric import *
import argparse


parser = argparse.ArgumentParser()
parser.add_argument('--storage', type=str, default='')
parser.add_argument('--cloud', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--azure_storage_account', type=str, default='')
parser.add_argument('--azure_datalake_account', type=str, default='')
args = parser.parse_args()


def prepare_templates():
    subprocess.run('mv /tmp/rstudio /home/{0}/test_templates'.format(args.os_user), shell=True, check=True)

def get_storage():
    storages = {"aws": args.storage,
                "azure": "{0}@{1}.blob.core.windows.net".format(args.storage, args.azure_storage_account),
                "gcp": args.storage}
    protocols = {"aws": "s3a", "azure": "wasbs", "gcp": "gs"}
    if args.azure_datalake_account:
        storages['azure'] = "{0}.azuredatalakestore.net/{1}".format(args.azure_datalake_account, args.storage)
        protocols['azure'] = 'adl'
    return (storages[args.cloud], protocols[args.cloud])

def prepare_rscript(template_path, rscript_name, kernel='remote'):
    with open(template_path, 'r') as f:
        text = f.read()
    text = text.replace('WORKING_STORAGE', get_storage()[0])
    text = text.replace('PROTOCOL_NAME', get_storage()[1])
    if kernel == 'remote':
        if '-de-' in args.cluster_name:
            text = text.replace('MASTER', 'master')
        elif '-des-' in args.cluster_name:
            text = text.replace('MASTER', 'master = "yarn"')
    elif kernel == 'local':
        text = text.replace('MASTER', 'master = "local[*]"')
    with open('/home/{}/{}.r'.format(args.os_user, rscript_name), 'w') as f:
        f.write(text)

def enable_local_kernel():
    subprocess.run("sed -i 's/^master/#master/' /home/{0}/.Rprofile".format(args.os_user), shell=True, check=True)
    subprocess.run('''sed -i "s/^/#/g" /home/{0}/.Renviron'''.format(args.os_user), shell=True, check=True)
    subprocess.run('''sed -i "/\/opt\/spark\//s/#//g" /home/{0}/.Renviron'''.format(args.os_user), shell=True, check=True)
    subprocess.run('rm -f metastore_db/db* derby.log', shell=True, check=True)

def run_rscript(rscript_name):
    subprocess.run('R < /home/{0}/{1}.r --no-save'.format(args.os_user, rscript_name), shell=True, check=True)


if __name__ == "__main__":
    try:
        prepare_templates()
        # Running on remote kernel
        prepare_rscript('/home/{}/test_templates/template_preparation.r'.format(args.os_user), 'preparation', 'remote')
        run_rscript('preparation')
        prepare_rscript('/home/{}/test_templates/template_visualization.r'.format(args.os_user), 'visualization', 'remote')
        run_rscript('visualization')
        # Running on local kernel
        enable_local_kernel()
        prepare_rscript('/home/{}/test_templates/template_preparation.r'.format(args.os_user), 'preparation', 'local')
        prepare_rscript('/home/{}/test_templates/template_visualization.r'.format(args.os_user), 'visualization', 'local')
        run_rscript('preparation')
        run_rscript('visualization')
    except Exception as err:
        print('Error!', str(err))
        sys.exit(1)

    sys.exit(0)
