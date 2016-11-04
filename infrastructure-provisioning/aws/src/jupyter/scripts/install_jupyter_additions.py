#!/usr/bin/python

# ******************************************************************************************************
#
# Copyright (c) 2016 EPAM Systems Inc.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including # without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject # to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. # IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH # # THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# ****************************************************************************************************/

from fabric.api import *
from fabric.contrib.files import exists
import argparse
import json
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
args = parser.parse_args()


def ensure_matplot():
    if not exists('/home/ubuntu/.ensure_dir/matplot_ensured'):
        try:
            sudo('apt-get build-dep -y python-matplotlib')
            sudo('pip install matplotlib')
            sudo('pip3 install matplotlib')
            sudo('python3.4 -m pip install matplotlib  --upgrade')
            sudo('touch /home/ubuntu/.ensure_dir/matplot_ensured')
        except:
            sys.exit(1)


def ensure_sbt():
    if not exists('/home/ubuntu/.ensure_dir/sbt_ensured'):
        try:
            sudo('apt-get install -y apt-transport-https')
            sudo('echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list')
            sudo('apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823')
            sudo('apt-get update')
            sudo('apt-get install -y sbt')
            sudo('touch /home/ubuntu/.ensure_dir/sbt_ensured')
        except:
            sys.exit(1)


def ensure_libraries_py2():
    if not exists('/home/ubuntu/.ensure_dir/ensure_libraries_py2_installed'):
        try:
            sudo('export LC_ALL=C')
            sudo('apt-get install -y libjpeg8-dev zlib1g-dev')
            sudo('pip2 install boto boto3')
            sudo('pip2 install NumPy SciPy Matplotlib pandas Sympy Pillow sklearn')
            sudo('touch /home/ubuntu/.ensure_dir/ensure_libraries_py2_installed')
        except:
            sys.exit(1)


def ensure_libraries_py3():
    if not exists('/home/ubuntu/.ensure_dir/ensure_libraries_py3_installed'):
        try:
            sudo('pip3 install boto boto3')
            sudo('python3.4 -m pip install boto boto3 --upgrade')
            sudo('pip3 install NumPy SciPy Matplotlib pandas Sympy Pillow sklearn')
            sudo('python3.4 -m pip install NumPy SciPy Matplotlib pandas Sympy Pillow sklearn --upgrade')
            sudo('touch /home/ubuntu/.ensure_dir/ensure_libraries_py3_installed')
        except:
            sys.exit(1)

##############
# Run script #
##############
if __name__ == "__main__":
    print "Configure connections"
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = 'ubuntu@' + args.hostname
    deeper_config = json.loads(args.additional_config)

    print "Installing required libraries for Python 2.7"
    ensure_libraries_py2()

    print "Installing required libraries for Python 3"
    ensure_libraries_py3()

    print "Installing notebook additions: matplotlib."
    ensure_matplot()

    print "Installing notebook additions: sbt."
    ensure_sbt()

