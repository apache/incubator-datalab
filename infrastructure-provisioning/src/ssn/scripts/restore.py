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

from fabric.api import *
import argparse
import filecmp
# import json
# import time
import yaml
import sys
import os

parser = argparse.ArgumentParser(description="Restore script for DLab configs, keys, jars & database")
parser.add_argument('--dlab_path', type=str, default='/opt/dlab/', help='Path to DLab. Default: /opt/dlab/')
parser.add_argument('--config', type=str, default='all', help='Comma separated names of config files, like "security.yml", etc. Also available: skip. Default: all')
parser.add_argument('--keys', type=str, default='all', help='Comma separated names keys, like "user_name.pub". Also available: skip. Default: all')
parser.add_argument('--jar', type=str, default='skip', help='Comma separated names of jar application, like "self-service", etc. Default: skip')
parser.add_argument('--db', type=str, default=True, help='Mongo DB. True - Enable, False - Disable. Default: Enabled')
parser.add_argument('--file', type=str, default='', required=True, help='Full or relative path to backup file')
parser.add_argument('--force', type=str, default=False, help='Force mode. Without any questions. Default: False')
args = parser.parse_args()


def ask(question):
    if args.force:
        return True
    valid = {"yes": True, "y": True, "no": False, "n": False}
    while True:
        choice = raw_input("{0} {1} ".format(question, "[Y/yes/N/no]")).lower()
        try:
            if choice is None and choice == '':
                return True
            elif valid[choice]:
                return True
            else:
                return False
        except:
            print "Incorrect answer. Try again..."
            continue


