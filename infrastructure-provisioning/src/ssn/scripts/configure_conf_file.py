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


import configparser
import argparse
import json
import sys
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--datalab_dir', type=str, default='')
parser.add_argument('--variables_list', type=str, default='')
args = parser.parse_args()


def modify_conf_file():
    try:
        variables_list = json.loads(args.variables_list)
        conf_list = []
        conf_file = open('{}sources/infrastructure-provisioning/src/general/conf/datalab.ini'.format(args.datalab_dir),
                         'r')
        for line in conf_file:
            conf_list.append(line)

        for line in conf_list:
            if line[0:2] == '# ':
                conf_list[conf_list.index(line)] = line.replace('# ', '')

        with open('/tmp/datalab.ini.modified', 'w') as conf_file_modified:
            conf_file_modified.writelines(conf_list)

        config = configparser.RawConfigParser()
        config.read('/tmp/datalab.ini.modified')
        for section in config.sections():
            options = config.options(section)
            for option in options:
                try:
                    print('Trying to put variable {}_{} to conf file'.format(section, option))
                    config.set(section, option, variables_list['{}_{}'.format(section, option)])
                except:
                    print('Such variable doesn`t exist!')
                    config.remove_option(section, option)

        with open('{}sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(args.datalab_dir),
                  'w') as conf_file_final:
            config.write(conf_file_final)
    except Exception as error:
        print('Error with modifying conf files:')
        print(str(error))
        sys.exit(1)


if __name__ == "__main__":
    try:
        modify_conf_file()
    except:
        sys.exit(1)
