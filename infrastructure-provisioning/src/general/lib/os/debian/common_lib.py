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
from patchwork.files import exists
from patchwork import files
from datalab.logger import logging
import sys
import os
import time
import subprocess
import datalab.fab

def handle_dpkg_lock(error='', rerun=False):
    try:
        count = 0
        if 'E: Could not get lock ' and 'It is held by process ' in error:
            log = datalab.fab.conn.sudo('cat /tmp/dpkg.log | grep "E: Could not get lock"').stdout
            lock_path = log.split('\n')[0][22:log.find('.')]
            pid = log.split('\n')[0][log.find('It is held by process ') + 22:].split(' ')[0]
            datalab.fab.conn.sudo('kill -9 {}'.format(pid))
            datalab.fab.conn.sudo('rm -f {}'.format(lock_path))
        while 'no_lock' not in error and count < 10:
            pid = datalab.fab.conn.sudo('lsof /var/lib/dpkg/lock-frontend | grep dpkg | awk \'{print $2}\'').stdout.replace( '\n', '')
            if pid != '':
                datalab.fab.conn.sudo('kill -9 {}'.format(pid))
                datalab.fab.conn.sudo('rm -f /var/lib/dpkg/lock-frontend')

            pid = datalab.fab.conn.sudo('lsof /var/lib/dpkg/lock | grep dpkg | awk \'{print $2}\'').stdout.replace('\n', '')
            if pid != '':
                datalab.fab.conn.sudo('kill -9 {}'.format(pid))
                datalab.fab.conn.sudo('rm -f /var/lib/dpkg/lock')

            if rerun:
                datalab.fab.conn.sudo('dpkg --configure -a 2>&1 | tee /tmp/tee.tmp; '
                                      'if ! grep -w -E "({0})" /tmp/tee.tmp; '
                                      'then echo "no_lock" > /tmp/dpkg.log; '
                                      'else cat /tmp/tee.tmp > /tmp/dpkg.log;fi; '
                                      'if ! grep -w -E "({1})" /tmp/tee.tmp; '
                                      'then echo "no_error" >> /tmp/dpkg.log; '
                                      'else cat /tmp/tee.tmp >> /tmp/dpkg.log;fi'.format(lock_parser,
                                                                                        error_parser))
                error = datalab.fab.conn.sudo('cat /tmp/dpkg.log').stdout
            else:
                error = 'no_lock'
            count = count + 1
        if 'no_error' not in error:
            raise Exception
    except:
        sys.exit(1)

def handle_apt_lock(error='', rerun=False):
    try:
        count = 0
        if 'E: Could not get lock ' and 'It is held by process ' in error:
            log = datalab.fab.conn.sudo('cat /tmp/apt.log | grep "E: Could not get lock"').stdout
            lock_path = log.split('\n')[0][22:log.find('.')]
            pid = log.split('\n')[0][log.find('It is held by process ') + 22:].split(' ')[0]
            datalab.fab.conn.sudo('kill -9 {}'.format(pid))
            datalab.fab.conn.sudo('rm -f {}'.format(lock_path))
        while 'no_lock' not in error and count < 10:
            pid = datalab.fab.conn.sudo('lsof /var/lib/apt/lists/lock | grep apt | awk \'{print $2}\'').stdout.replace('\n', '')
            if pid != '':
                datalab.fab.conn.sudo('kill -9 {}'.format(pid))
                datalab.fab.conn.sudo('rm -f /var/lib/apt/lists/lock')

            if rerun:
                datalab.fab.conn.sudo('apt update 2>&1 | tee /tmp/tee.tmp; '
                                      'if ! grep -w -E "({0})" /tmp/tee.tmp; '
                                      'then echo "no_lock" > /tmp/apt.log; '
                                      'else cat /tmp/tee.tmp > /tmp/apt.log;fi; '
                                      'if ! grep -w -E "({1})" /tmp/tee.tmp; '
                                      'then echo "no_error" >> /tmp/apt.log; '
                                      'else cat /tmp/tee.tmp >> /tmp/apt.log;fi'.format(lock_parser,
                                                                                       error_parser))
                error = datalab.fab.conn.sudo('cat /tmp/apt.log').stdout
            else:
                error = 'no_lock'
            count = count + 1
        if 'no_error' not in error:
            raise Exception
    except:
        sys.exit(1)

