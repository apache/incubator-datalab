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

import boto3
from fabric.api import *
import uuid
import argparse
import sys
import json
import os

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()


def get_note_status(note_id, notebook_ip):
    running = False
    local('sleep 5')
    response = local("curl -H 'Content-Type: application/json' -X GET  http://" +
                     notebook_ip + ":8080/api/notebook/job/" + note_id, capture=True)
    status = json.loads(response)
    for i in status.get('body'):
        if i.get('status') == "RUNNING" or i.get('status') == "PENDING":
            running = True
        elif i.get('status') == "ERROR":
            sys.exit(1)
    if running:
        local('sleep 5')
        get_note_status(note_id, notebook_ip)
    else:
        return "OK"


def import_note(note_path, notebook_ip):
    response = local("curl -H 'Content-Type: application/json' -X POST -d @" + note_path + " http://" +
                     notebook_ip + ":8080/api/notebook/import", capture=True)
    status = json.loads(response)
    if status.get('status') == 'CREATED':
        return status.get('body') 
    else:
        sys.exit(1)


def prepare_note(interpreter_name, template_path, note_name):
    with open(template_path, 'r') as f:
        text = f.read()
    text = text.replace('S3_BUCKET', args.bucket)
    text = text.replace('INTERPRETER_NAME', interpreter_name)
    with open(note_name, 'w') as f:
        f.write(text)


def run_note(note_id, notebook_ip):
    response = local("curl -H 'Content-Type: application/json' -X POST  http://" + notebook_ip +
                     ":8080/api/notebook/job/" + note_id, capture=True)
    status = json.loads(response)
    if status.get('status') == 'OK':
        get_note_status(note_id, notebook_ip)
    else:
        sys.exit(1)


def remove_note(note_id, notebook_ip):
    response = local("curl -H 'Content-Type: application/json' -X DELETE  http://" + notebook_ip +
                     ":8080/api/notebook/" + note_id, capture=True)
    status = json.loads(response)
    if status.get('status') == 'OK':
        return "OK"
    else:
        sys.exit(1)


def run_pyspark():
    interpreters = ['local_interpreter_python2.pyspark', args.cluster_name + "_py2.pyspark"]
    for i in interpreters:
        prepare_note(i, '/home/{}/test_templates/template_preparation_pyspark.json'.format(args.os_user),
                     '/home/{}/preparation_pyspark.json'.format(args.os_user))
        note_id = import_note('/home/{}/preparation_pyspark.json'.format( args.os_user), notebook_ip)
        run_note(note_id, notebook_ip)
        remove_note(note_id, notebook_ip)
        prepare_note(i, '/home/{}/test_templates/template_visualization_pyspark.json'.format(args.os_user),
                     '/home/{}/visualization_pyspark.json'.format(args.os_user))
        note_id = import_note('/home/{}/visualization_pyspark.json'.format( args.os_user), notebook_ip)
        run_note(note_id, notebook_ip)
        remove_note(note_id, notebook_ip)


def run_spark():
    interpreters = ['local_interpreter_python2.spark', args.cluster_name + "_py2.spark"]
    for i in interpreters:
        prepare_note(i, '/home/{}/test_templates/template_preparation_spark.json'.format(args.os_user),
                     '/home/{}/preparation_spark.json'.format(args.os_user))
        note_id = import_note('/home/{}/preparation_spark.json'.format( args.os_user), notebook_ip)
        run_note(note_id, notebook_ip)
        remove_note(note_id, notebook_ip)
        #prepare_note(i, '/home/{}/test_templates/template_visualization_spark.json'.format(args.os_user),
        # '/home/{0}/visualization_spark.json'.format(args.os_user))
        #note_id = import_note('/home/{}/visualization_spark.json'.format( args.os_user), notebook_ip)
        #run_note(note_id, notebook_ip)
        #remove_note(note_id, notebook_ip)


def run_sparkr():
    if os.path.exists('/opt/livy/'):
        interpreters = ['local_interpreter_python2.sparkr', args.cluster_name + "_py2.sparkr"]
    else:
        interpreters = ['local_interpreter_python2.r', args.cluster_name + "_py2.r"]
    for i in interpreters:
        prepare_note(i, '/home/{}/test_templates/template_preparation_sparkr.json'.format(args.os_user),
                     '/home/{}/preparation_sparkr.json'.format(args.os_user))
        note_id = import_note('/home/{}/preparation_sparkr.json'.format( args.os_user), notebook_ip)
        run_note(note_id, notebook_ip)
        remove_note(note_id, notebook_ip)
        prepare_note(i, '/home/{}/test_templates/template_visualization_sparkr.json'.format(args.os_user),
                     '/home/{}/visualization_sparkr.json'.format(args.os_user))
        note_id = import_note('/home/{}/visualization_sparkr.json'.format( args.os_user), notebook_ip)
        run_note(note_id, notebook_ip)
        remove_note(note_id, notebook_ip)


def prepare_templates():
    local('aws s3 cp --recursive s3://' + args.bucket + '/test_templates_zeppelin/ /home/{}/test_templates/'.
          format(args.os_user))
    
notebook_ip = local('hostname -I', capture=True)
prepare_templates()
run_pyspark()
run_sparkr()
run_spark()
