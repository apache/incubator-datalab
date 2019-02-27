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

from fabric.api import *
import argparse
import filecmp
import yaml
import sys
import os

parser = argparse.ArgumentParser(description="Restore script for DLab configs, keys, certs, jars & database")
parser.add_argument('--dlab_path', type=str, default='/opt/dlab/', help='Path to DLab. Default: /opt/dlab/')
parser.add_argument('--configs', type=str, default='all', help='Comma separated names of config files, like "security.yml", etc. Also available: skip. Default: all')
parser.add_argument('--keys', type=str, default='all', help='Comma separated names of keys, like "user_name.pub". Also available: skip. Default: all')
parser.add_argument('--certs', type=str, default='all', help='Comma separated names of SSL certificates and keys, like "dlab.crt", etc. Also available: skip. Default: all')
parser.add_argument('--jars', type=str, default='skip', help='Comma separated names of jar application, like "self-service", etc. Default: skip')
parser.add_argument('--db', action='store_true', default=False, help='Mongo DB. Key without arguments. Default: disable')
parser.add_argument('--file', type=str, default='', required=True, help='Full or relative path to backup file or folder. Required field')
parser.add_argument('--force', action='store_true', default=False, help='Force mode. Without any questions. Key without arguments. Default: disable')
args = parser.parse_args()


def ask(question):
    if args.force:
        return True
    valid = {"": True, "yes": True, "y": True, "no": False, "n": False}
    while True:
        choice = raw_input("{0} {1} ".format(question, "[Y/yes/N/no]")).lower()
        try:
            if valid[choice]:
                return True
            else:
                return False
        except:
            print("Incorrect answer. Try again...")
            continue


def restore_prepare():
    try:
        if os.path.isfile(backup_file):
            head, tail = os.path.split(args.file)
            temp_folder = "/tmp/{}/".format(tail.split(".")[0])
            if os.path.isdir(temp_folder):
                print("Temporary folder with this backup already exist.")
                print("Use folder path '{}' in --file key".format(temp_folder))
                raise Exception
            print("Backup acrhive will be unpacked to: {}".format(temp_folder))
            local("mkdir {}".format(temp_folder))
            local("tar -xf {0} -C {1}".format(backup_file, temp_folder))
        elif os.path.isdir(backup_file):
            temp_folder = backup_file
        else:
            print("Please, specify file or folder. Try --help for more details.")
            raise Exception
        print("Backup acrhive: {} contains following files (exclude logs):".format(backup_file))
        local("find {} -not -name '*log'".format(temp_folder))
    except Exception as err:
        print("Failed to open backup.{}".format(str(err)))
        sys.exit(1)

    try:
        if ask("Maybe you want to create backup of existing configuration before restoring?"):
            with settings(hide('everything')):
                print("Creating new backup...")
                local("python backup.py --configs all --keys all --certs all --jar all --db")
    except:
        print("Failed to create new backup.")
        sys.exit(1)

    try:
        if ask("Stop all services before restoring?"):
            local("sudo supervisorctl stop all")
        else:
            raise Exception
    except:
        print("Failed to stop all services. Can not continue.")
        sys.exit(1)

    return temp_folder


def restore_configs():
    try:
        if not os.path.isdir("{0}{1}".format(temp_folder, conf_folder)):
            print("Config files are not available in this backup.")
            raise Exception

        configs = list()
        if args.configs == "all":
            configs = [files for root, dirs, files in os.walk("{0}{1}".format(temp_folder, conf_folder))][0]
        else:
            configs = args.configs.split(",")
        print("Restore configs: {}".format(configs))

        if args.configs != "skip":
            for filename in configs:
                if not os.path.isfile("{0}{1}{2}".format(temp_folder, conf_folder, filename)):
                    print("Config {} are not available in this backup.".format(filename))
                else:
                    if os.path.isfile("{0}{1}{2}".format(args.dlab_path, conf_folder, filename)):
                        backupfile = "{0}{1}{2}".format(temp_folder, conf_folder, filename)
                        destfile = "{0}{1}{2}".format(args.dlab_path, conf_folder, filename)
                        if not filecmp.cmp(backupfile, destfile):
                            if ask("Config {} was changed, rewrite it?".format(filename)):
                                local("cp -f {0} {1}".format(backupfile, destfile))
                            else:
                                print("Config {} was skipped.".format(destfile))
                        else:
                            print("Config {} was not changed. Skipped.".format(filename))
                    else:
                        print("Config {} does not exist. Creating.".format(filename))
                        local("cp {0}{1}{2} {3}{1}{2}".format(temp_folder, conf_folder, filename, args.dlab_path))
    except:
        print("Restore configs failed.")


def restore_keys():
    try:
        if not os.path.isdir("{}keys".format(temp_folder)):
            print("Key files are not available in this backup.")
            raise Exception

        keys = list()
        if args.keys == "all":
            keys = [files for root, dirs, files in os.walk("{}keys".format(temp_folder))][0]
        else:
            keys = args.keys.split(",")
        print("Restore keys: {}".format(keys))

        if args.keys != "skip":
            for filename in keys:
                if not os.path.isfile("{0}keys/{1}".format(temp_folder, filename)):
                    print("Key {} are not available in this backup.".format(filename))
                else:
                    if os.path.isfile("{0}{1}".format(keys_folder, filename)):
                        print("Key {} already exist.".format(filename))
                        if not filecmp.cmp("{0}keys/{1}".format(temp_folder, filename), "{0}{1}".format(keys_folder, filename)):
                            if ask("Key {} was changed, rewrite it?".format(filename)):
                                local("cp -f {0}keys/{2} {1}{2}".format(temp_folder, keys_folder, filename))
                            else:
                                print("Key {} was skipped.".format(filename))
                        else:
                            print("Key {} was not changed. Skipped.".format(filename))
                    else:
                        print("Key {} does not exist. Creating.".format(filename))
                        local("cp {0}keys/{2} {1}{2}".format(temp_folder, keys_folder, filename))
    except:
        print("Restore keys failed.")


