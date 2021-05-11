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

import os, sys, json
from fabric import *
import argparse
import requests
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--storage', type=str, default='')
parser.add_argument('--cloud', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--azure_storage_account', type=str, default='')
parser.add_argument('--azure_datalake_account', type=str, default='')
args = parser.parse_args()


def prepare_templates():
    subprocess.run('mv /tmp/zeppelin /home/{0}/test_templates'.format(args.os_user), shell=True, check=True)

def get_storage():
    storages = {"aws": args.storage,
                "azure": "{0}@{1}.blob.core.windows.net".format(args.storage, args.azure_storage_account),
                "gcp": args.storage}
    protocols = {"aws": "s3a", "azure": "wasbs", "gcp": "gs"}
    if args.azure_datalake_account:
        storages['azure'] = "{0}.azuredatalakestore.net/{1}".format(args.azure_datalake_account, args.storage)
        protocols['azure'] = 'adl'
    return (storages[args.cloud], protocols[args.cloud])

def get_note_status(note_id, notebook_ip):
    running = False
    subprocess.run('sleep 5', shell=True, check=True)
    response = requests.get('http://{0}:8080/api/notebook/job/{1}'.format(notebook_ip, note_id))
    status = json.loads(response.content)
    for i in status.get('body'):
        if i.get('status') == "RUNNING" or i.get('status') == "PENDING":
            print('Notebook status: {}'.format(i.get('status')))
            running = True
        elif i.get('status') == "ERROR":
            print('Error in notebook')
            sys.exit(1)
    if running:
        subprocess.run('sleep 5', shell=True, check=True)
        get_note_status(note_id, notebook_ip)
    else:
        return "OK"

def import_note(note_path, notebook_ip):
    headers = {'Accept': 'application/json', 'Content-Type': 'application/json', 'Expires': '0'}
    response = requests.post('http://{0}:8080/api/notebook/import'.format(notebook_ip), data=open(note_path, 'rb'), headers=headers)
    status = json.loads(response.content)
    if status.get('status') == 'OK':
        print('Imported notebook: {}'.format(note_path))
        return status.get('body')
    else:
        print('Failed to import notebook')
        sys.exit(1)

def prepare_note(interpreter_name, template_path, note_name):
    with open(template_path, 'r') as f:
        text = f.read()
    text = text.replace('INTERPRETER_NAME', interpreter_name)
    text = text.replace('WORKING_STORAGE', get_storage()[0])
    text = text.replace('PROTOCOL_NAME', get_storage()[1])
    with open(note_name, 'w') as f:
        f.write(text)

def run_note(note_id, notebook_ip):
    response = requests.post('http://{0}:8080/api/notebook/job/{1}'.format(notebook_ip, note_id))
    status = json.loads(response.content)
    if status.get('status') == 'OK':
        get_note_status(note_id, notebook_ip)
    else:
        print('Failed to run notebook')
        sys.exit(1)

def remove_note(note_id, notebook_ip):
    response = requests.delete('http://{0}:8080/api/notebook/{1}'.format(notebook_ip, note_id))
    status = json.loads(response.content)
    if status.get('status') == 'OK':
        return "OK"
    else:
        sys.exit(1)

def restart_interpreter(notebook_ip, interpreter):
    response = requests.get('http://{0}:8080/api/interpreter/setting'.format(notebook_ip))
    status = json.loads(response.content)
    if status.get('status') == 'OK':
        id = [i['id'] for i in status['body'] if i['name'] in interpreter][0]
        response = requests.put('http://{0}:8080/api/interpreter/setting/restart/{1}'.format(notebook_ip, id))
        status = json.loads(response.content)
        if status.get('status') == 'OK':
            subprocess.run('sleep 5', shell=True, check=True)
            return "OK"
        else:
            print('Failed to restart interpreter')
            sys.exit(1)
    else:
        print('Failed to get interpreter settings')
        sys.exit(1)

def run_pyspark():
    interpreters = ['local_interpreter_python2.pyspark', args.cluster_name + "_py2.pyspark"]
    for i in interpreters:
        prepare_note(i, '/home/{}/test_templates/template_preparation_pyspark.json'.format(args.os_user),
                     '/home/{}/preparation_pyspark.json'.format(args.os_user))
        note_id = import_note('/home/{}/preparation_pyspark.json'.format(args.os_user), notebook_ip)
        run_note(note_id, notebook_ip)
        remove_note(note_id, notebook_ip)
        prepare_note(i, '/home/{}/test_templates/template_visualization_pyspark.json'.format(args.os_user),
                     '/home/{}/visualization_pyspark.json'.format(args.os_user))
        note_id = import_note('/home/{}/visualization_pyspark.json'.format(args.os_user), notebook_ip)
        run_note(note_id, notebook_ip)
        remove_note(note_id, notebook_ip)
        restart_interpreter(notebook_ip, i)

def run_sparkr():
    if os.path.exists('/opt/livy/'):
        interpreters = ['local_interpreter_python2.sparkr', args.cluster_name + "_py2.sparkr"]
    else:
        interpreters = ['local_interpreter_python2.r', args.cluster_name + "_py2.r"]
    for i in interpreters:
        prepare_note(i, '/home/{}/test_templates/template_preparation_sparkr.json'.format(args.os_user),
                     '/home/{}/preparation_sparkr.json'.format(args.os_user))
        note_id = import_note('/home/{}/preparation_sparkr.json'.format(args.os_user), notebook_ip)
        run_note(note_id, notebook_ip)
        remove_note(note_id, notebook_ip)
        prepare_note(i, '/home/{}/test_templates/template_visualization_sparkr.json'.format(args.os_user),
                     '/home/{}/visualization_sparkr.json'.format(args.os_user))
        note_id = import_note('/home/{}/visualization_sparkr.json'.format(args.os_user), notebook_ip)
        run_note(note_id, notebook_ip)
        remove_note(note_id, notebook_ip)
        restart_interpreter(notebook_ip, i)

def run_spark():
    interpreters = ['local_interpreter_python2.spark', args.cluster_name + "_py2.spark"]
    for i in interpreters:
        prepare_note(i, '/home/{}/test_templates/template_preparation_spark.json'.format(args.os_user),
                     '/home/{}/preparation_spark.json'.format(args.os_user))
        note_id = import_note('/home/{}/preparation_spark.json'.format(args.os_user), notebook_ip)
        run_note(note_id, notebook_ip)
        remove_note(note_id, notebook_ip)
        restart_interpreter(notebook_ip, i)


if __name__ == "__main__":
    try:
        notebook_ip = subprocess.run('hostname -I', capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
        prepare_templates()
        run_pyspark()
        run_sparkr()
        run_spark()
    except Exception as err:
        print('Error!', str(err))
        sys.exit(1)

    sys.exit(0)