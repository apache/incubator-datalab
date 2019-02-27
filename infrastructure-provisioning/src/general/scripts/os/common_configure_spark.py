#!/usr/bin/python

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

import os
import sys
import time
import ast
import argparse
from dlab.fab import *
from fabric.api import *
from dlab.notebook_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--spark_conf', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
args = parser.parse_args()


def update_spark_defaults_conf(spark_conf):
    try:
        timestamp = time.strftime("%a, %d %b %Y %H:%M:%S %Z", time.gmtime())
        configs = sudo('find /opt/ /etc/ /usr/lib/ -name spark-defaults.conf -type f').split('\r\n')
        for conf in filter(None, configs):
            sudo('''sed -i '/^# Updated/d' {0}'''.format(conf))
            sudo('''echo "# Updated by DLab at {0} >> {1}'''.format(timestamp, conf))
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)


def add_custom_spark_properties(cluster_name):
    try:
        spark_configurations = ast.literal_eval(os.environ['spark_configurations'])
        new_spark_defaults = list()
        spark_defaults = sudo('cat /opt/{0}/spark/conf/spark-defaults.conf'.format(cluster_name))
        current_spark_properties = spark_defaults.split('\n')
        for param in current_spark_properties:
            for config in spark_configurations:
                if config['Classification'] == 'spark-defaults':
                    for property in config['Properties']:
                        if property == param.split(' ')[0]:
                            param = property + ' ' + config['Properties'][property]
                        else:
                            new_spark_defaults.append(property + ' ' + config['Properties'][property])
            new_spark_defaults.append(param)
        new_spark_defaults = set(new_spark_defaults)
        sudo('echo "" > /opt/{0}/spark/conf/spark-defaults.conf'.format(cluster_name))
        for prop in new_spark_defaults:
            prop = prop.rstrip()
            sudo('echo "{0}" >> /opt/{1}/spark/conf/spark-defaults.conf'.format(prop, cluster_name))
        sudo('sed -i "/^\s*$/d" /opt/{0}/spark/conf/spark-defaults.conf'.format(cluster_name))
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)


if __name__ == "__main__":
    print('Configure connections')
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname

    if (args.spark_conf != ''):
        update_spark_defaults_conf(args.spark_conf)

    update_spark_jars()

    if 'spark_configurations' in os.environ:
        add_custom_spark_properties(args.cluster_name)
