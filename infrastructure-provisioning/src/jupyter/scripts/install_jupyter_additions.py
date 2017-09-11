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

from dlab.actions_lib import *
from fabric.api import *
from fabric.contrib.files import exists
import argparse
import json
import sys
from dlab.notebook_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()


def add_breeze_library_local(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/breeze_local_ensured'):
        try:
            breeze_tmp_dir = '/tmp/breeze_tmp_local/'
            jars_dir = '/opt/jars/'
            sudo('mkdir -p ' + breeze_tmp_dir)
            sudo('wget http://central.maven.org/maven2/org/scalanlp/breeze_2.11/0.12/breeze_2.11-0.12.jar -O ' +
                 breeze_tmp_dir + 'breeze_2.11-0.12.jar')
            sudo('wget http://central.maven.org/maven2/org/scalanlp/breeze-natives_2.11/0.12/breeze-natives_2.11-0.12.jar -O ' +
                 breeze_tmp_dir + 'breeze-natives_2.11-0.12.jar')
            sudo('wget http://central.maven.org/maven2/org/scalanlp/breeze-viz_2.11/0.12/breeze-viz_2.11-0.12.jar -O ' +
                 breeze_tmp_dir + 'breeze-viz_2.11-0.12.jar')
            sudo('wget http://central.maven.org/maven2/org/scalanlp/breeze-macros_2.11/0.12/breeze-macros_2.11-0.12.jar -O ' +
                 breeze_tmp_dir + 'breeze-macros_2.11-0.12.jar')
            sudo('wget http://central.maven.org/maven2/org/scalanlp/breeze-parent_2.11/0.12/breeze-parent_2.11-0.12.jar -O ' +
                 breeze_tmp_dir + 'breeze-parent_2.11-0.12.jar')
            sudo('wget http://central.maven.org/maven2/org/jfree/jfreechart/1.0.19/jfreechart-1.0.19.jar -O ' +
                 breeze_tmp_dir + 'jfreechart-1.0.19.jar')
            sudo('wget http://central.maven.org/maven2/org/jfree/jcommon/1.0.24/jcommon-1.0.24.jar -O ' +
                 breeze_tmp_dir + 'jcommon-1.0.24.jar')
            sudo('wget https://brunelvis.org/jar/spark-kernel-brunel-all-2.3.jar -O ' +
                 breeze_tmp_dir + 'spark-kernel-brunel-all-2.3.jar')
            sudo('mv ' + breeze_tmp_dir + '* ' + jars_dir)
        except:
            sys.exit(1)


##############
# Run script #
##############
if __name__ == "__main__":
    print "Configure connections"
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname
    deeper_config = json.loads(args.additional_config)

    print "Installing additional Python libraries"
    ensure_additional_python_libs(args.os_user)

    print "Installing notebook additions: matplotlib."
    ensure_matplot(args.os_user)

    print "Installing notebook additions: sbt."
    ensure_sbt(args.os_user)

    print "Installing Breeze library"
    add_breeze_library_local(args.os_user)