def restore_certs():
    try:
        if not os.path.isdir("{}certs".format(temp_folder)):
            print("Cert files are not available in this backup.")
            raise Exception

        certs = list()
        if args.certs == "all":
            certs = [files for root, dirs, files in os.walk("{}certs".format(temp_folder))][0]
        else:
            certs = args.certs.split(",")
        print("Restore certs: {}".format(certs))

        if args.certs != "skip":
            for filename in certs:
                if not os.path.isfile("{0}certs/{1}".format(temp_folder, filename)):
                    print("Cert {} are not available in this backup.".format(filename))
                else:
                    if os.path.isfile("{0}{1}".format(certs_folder, filename)):
                        print("Cert {} already exist.".format(filename))
                        if not filecmp.cmp("{0}certs/{1}".format(temp_folder, filename), "{0}{1}".format(certs_folder, filename)):
                            if ask("Cert {} was changed, rewrite it?".format(filename)):
                                local("sudo cp -f {0}certs/{2} {1}{2}".format(temp_folder, certs_folder, filename))
                                local("sudo chown {0}:{0} {1}{2}".format("root", certs_folder, filename))
                            else:
                                print("Cert {} was skipped.".format(filename))
                        else:
                            print("Cert {} was not changed. Skipped.".format(filename))
                    else:
                        print("Cert {} does not exist. Creating.".format(filename))
                        local("sudo cp {0}certs/{2} {1}{2}".format(temp_folder, certs_folder, filename))
                        local("sudo chown {0}:{0} {1}{2}".format("root", certs_folder, filename))
    except:
        print("Restore certs failed.")


def restore_jars():
    try:
        if not os.path.isdir("{0}jars".format(temp_folder)):
            print("Jar files are not available in this backup.")
            raise Exception

        jars = list()
        if args.jars == "all":
            jars = [dirs for root, dirs, files in os.walk("{}jars".format(temp_folder))][0]
        else:
            jars = args.jars.split(",")
        print("Restore jars: {}".format(jars))

        if args.jars != "skip":
            for service in jars:
                if not os.path.isdir("{0}jars/{1}".format(temp_folder, service)):
                    print("Jar {} are not available in this backup.".format(service))
                else:
                    for root, dirs, files in os.walk("{0}jars/{1}".format(temp_folder, service)):
                        for filename in files:
                            if os.path.isfile("{0}{1}{2}/{3}".format(args.dlab_path, jars_folder, service, filename)):
                                backupfile = "{0}jars/{1}/{2}".format(temp_folder, service, filename)
                                destfile = "{0}{1}{2}/{3}".format(args.dlab_path, jars_folder, service, filename)
                                if not filecmp.cmp(backupfile, destfile):
                                    if ask("Jar {} was changed, rewrite it?".format(filename)):
                                        local("cp -fP {0} {1}".format(backupfile, destfile))
                                    else:
                                        print("Jar {} was skipped.".format(destfile))
                                else:
                                    print("Jar {} was not changed. Skipped.".format(filename))
                            else:
                                print("Jar {} does not exist. Creating.".format(filename))
                                local("cp -P {0}jars/{1}/{2} {3}{4}{1}".format(temp_folder, service, filename, args.dlab_path, jars_folder))
    except:
        print("Restore jars failed.")


def restore_database():
    try:
        print("Restore database: {}".format(args.db))
        if args.db:
            if not os.path.isfile("{0}{1}".format(temp_folder, "mongo.db")):
                print("File {} are not available in this backup.".format("mongo.db"))
                raise Exception
            else:
                if ask("Do you want to drop existing database and restore another from backup?"):
                    ssn_conf = open(args.dlab_path + conf_folder + 'ssn.yml').read()
                    data = yaml.load("mongo" + ssn_conf.split("mongo")[-1])
                    print("Restoring database from backup")
                    local("mongorestore --drop --host {0} --port {1} --archive={2}/mongo.db --username {3} --password '{4}' --authenticationDatabase={5}" \
                            .format(data['mongo']['host'], data['mongo']['port'], temp_folder,
                                    data['mongo']['username'], data['mongo']['password'], data['mongo']['database']))
        else:
            print("Restore database was skipped.")
    except:
        print("Restore database failed.")


def restore_finalize():
    try:
        if ask("Start all services after restoring?"):
            local("sudo supervisorctl start all")
    except:
        print("Failed to start all services.")

    try:
        if ask("Clean temporary folder {}?".format(temp_folder)) and temp_folder != "/":
            local("rm -rf {}".format(temp_folder))
    except Exception as err:
        print("Clear temp folder failed. {}".format(str(err)))


if __name__ == "__main__":
    backup_file = os.path.join(os.path.dirname(__file__), args.file)
    conf_folder = "conf/"
    keys_folder = "/home/{}/keys/".format(os.environ['USER'])
    certs_folder = "/etc/ssl/certs/"
    jars_folder = "webapp/lib/"
    temp_folder = ""

    # Backup file section
    temp_folder = restore_prepare()

    # Restore section
    restore_configs()
    restore_keys()
    restore_certs()
    restore_jars()
    restore_database()

    # Starting services & cleaning tmp folder
    restore_finalize()

    print("Restore is finished. Good luck.")