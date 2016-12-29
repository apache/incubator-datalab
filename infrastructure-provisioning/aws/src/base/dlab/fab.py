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
from fabric.contrib.files import exists
import logging
import os


class RoutineException(Exception):
    pass


def ensure_apt(requisites):
    try:
        if not exists('/tmp/apt_upgraded'):
            sudo('apt-get update')
            sudo('apt-get -y upgrade')
            sudo('export LC_ALL=C')
            sudo('touch /tmp/apt_upgraded')
        sudo('apt-get -y install ' + requisites)
        return True
    except:
        return False


def ensure_pip(requisites):
    try:
        if not exists('/tmp/pip_path_added'):
            sudo('echo PATH=$PATH:/usr/local/bin/:/opt/spark/bin/ >> /etc/profile')
            sudo('echo export PATH >> /etc/profile')
            sudo('touch /tmp/pip_path_added')
            sudo('pip install -U pip --no-cache-dir')
        sudo('pip install -U ' + requisites + ' --no-cache-dir')
        return True
    except:
        return False


def run_routine(routine_name, params, resource='default'):
    success = False
    local_log_filename = "{}_{}.log".format(os.environ['resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['resource'] +  "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.INFO,
                        filename=local_log_filepath)
    try:
        with settings(abort_exception=RoutineException):
            logging.info("~/scripts/%s.py %s" % (routine_name, params))
            local("~/scripts/%s.py %s" % (routine_name, params))
            success = True
    except RoutineException:
        success = False
    return success


def create_aws_config_files(generate_full_config=False):
    try:
        aws_user_dir = os.environ['AWS_DIR']
        logging.info(local("rm -rf " + aws_user_dir+" 2>&1", capture=True))
        logging.info(local("mkdir -p " + aws_user_dir+" 2>&1", capture=True))

        with open(aws_user_dir + '/config', 'w') as aws_file:
            aws_file.write("[default]\n")
            aws_file.write("region = %s\n" % os.environ['creds_region'])

        if generate_full_config:
            with open(aws_user_dir + '/credentials', 'w') as aws_file:
                aws_file.write("[default]\n")
                aws_file.write("aws_access_key_id = %s\n" % os.environ['creds_access_key'])
                aws_file.write("aws_secret_access_key = %s\n" % os.environ['creds_secret_access_key'])

        logging.info(local("chmod 600 " + aws_user_dir + "/*"+" 2>&1", capture=True))
        logging.info(local("chmod 550 " + aws_user_dir+" 2>&1", capture=True))

        return True
    except:
        return False
