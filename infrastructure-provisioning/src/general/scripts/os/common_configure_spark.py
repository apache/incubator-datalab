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

import os
import sys
import time
import argparse
from dlab.fab import *
from fabric.api import *
from dlab.notebook_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--spark_conf', type=str, default='')
args = parser.parse_args()


def update_spark_defaults_conf(spark_conf):
    try:
        timestamp = time.strftime("%a, %d %b %Y %H:%M:%S %Z", time.gmtime())
        configs = sudo('find /opt/ /etc/ /usr/lib/ -name spark-defaults.conf -type f').split('\r\n')
        for conf in filter(None, configs):
            sudo('''sed -i '/^# Updated/d' {0}'''.format(conf))
            sudo('''echo "# Updated by DLab at {0} >> {1}'''.format(timestamp, conf))
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)


if __name__ == "__main__":
    print('Configure connections')
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname

    if (args.spark_conf != ''):
        update_spark_defaults_conf(args.spark_conf)

    update_spark_jars()

    sys.exit(0)