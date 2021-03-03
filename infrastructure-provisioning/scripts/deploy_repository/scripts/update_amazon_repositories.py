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


from fabric import *
import argparse
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--region', required=True, type=str, default='', help='AWS region name')
args = parser.parse_args()


if __name__ == "__main__":
    nexus_password = 'NEXUS_PASSWORD'
    subprocess.run('wget http://repo.{}.amazonaws.com/2017.09/main/mirror.list -O /tmp/main_mirror.list'.format(args.region), shell=True, check=True)
    subprocess.run('wget http://repo.{}.amazonaws.com/2017.09/updates/mirror.list -O /tmp/updates_mirror.list'.format(
        args.region), shell=True, check=True)
    amazon_main_repo = subprocess.run("cat /tmp/main_mirror.list  | grep {} | sed 's/$basearch//g'".format(args.region),
                             capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
    amazon_updates_repo = subprocess.run("cat /tmp/updates_mirror.list  | grep {} | sed 's/$basearch//g'".format(args.region),
                                capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
    subprocess.run('cp -f /opt/nexus/updateRepositories.groovy /tmp/updateRepositories.groovy', shell=True, check=True)
    subprocess.run('sed -i "s|AMAZON_MAIN_URL|{}|g" /tmp/updateRepositories.groovy'.format(amazon_main_repo), shell=True, check=True)
    subprocess.run('sed -i "s|AMAZON_UPDATES_URL|{}|g" /tmp/updateRepositories.groovy'.format(amazon_updates_repo), shell=True, check=True)
    subprocess.run('/usr/local/groovy/latest/bin/groovy /tmp/addUpdateScript.groovy -u "admin" -p "{}" '
          '-n "updateRepositories" -f "/tmp/updateRepositories.groovy" -h "http://localhost:8081"'.format(
           nexus_password), shell=True, check=True)
    subprocess.run('curl -u admin:{} -X POST --header \'Content-Type: text/plain\' '
          'http://localhost:8081/service/rest/v1/script/updateRepositories/run'.format(nexus_password), shell=True, check=True)
    subprocess.run('rm -f /tmp/main_mirror.list', shell=True, check=True)
    subprocess.run('rm -f /tmp/updates_mirror.list', shell=True, check=True)
    subprocess.run('rm -f /tmp/updateRepositories.groovy', shell=True, check=True)
    print('Amazon repositories have been successfully updated!')
