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

import argparse
import pymongo
import yaml
import os

parser = argparse.ArgumentParser()
parser.add_argument('--dlab_path', type=str, default='')
parser.add_argument('--user_name', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    ssn_yml = os.popen("cat {}conf/ssn.yml | sed '/^[#<]/d'".format(args.dlab_path))
    config = yaml.load(ssn_yml)
    mongo_user = config['mongo']['username']
    mongo_password = config['mongo']['password']
    mongo_database = config['mongo']['database']
    mongo_host = config['mongo']['host']
    mongo_port = config['mongo']['port']
    uri = "mongodb://{}:{}@{}:{}/{}".format(mongo_user, mongo_password, mongo_host, mongo_port, mongo_database)
    client = pymongo.MongoClient(uri)
    db = client[mongo_database]
    collections = db.collection_names()
    for collection in collections:
        db_collection = db[collection]
        for document in db_collection.find():
            for key in document:
                if document[key] == args.user_name:
                    db_collection.delete_one({key: document[key]})