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

from pymongo import MongoClient
import random
import string
import yaml, json
import subprocess
import time
import argparse
import os

path = "/etc/mongod.conf"
outfile = "/etc/mongo_params.yml"

parser = argparse.ArgumentParser()
parser.add_argument('--region', type=str, default='')
parser.add_argument('--vpc', type=str, default='')
parser.add_argument('--subnet', type=str, default='')
parser.add_argument('--base_name', type=str, default='')
parser.add_argument('--sg', type=str, default='')
args = parser.parse_args()


def id_generator(size=10, chars=string.digits + string.ascii_letters):
    return ''.join(random.choice(chars) for _ in range(size))


def read_yml_conf(path, section, param):
    try:
        with open(path, 'r') as config_yml:
            config = yaml.load(config_yml)
        result = config[section][param]
        return result
    except:
        print "File does not exist"
        return ''


def add_2_yml_config(path,section,param,value):
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
            yaml.dump(config_orig, outfile_yml_w, default_flow_style=True)
        return True
    except:
        print "Could not write the target file"
        return False

if __name__ == "__main__":
    # mongo_passwd = id_generator()
    mongo_passwd = "XS3ms9R3tP"
    mongo_ip = read_yml_conf(path,'net','bindIp')
    mongo_port = read_yml_conf(path,'net','port')

    try:
        with open(os.environ['ssn_dlab_path'] + 'tmp/instance_shapes.lst', 'r') as source_shapes:
            shapes = json.load(source_shapes)
    except:
        shapes = []

    # Setting up admin's password and enabling security
    client = MongoClient(mongo_ip + ':' + str(mongo_port))
    pass_upd = True
    try:
        command = ['service', 'mongod', 'start']
        subprocess.call(command, shell=False)
        time.sleep(5)
        client.dlabdb.add_user('admin', mongo_passwd, roles=[{'role':'userAdminAnyDatabase','db':'admin'}])
        client.dlabdb.command('grantRolesToUser', "admin", roles=["readWrite"])
        client.dlabdb.settings.insert_one({"_id": "creds_region", "value": args.region})
        client.dlabdb.settings.insert_one({"_id": "creds_vpc_id", "value": args.vpc})
        client.dlabdb.settings.insert_one({"_id": "creds_subnet_id", "value": args.subnet})
        client.dlabdb.settings.insert_one({"_id": "service_base_name", "value": args.base_name})
        client.dlabdb.settings.insert_one({"_id": "security_groups_ids", "value": args.sg})
        client.dlabdb.settings.insert_one({"_id": "notebook_ssh_user", "value": "ubuntu"})
        client.dlabdb.settings.insert_one({"_id": "creds_key_dir", "value": "/root/keys"})
        client.dlabdb.security.insert({ "expireAt": "1" }, { "expireAfterSeconds": "3600" })
        client.dlabdb.shapes.insert(shapes)
        if add_2_yml_config(path,'security','authorization','enabled'):
            command = ['service', 'mongod', 'restart']
            subprocess.call(command, shell=False)
    except:
        print "Looks like MongoDB have already been secured"
        pass_upd = False

    # Generating output config
    add_2_yml_config(outfile,'network','ip',mongo_ip)
    add_2_yml_config(outfile,'network','port',mongo_port)
    add_2_yml_config(outfile,'account','user','admin')
    if pass_upd:
        add_2_yml_config(outfile,'account','pass',mongo_passwd)

