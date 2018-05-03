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

from dlab.fab import *
from fabric.api import *
from fabric.contrib.files import exists
import argparse
import sys
import os

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()


def exec_bash_script(os_user):
    list_script = os.listdir("/logs/notebook/rstudio_scripts")
    if list_script:
        script = list_script[0]
        if not exists('/home/{}/rstudio_script'.format(os_user)):
            with cd('/tmp'):
                run('mkdir rstudio_script')
                put('/logs/notebook/rstudio_scripts/{}'.format(script), 'rstudio_script/')
                run('cp -r rstudio_script/ /home/{}/'.format(os_user))
                run('rm -rf rstudio_script/')
            run('/bin/bash /home/{}/rstudio_script/{}'.format(os_user, script))
    else:
        print("There no bash script in the directory /logs/notebook/rstudio_script/")

##############
# Run script #
##############
if __name__ == "__main__":
    print("Configure connections")
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname

# POST INSTALLATION PROCESS
print('Executing bash script')
exec_bash_script(args.os_user)