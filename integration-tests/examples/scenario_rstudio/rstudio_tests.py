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

from fabric.api import *
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()


def prepare_templates():
    local('aws s3 cp --recursive s3://' + args.bucket + '/test_templates_rstudio/ /home/{}/test_rstudio/'.
          format(args.os_user))


def prepare_rscript(template_path, rscript_name):
    with open(template_path, 'r') as f:
        text = f.read()
    text = text.replace('S3_BUCKET', args.bucket)
    text = text.replace('MASTER', 'yarn')
    with open('/home/{}/{}.r'.format(args.os_user, rscript_name), 'w') as f:
        f.write(text)


def enable_local_kernel():
    local('sed -i "s/^#//g" /home/{0}/.Renviron | sed -i "/emr/s/^/#/g" /home/{0}/.Renviron'.format(args.os_user))
    local('rm -f metastore_db/db*')


def enable_local_kernel_in_template(template_path, rscript_name):
    with open(template_path, 'r') as f:
        text = f.read()
    text = text.replace('S3_BUCKET', args.bucket)
    text = text.replace('MASTER', 'local[*]')
    with open('/home/{}/{}.r'.format(args.os_user, rscript_name), 'w') as f:
        f.write(text)


def enable_remote_kernel():
    local('sed -i "s/^#//g" /home/{0}/.Renviron | sed -i "/\/opt\/spark\//s/^/#/g" /home/{0}/.Renviron'.
          format(args.os_user))


def run_rscript(rscript_name):
    local('R < ' + rscript_name + '.r --no-save')


prepare_templates()
# Running on remote kernel
prepare_rscript('/home/{}/test_rstudio/template_preparation.r'.format(args.os_user), 'preparation')
run_rscript('preparation')
prepare_rscript('/home/{}/test_rstudio/template_visualization.r'.format(args.os_user), 'visualization')
run_rscript('visualization')
# Running on local kernel
enable_local_kernel()
enable_local_kernel_in_template('/home/{}/test_rstudio/template_preparation.r'.format(args.os_user), 'preparation')
enable_local_kernel_in_template('/home/{}/test_rstudio/template_visualization.r'.format(args.os_user), 'visualization')
run_rscript('preparation')
run_rscript('visualization')

