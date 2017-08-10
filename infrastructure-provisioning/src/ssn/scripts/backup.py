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

from time import gmtime, strftime
from fabric.api import *
import argparse
import yaml
import sys
import os

parser = argparse.ArgumentParser(description="Backup script for DLab configs, keys, certs, jars, database & logs")
parser.add_argument('--dlab_path', type=str, default='/opt/dlab/', help='Path to DLab. Default: /opt/dlab/')
parser.add_argument('--configs', type=str, default='all', help='Comma separated names of config files, like "security.yml", etc. Default: all')
parser.add_argument('--keys', type=str, default='all', help='Comma separated names of keys, like "user_name.pub". Default: all')
parser.add_argument('--certs', type=str, default='all', help='Comma separated names of SSL certificates and keys, like "dlab-selfsigned.crt", etc. Also available: skip. Default: all')
parser.add_argument('--jars', type=str, default='skip', help='Comma separated names of jar application, like "self-service" (without .jar), etc. Also available: all. Default: skip')
parser.add_argument('--db', action='store_true', default=False, help='Mongo DB. Key without arguments. Default: disable')
parser.add_argument('--logs', action='store_true', default=False, help='All logs (include docker). Key without arguments. Default: disable')
args = parser.parse_args()


def backup_prepare():
    try:
        local("mkdir {}".format(temp_folder))
        local("mkdir {0}{1}".format(temp_folder, conf_folder))
        local("mkdir {}keys".format(temp_folder))
        if args.certs != "skip":
            local("mkdir {}certs".format(temp_folder))
        if args.jars != "skip":
            local("mkdir {}jars".format(temp_folder))
        if args.logs:
            local("mkdir {}logs".format(temp_folder))
            local("mkdir {}logs/docker".format(temp_folder))
    except Exception as err:
        print "Failed to create temp folder.", str(err)
        sys.exit(1)


def backup_configs():
    try:
        print "Backup configs: ", args.configs
        if args.configs == "all":
            local("find {0}{2} -name '*yml' -exec cp {3} {1}{2} \;".format(args.dlab_path, temp_folder, conf_folder, "{}"))
        else:
            for conf_file in args.configs.split(","):
                local("cp {0}{2}{3} {1}{2}".format(args.dlab_path, temp_folder, conf_folder, conf_file))
    except:
        print "Backup configs failed."
        pass


def backup_keys():
    try:
        print "Backup keys: ", args.keys
        if args.keys == "all":
            local("cp {0}* {1}keys".format(keys_folder, temp_folder))
        else:
            for key_file in args.keys.split(","):
                local("cp {0}{1} {2}keys".format(keys_folder, key_file, temp_folder))
    except:
        print "Backup keys failed."
        pass


def backup_certs():
    try:
        print "Backup certs: ", args.certs
        if args.certs == "skip":
            pass
        elif args.certs == "all":
            for cert in all_certs:
                local("sudo cp {0}{1} {2}certs".format(certs_folder, cert, temp_folder))
                local("sudo chown {0}:{0} {1}certs/{2} ".format(os_user, temp_folder, cert))
        else:
            for cert in args.certs.split(","):
                local("cp {0}{1} {2}certs".format(certs_folder, cert, temp_folder))
                local("sudo chown {0}:{0} {1}certs/{2} ".format(os_user, temp_folder, cert))
    except:
        print "Backup certs failed."
        pass


def backup_jars():
    try:
        print "Backup jars: ", args.jars
        if args.jars == "skip":
            pass
        elif args.jars == "all":
            for root, dirs, files in os.walk("{0}{1}".format(args.dlab_path, jars_folder)):
                for service in dirs:
                    local("cp -RP {0}{1}{2}* {3}jars".format(args.dlab_path, jars_folder, service, temp_folder))
        else:
            for service in args.jars.split(","):
                local("cp -RP {0}{1}{2}* {3}jars".format(args.dlab_path, jars_folder, service, temp_folder))
    except:
        print "Backup jars failed."
        pass


def backup_database():
    try:
        print "Backup db: ", args.db
        if args.db:
            ssn_conf = open("{0}{1}ssn.yml".format(args.dlab_path, conf_folder)).read()
            data = yaml.load("mongo{}".format(ssn_conf.split("mongo")[-1]))
            with settings(hide('running')):
                local("mongodump --host {0} --port {1} --username {2} --password '{3}' --db={4} --archive={5}mongo.db" \
                    .format(data['mongo']['host'], data['mongo']['port'], data['mongo']['username'],
                            data['mongo']['password'], data['mongo']['database'], temp_folder))
    except:
        print "Backup db failed."
        pass


def backup_logs():
    try:
        print "Backup logs: ", args.logs
        if args.logs:
            print "Backup dlab logs"
            local("cp -R {0}* {1}logs".format(dlab_logs_folder, temp_folder))
            print "Backup docker logs"
            local("sudo find {0} -name '*log' -exec cp {2} {1}logs/docker \;".format(docker_logs_folder, temp_folder, "{}"))
            local("sudo chown -R {0}:{0} {1}logs/docker".format(os_user, temp_folder))
    except:
        print "Backup logs failed."
        pass


def backup_finalize():
    try:
        print "Compressing all files to archive..."
        local("cd {0} && tar -zcf {1} .".format(temp_folder, dest_file))
    except Exception as err:
        print "Compressing backup failed.", str(err)

    try:
        print "Clear temp folder..."
        if temp_folder != "/":
            local("rm -rf {}".format(temp_folder))
    except Exception as err:
        print "Clear temp folder failed.", str(err)


if __name__ == "__main__":
    backup_time = strftime("%d_%b_%Y_%H-%M-%S", gmtime())
    os_user = os.environ['USER']
    temp_folder = "/tmp/dlab_backup-{}/".format(backup_time)
    conf_folder = "conf/"
    keys_folder = "/home/{}/keys/".format(os_user)
    certs_folder = "/etc/ssl/certs/"
    all_certs = ["dhparam.pem", "dlab-selfsigned.crt", "dlab-selfsigned.key"]
    jars_folder = "webapp/lib/"
    dlab_logs_folder = "/var/log/dlab/"
    docker_logs_folder = "/var/lib/docker/containers/"
    dest_file = "{0}tmp/dlab_backup-{1}.tar.gz".format(args.dlab_path, backup_time)

    # Backup file section
    backup_prepare()

    # Backup section
    backup_configs()
    backup_keys()
    backup_certs()
    backup_jars()
    backup_database()
    backup_logs()

    # Compressing & cleaning tmp folder
    backup_finalize()

    print "Successfully created backup file: {}".format(dest_file)