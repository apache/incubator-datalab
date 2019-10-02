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

from pymongo import MongoClient
import yaml, json, sys
import subprocess
import time
import argparse
from dlab.fab import *

path = "/etc/mongod.conf"
outfile = "/etc/mongo_params.yml"

parser = argparse.ArgumentParser()
parser.add_argument('--dlab_path', type=str, default='')
#parser.add_argument('--mongo_parameters', type=str, default='')
args = parser.parse_args()


def add_2_yml_config(path, section, param, value):
    try:
        try:
            with open(path, 'r') as config_yml_r:
                config_orig = yaml.load(config_yml_r)
        except:
            config_orig = {}
        sections = []
        for i in config_orig:
            sections.append(i)
        if section in sections:
            config_orig[section].update({param:value})
        else:
            config_orig.update({section:{param:value}})
        with open(path, 'w') as outfile_yml_w:
            yaml.dump(config_orig, outfile_yml_w, default_flow_style=False)
        return True
    except:
        print("Could not write the target file")
        return False


def read_yml_conf(path, section, param):
    try:
        with open(path, 'r') as config_yml:
            config = yaml.load(config_yml)
        result = config[section][param]
        return result
    except:
        print("File does not exist")
        return ''


if __name__ == "__main__":
    mongo_passwd = "PASSWORD"
    mongo_ip = read_yml_conf(path,'net','bindIp')
    mongo_port = read_yml_conf(path,'net','port')
    #mongo_parameters = json.loads(args.mongo_parameters)
    # Setting up admin's password and enabling security
    client = MongoClient(mongo_ip + ':' + str(mongo_port))
    pass_upd = True
    try:
        command = ['service', 'mongod', 'start']
        subprocess.call(command, shell=False)
        time.sleep(5)
        client.dlabdb.add_user('admin', mongo_passwd, roles=[{'role':'userAdminAnyDatabase','db':'admin'}])
        client.dlabdb.command('grantRolesToUser', "admin", roles=["readWrite"])
        # set_mongo_parameters(client, mongo_parameters)
        # with open(args.dlab_path + 'tmp/mongo_roles.json', 'r') as data:
        #     json_data = json.load(data)
        # for i in json_data:
        #     client.dlabdb.roles.insert_one(i)
        # client.dlabdb.security.create_index("expireAt", expireAfterSeconds=7200)
        if add_2_yml_config(path,'security','authorization','enabled'):
            command = ['service', 'mongod', 'restart']
            subprocess.call(command, shell=False)
    except:
        print("Looks like MongoDB have already been secured")
        pass_upd = False

    # Generating output config
    add_2_yml_config(outfile, 'network', 'ip', mongo_ip)
    add_2_yml_config(outfile, 'network', 'port', mongo_port)
    add_2_yml_config(outfile, 'account', 'user', 'admin')
    if pass_upd:
        add_2_yml_config(outfile, 'account', 'pass', mongo_passwd)

