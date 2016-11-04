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

from pymongo import MongoClient
import random
import string
import yaml, json
import subprocess
import time
import argparse

path = "/etc/mongod.conf"
outfile = "/etc/mongo_params.yml"

parser = argparse.ArgumentParser()
parser.add_argument('--region', type=str, default='')
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
    mongo_passwd = id_generator()
    mongo_ip = read_yml_conf(path,'net','bindIp')
    mongo_port = read_yml_conf(path,'net','port')

    try:
        with open('/tmp/instance_shapes.lst', 'r') as source_shapes:
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
        client.dlabdb.settings.insert_one({"_id": "aws_region", "value": args.region})
        client.dlabdb.settings.insert_one({"_id": "service_base_name", "value": args.base_name})
        client.dlabdb.settings.insert_one({"_id": "security_groups_ids", "value": args.sg})
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

