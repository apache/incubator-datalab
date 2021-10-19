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

import argparse
import os
import sys
import subprocess
from datalab.actions_lib import *
from datalab.common_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric import *
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--emr_version', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--excluded_lines', type=str, default='')
parser.add_argument('--project_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()

emr_dir = '/opt/{}/jars/'.format(args.emr_version)
spark_dir = '/opt/{0}/{1}/spark/'.format(args.emr_version, args.cluster_name)
yarn_dir = '/opt/{0}/{1}/conf/'.format(args.emr_version, args.cluster_name)


def configure_rstudio():
    if not os.path.exists('/home/' + args.os_user + '/.ensure_dir/rstudio_dataengine-service_ensured'):
        try:
            subprocess.run('echo "export R_LIBS_USER=' + spark_dir + '/R/lib:" >> /home/' + args.os_user + '/.bashrc', shell=True, check=True)
            subprocess.run("sed -i 's/^SPARK_HOME/#SPARK_HOME/' /home/" + args.os_user + "/.Renviron", shell=True, check=True)
            subprocess.run('echo \'SPARK_HOME="' + spark_dir + '"\' >> /home/' + args.os_user + '/.Renviron', shell=True, check=True)
            subprocess.run('echo \'YARN_CONF_DIR="' + yarn_dir + '"\' >> /home/' + args.os_user + '/.Renviron', shell=True, check=True)
            subprocess.run('echo \'HADOOP_CONF_DIR="' + yarn_dir + '"\' >> /home/' + args.os_user + '/.Renviron', shell=True, check=True)
            subprocess.run("sed -i 's/^master/#master/' /home/" + args.os_user + "/.Rprofile", shell=True, check=True)
            subprocess.run('''R -e "source('/home/{}/.Rprofile')"'''.format(args.os_user), shell=True, check=True)
            #fix emr 5.19 problem with warnings in rstudio because of bug in AWS configuration
            if args.emr_version == "emr-5.19.0":
                subprocess.run("sed -i '/DRFA/s/^/#/' " + spark_dir + "conf/log4j.properties", shell=True, check=True)
            subprocess.run('touch /home/' + args.os_user + '/.ensure_dir/rstudio_dataengine-service_ensured', shell=True, check=True)
        except Exception as err:
            logging.error('Error: {0}'.format(err))
            sys.exit(1)
    else:
        try:
            subprocess.run("sed -i '/R_LIBS_USER/ { s|=\(.*\)|=\\1" + spark_dir + "/R/lib:| }' /home/" + args.os_user + "/.bashrc", shell=True, check=True)
            subprocess.run("sed -i 's/^SPARK_HOME/#SPARK_HOME/' /home/" + args.os_user + "/.Renviron", shell=True, check=True)
            subprocess.run("sed -i 's/^YARN_CONF_DIR/#YARN_CONF_DIR/' /home/" + args.os_user + "/.Renviron", shell=True, check=True)
            subprocess.run("sed -i 's/^HADOOP_CONF_DIR/#HADOOP_CONF_DIR/' /home/" + args.os_user + "/.Renviron", shell=True, check=True)
            subprocess.run("sed -i 's/^master/#master/' /home/" + args.os_user + "/.Rprofile", shell=True, check=True)
            subprocess.run('echo \'SPARK_HOME="' + spark_dir + '"\' >> /home/' + args.os_user + '/.Renviron', shell=True, check=True)
            subprocess.run('echo \'YARN_CONF_DIR="' + yarn_dir + '"\' >> /home/' + args.os_user + '/.Renviron', shell=True, check=True)
            subprocess.run('echo \'HADOOP_CONF_DIR="' + yarn_dir + '"\' >> /home/' + args.os_user + '/.Renviron', shell=True, check=True)
            subprocess.run('''R -e "source('/home/{}/.Rprofile')"'''.format(args.os_user), shell=True, check=True)
            #fix emr 5.19 problem with warnings in rstudio because of bug in AWS configuration
            if args.emr_version == "emr-5.19.0":
                subprocess.run("sed -i '/DRFA/s/^/#/' " + spark_dir + "conf/log4j.properties", shell=True, check=True)
        except Exception as err:
            logging.error('Error: {0}'.format(err))
            sys.exit(1)


if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        result = prepare(emr_dir, yarn_dir)
        if result == False :
            jars(args, emr_dir)
        yarn(args, yarn_dir)
        install_emr_spark(args)
        spark_defaults(args)
        configuring_notebook(args.emr_version)
        configure_rstudio()