def handle_apt_get_lock(error='', rerun=False):
    try:
        count = 0
        if 'E: Could not get lock ' and 'It is held by process ' in error:
            log = datalab.fab.conn.sudo('cat /tmp/apt.log | grep "E: Could not get lock"').stdout
            lock_path = log.split('\n')[0][22:log.find('.')]
            pid = log.split('\n')[0][log.find('It is held by process ') + 22:].split(' ')[0]
            datalab.fab.conn.sudo('kill -9 {}'.format(pid))
            datalab.fab.conn.sudo('rm -f {}'.format(lock_path))
        while 'no_lock' not in error and count < 10:
            datalab.fab.conn.sudo('lsof /var/lib/dpkg/lock')
            datalab.fab.conn.sudo('lsof /var/lib/apt/lists/lock')
            datalab.fab.conn.sudo('lsof /var/cache/apt/archives/lock')
            datalab.fab.conn.sudo('rm -f /var/lib/apt/lists/lock')
            datalab.fab.conn.sudo('rm -f /var/cache/apt/archives/lock')
            datalab.fab.conn.sudo('rm -f /var/lib/dpkg/lock')

            if rerun:
                datalab.fab.conn.sudo('apt-get {0} {1} 2>&1 | tee /tmp/tee.tmp; '
                                      'if ! grep -w -E "({2})" /tmp/tee.tmp; '
                                      'then echo "no_lock" > /tmp/apt_get.log; '
                                      'else cat /tmp/tee.tmp > /tmp/apt_get.log;fi; '
                                      'if ! grep -w -E "({3})" /tmp/tee.tmp; '
                                      'then echo "no_error" >> /tmp/apt_get.log; '
                                      'else cat /tmp/tee.tmp >> /tmp/apt_get.log;fi'.format(command,
                                                                                           requisites,
                                                                                           lock_parser,
                                                                                           error_parser))
                error = datalab.fab.conn.sudo('cat /tmp/apt_get.log').stdout
            else:
                error = 'no_lock'
            count = count + 1
        if 'no_error' not in error:
            raise Exception
    except:
        sys.exit(1)