if __name__ == "__main__":
    backup_file = os.path.join(os.path.dirname(__file__), args.file)
    conf_folder = "conf/"
    keys_folder = "/home/{}/keys/".format(os.environ['USER'])
    jar_folder = "webapp/lib/"
    temp_folder = ""
    try:
        if os.path.isfile(backup_file):
            local("mkdir {}".format(temp_folder))
            local("tar -xf {0} -C {1}".format(backup_file, temp_folder))
            temp_folder = "/tmp/{}/".format(args.file)
        elif os.path.isdir(backup_file):
            temp_folder = backup_file
        else:
            print "Please, specify file or folder. Try --help for more details."
            raise Exception
        print "Backup acrhive: {} contains following files (exclude logs):".format(backup_file)
        local("find {} -not -name '*log'".format(temp_folder))
    except Exception as err:
        print "Failed to unpack backup archive.", str(err)
        sys.exit(1)

    try:
        if ask("Maybe you want to create backup of existing configuration before restoring?"):
            with settings(hide('everything')):
                local("python backup.py --config all --keys all --jar all --db true")
    except:
        print "Failed to create new backup."
        sys.exit(1)

    try:
        if ask("Stop all services before restoring?"):
            local("sudo supervisorctl stop all")
        else:
            raise Exception
    except:
        print "Failed to stop all services. Can not continue."
        sys.exit(1)

    try:
        if not os.path.isdir("{0}{1}".format(temp_folder, conf_folder)):
            print "Config files are not available in this backup."
            raise Exception

        configs = list()
        if args.config == "all":
            configs = [files for root, dirs, files in os.walk("{0}{1}".format(temp_folder, conf_folder))][0]
        else:
            configs = args.config.split(",")
        print "Restore configs: ", configs

        if args.config == "skip":
            pass
        else:
            for filename in configs:
                if not os.path.isfile("{0}{1}{2}".format(temp_folder, conf_folder, filename)):
                    print "Config {} are not available in this backup.".format(filename)
                else:
                    if os.path.isfile("{0}{1}{2}".format(args.dlab_path, conf_folder, filename)):
                        backupfile = "{0}{1}{2}".format(temp_folder, conf_folder, filename)
                        destfile = "{0}{1}{2}".format(args.dlab_path, conf_folder, filename)
                        if not filecmp.cmp(backupfile, destfile):
                            if ask("Config {} was changed, rewrite it?".format(filename)):
                                local("cp -f {0} {1}".format(backupfile, destfile))
                            else:
                                print "Config {} was skipped.".format(destfile)
                        else:
                            print "Config {} was not changed. Skipped.".format(filename)
                    else:
                        print "Config {} does not exist. Creating.".format(filename)
                        local("cp {0}{1}{2} {3}{1}{2}".format(temp_folder, conf_folder, filename, args.dlab_path))
    except:
        print "Restore configs failed."
        pass

    try:
        if not os.path.isdir("{}keys".format(temp_folder)):
            print "Key files are not available in this backup."
            raise Exception

        keys = list()
        if args.keys == "all":
            keys = [files for root, dirs, files in os.walk("{0}keys".format(temp_folder))][0]
        else:
            keys = args.keys.split(",")
        print "Restore keys: ", keys

        if args.keys == "skip":
            pass
        else:
            for filename in keys:
                if not os.path.isfile("{0}keys/{1}".format(temp_folder, filename)):
                    print "Key {} are not available in this backup.".format(filename)
                else:
                    if os.path.isfile("{0}{1}".format(keys_folder, filename)):
                        print "Key {} already exist.".format(filename)
                        if not filecmp.cmp("{0}keys/{1}".format(temp_folder, filename), "{0}{1}".format(keys_folder, filename)):
                            if ask("Key {} was changed, rewrite it?".format(filename)):
                                local("cp -f {0}keys/{2} {1}{2}".format(temp_folder, keys_folder, filename))
                            else:
                                print "Key {} was skipped.".format(filename)
                        else:
                            print "Key {} was not changed. Skipped.".format(filename)
                    else:
                        print "Key {} does not exist. Creating.".format(filename)
                        local("cp {0}keys/{2} {1}{2}".format(temp_folder, keys_folder, filename))
    except:
        print "Restore keys failed."
        pass

    try:
        if not os.path.isdir("{0}jars".format(temp_folder)):
            print "Jar files are not available in this backup."
            raise Exception

        jars = list()
        if args.jar == "all":
            jars = [dirs for root, dirs, files in os.walk("{}jars".format(temp_folder))][0]
        else:
            jars = args.jar.split(",")
        print "Restore configs: ", jars

        if args.jar == "skip":
            pass
        else:
            for service in jars:
                if not os.path.isdir("{0}jars/{1}".format(temp_folder, service)):
                    print "Jar {} are not available in this backup.".format(service)
                else:
                    for root, dirs, files in os.walk("{0}jars/{1}".format(temp_folder, service)):
                        for filename in files:
                            if os.path.isfile("{0}{1}{2}/{3}".format(args.dlab_path, jar_folder, service, filename)):
                                backupfile = "{0}jars/{1}/{2}".format(temp_folder, service, filename)
                                destfile = "{0}{1}{2}/{3}".format(args.dlab_path, jar_folder, service, filename)
                                if not filecmp.cmp(backupfile, destfile):
                                    if ask("Jar {} was changed, rewrite it?".format(filename)):
                                        local("cp -f {0} {1}".format(backupfile, destfile))
                                    else:
                                        print "Jar {} was skipped.".format(destfile)
                                else:
                                    print "Jar {} was not changed. Skipped.".format(filename)
                            else:
                                local("cp {0}jars/{1}/{2} {3}{4}{1}".format(temp_folder, service, filename, args.dlab_path, jar_folder))
    except:
        print "Restore jars failed."
        pass

    try:
        print "Restore db: ", args.db
        if args.db:
            if ask("Do you want to drop existing database and restore another from backup?"):
                ssn_conf = open(args.dlab_path + conf_folder + 'ssn.yml').read()
                data = yaml.load("mongo" + ssn_conf.split("mongo")[-1])
                try:
                    print "Try to drop existing database."
                    # https://docs.mongodb.com/manual/reference/program/mongorestore/
                    with settings(hide('running')):
                        local("mongo --host {0} --port {1} --username {2} --password '{3}' {4} --eval 'db.dropDatabase();'" \
                            .format(data['mongo']['host'], data['mongo']['port'], data['mongo']['username'],
                                    data['mongo']['password'], data['mongo']['database']))
                except:
                    print "Failed to drop existing database."
                    pass
                with settings(hide('running')):
                    local("mongorestore --host {0} --port {1} --archive={2}mongo.db" \
                        .format(data['mongo']['host'], data['mongo']['port'], temp_folder))
            else:
                print "Restore db was skipped."
    except:
        print "Restore db failed."
        pass

    try:
        if ask("Start all services after restoring?"):
            local("sudo supervisorctl start all")
    except:
        print "Failed to start all services."
