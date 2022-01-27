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

import argparse
import sys
import yaml
from pymongo import MongoClient

path = "/etc/mongod.conf"
outfile = "/etc/mongo_params.yml"

parser = argparse.ArgumentParser()
parser.add_argument('--resource', type=str, default='')
parser.add_argument('--status', type=str, default='')
args = parser.parse_args()


def read_yml_conf(path, section, param):
    try:
        with open(path, 'r') as config_yml:
            config = yaml.safe_load(config_yml)
        result = config[section][param]
        return result
    except:
        print("File does not exist")
        return ''


def update_resource_status(resource, status):
    path = "/etc/mongod.conf"
    mongo_passwd = "PASSWORD"
    mongo_ip = read_yml_conf(path, 'net', 'bindIp')
    mongo_port = read_yml_conf(path, 'net', 'port')
    client = MongoClient(mongo_ip + ':' + str(mongo_port))
    client = MongoClient("mongodb://admin:" + mongo_passwd + "@" + mongo_ip + ':' + str(mongo_port) + "/datalabdb")
    client.datalabdb.statuses.save({"_id": resource, "value": status})


if __name__ == "__main__":
    try:
        update_resource_status(args.resource, args.status)
    except:
        print("Unable to update status for the resource {}".format(args.resource))
        sys.exit(1)