def manage_pkg(command, environment, requisites):
    try:
        allow = False
        counter = 0
        while not allow:
            if counter > 60:
                logging.error("Instance is broken (app manager does not work properly) please recreate it.")
                traceback.print_exc()
                sys.exit(1)
            else:
                logging.info('Package manager is:')
                if environment == 'remote':
                    if 'busy' in datalab.fab.conn.sudo('pgrep "^apt" -a && echo "busy" || echo "ready"').stdout or 'busy' in datalab.fab.conn.sudo('pgrep "^dpkg" -a && echo "busy" || echo "ready"').stdout:
                        counter += 1
                        time.sleep(10)
                    else:
                        try:
                            lock_parser = "frontend is locked|locked|not get lock|unavailable"
                            error_parser = "Could not|No matching|Error:|E:|failed|Requires:"

                            datalab.fab.conn.sudo('dpkg --configure -a 2>&1 | tee /tmp/tee.tmp; '
                                                  'if ! grep -w -E "({0})" /tmp/tee.tmp; '
                                                  'then echo "no_lock" > /tmp/dpkg.log; '
                                                  'else cat /tmp/tee.tmp > /tmp/dpkg.log;fi;'
                                                  'if ! grep -w -E "({1})" /tmp/tee.tmp; '
                                                  'then echo "no_error" >> /tmp/dpkg.log; '
                                                  'else cat /tmp/tee.tmp >> /tmp/dpkg.log;fi'.format(lock_parser,
                                                                                                    error_parser))
                            err = datalab.fab.conn.sudo('cat /tmp/dpkg.log').stdout
                            if 'no_lock' not in err:
                                handle_dpkg_lock(err, rerun=True)

                            datalab.fab.conn.sudo('apt update 2>&1 | tee /tmp/tee.tmp; '
                                                  'if ! grep -w -E "({0})" /tmp/tee.tmp; '
                                                  'then echo "no_lock" > /tmp/apt.log; '
                                                  'else cat /tmp/tee.tmp > /tmp/apt.log;fi; '
                                                  'if ! grep -w -E "({1})" /tmp/tee.tmp; '
                                                  'then echo "no_error" >> /tmp/apt.log; '
                                                  'else cat /tmp/tee.tmp >> /tmp/apt.log;fi'.format(lock_parser,
                                                                                                   error_parser))
                            err = datalab.fab.conn.sudo('cat /tmp/apt.log').stdout
                            if 'no_lock' not in err:
                                handle_dpkg_lock()
                                handle_apt_lock(err, rerun=True)

                            datalab.fab.conn.sudo('apt-get {0} {1} 2>&1 | tee /tmp/tee.tmp; '
                                                  'if ! grep -w -E "({2})" /tmp/tee.tmp; '
                                                  'then echo "no_lock" > /tmp/apt_get.log; '
                                                  'else cat /tmp/tee.tmp > /tmp/apt_get.log;fi; '
                                                  'if ! grep -w -E "({3})" /tmp/tee.tmp; '
                                                  'then echo "no_error" >> /tmp/apt_get.log; '
                                                  'else cat /tmp/tee.tmp >> /tmp/apt_get.log;fi'.format(command,
                                                                                                       requisites,
                                                                                                       lock_parser,
                                                                                                       error_parser))
                            err = datalab.fab.conn.sudo('cat /tmp/apt_get.log').stdout

                            if 'no_lock' not in err:
                                handle_dpkg_lock()
                                handle_apt_lock()
                                handle_apt_get_lock(err, rerun=True)

                            allow = True
                        except Exception as err:
                            traceback.print_exc()
                            append_result("Failed to manage_pkgs", str(err))
                elif environment == 'local':
                    if subprocess.run('sudo pgrep "^apt" -a && echo "busy" || echo "ready"',
                                      capture_output=True, shell=True, check=True) == 'busy':
                        counter += 1
                        time.sleep(10)
                    else:
                        allow = True
                        subprocess.run('sudo apt-get {0} {1}'.format(command, requisites),
                                       capture_output=True, shell=True, check=True)
                else:
                    logging.error('Wrong environment')
                    sys.exit(1)
    except Exception as err:
        logging.error('Managing packages function error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def ensure_pkg(os_user, requisites='linux-headers-$(uname -r) python3-pip python3-opencv python3-dev '
                                   'python3-virtualenv groff gcc vim less git wget libssl-dev unattended-upgrades '
                                   'nmap libffi-dev unzip libxml2-dev haveged'):
    try:
        if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/pkg_upgraded'.format(os_user)):
            count = 0
            check = False
            while not check:
                if count > 60:
                    logging.error("Repositories are not available. Please, try again later.")
                    sys.exit(1)
                else:
                    try:
                        logging.info("Updating repositories "
                                "and installing requested tools: {}".format(requisites))
                        logging.info("Attempt number " + str(count) + " to install requested tools. Max 60 tries.")
                        manage_pkg('update', 'remote', '')
                        manage_pkg('-y install', 'remote', requisites)
                        datalab.fab.conn.sudo('unattended-upgrades -v')
                        datalab.fab.conn.sudo(
                            'sed -i \'s|APT::Periodic::Unattended-Upgrade "1"|APT::Periodic::Unattended-Upgrade "0"|\' '
                            '/etc/apt/apt.conf.d/20auto-upgrades')
                        datalab.fab.conn.run('export LC_ALL=C')
                        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/pkg_upgraded'.format(os_user))
                        datalab.fab.conn.sudo('systemctl enable haveged')
                        datalab.fab.conn.sudo('systemctl start haveged')
                        if os.environ['conf_cloud_provider'] == 'aws':
                            manage_pkg('-y install --install-recommends', 'remote', 'linux-aws-hwe')
                        check = True
                    except:
                        count += 1
                        time.sleep(50)
    except Exception as err:
        logging.error('Installing prerequisites packages error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def find_java_path_remote():
    try:
        java_path = datalab.fab.conn.sudo("sh -c \"update-alternatives --query java | grep 'Value: ' | grep "
                                          "-o '/.*/jre'\"").stdout.replace('\n','')
        return java_path
    except Exception as err:
        logging.error('Finding remote java path error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def find_java_path_local():
    try:
        java_path = subprocess.run("sh -c \"update-alternatives --query java | grep 'Value: ' | grep "
                                   "-o '/.*/jre'\"", capture_output=True, shell=True, check=True).stdout.decode(
            'UTF-8').rstrip("\n\r")
        return java_path
    except Exception as err:
        logging.error('Finding local java path error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def remove_scp_nmap_binary(os_user):
    try:
        if not exists(datalab.fab.conn, '/home/{}/.ensure_dir/remove_scp_nmap_binary'.format(os_user)):
            datalab.fab.conn.sudo('rm /usr/bin/scp')
            datalab.fab.conn.sudo('rm /usr/share/nmap/nselib/data/psexec/nmap_service.exe > /dev/null; echo $?')
        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/remove_scp_nmap_binary'.format(os_user))
    except Exception as err:
        logging.error('Updating openssh to version:', str(err))
        traceback.print_exc()
        sys.exit(1)


def ensure_openssh_version(os_user):
    try:
        if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/openssh_version_ensured'.format(os_user)):
            if os.environ['conf_openssh_version'] not in datalab.fab.conn.sudo('ssh -V').stdout:
                datalab.fab.conn.sudo('mkdir /var/lib/sshd')
                datalab.fab.conn.sudo('chmod -R 700 /var/lib/sshd/')
                datalab.fab.conn.sudo('chown -R root:sys /var/lib/sshd/')
                datalab.fab.conn.sudo('wget -c https://cdn.openbsd.org/pub/OpenBSD/OpenSSH/portable/openssh-{0}.tar.gz '
                                      '-O /tmp/openssh-{0}.tar.gz'.format(os.environ['conf_openssh_version']))
                datalab.fab.conn.sudo('bash -l -c "tar -zhxvf /tmp/openssh-{0}.tar.gz -C /tmp/; cd /tmp/openssh-{0}; ./configure; make; make install"'.format(os.environ['conf_openssh_version']))
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/openssh_version_ensured'.format(os_user))
    except Exception as err:
        logging.error('Updating openssh to version:', str(err))
        traceback.print_exc()
        sys.exit(1)

def ensure_ntpd(os_user, edge_private_ip=''):
    try:
        if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/ntpd_ensured'.format(os_user)):
            datalab.fab.conn.sudo('timedatectl set-ntp no')
            manage_pkg('-y install', 'remote', 'ntp ntpdate')
            datalab.fab.conn.sudo('bash -c \"echo "tinker panic 0" >> /etc/ntp.conf\"')
            if os.environ['conf_resource'] != 'ssn' and os.environ['conf_resource'] != 'edge':
                datalab.fab.conn.sudo('bash -c \"echo "server {} prefer iburst" >> /etc/ntp.conf\"'.format(
                    edge_private_ip))
            datalab.fab.conn.sudo('systemctl restart ntp')
            datalab.fab.conn.sudo('systemctl enable ntp')
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/ntpd_ensured'.format(os_user))
    except Exception as err:
        logging.error('Installing NTPD error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def ensure_java(os_user):
    try:
        if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/java_ensured'.format(os_user)):
            manage_pkg('-y install', 'remote', 'openjdk-8-jdk-headless')
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/java_ensured'.format(os_user))
    except Exception as err:
        logging.error('Installing Java error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def ensure_step(os_user):
    try:
        if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/step_ensured'.format(os_user)):
            manage_pkg('-y install', 'remote', 'wget')
            datalab.fab.conn.sudo('wget https://github.com/smallstep/cli/releases/download/v0.13.3/step-cli_0.13.3_amd64.deb '
                 '-O /tmp/step-cli_0.13.3_amd64.deb')
            datalab.fab.conn.sudo('dpkg -i /tmp/step-cli_0.13.3_amd64.deb')
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/step_ensured'.format(os_user))
    except:
        logging.error('Installing step-cli error:', str(err))
        traceback.print_exc()
        sys.exit(1)
