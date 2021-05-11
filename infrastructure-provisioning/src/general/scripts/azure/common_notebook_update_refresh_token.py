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

from xml.etree.ElementTree import parse, Element
from fabric import *
import argparse
import os
import sys
import time
from patchwork.files import exists
from patchwork import files

parser = argparse.ArgumentParser()
parser.add_argument('--refresh_token', type=str, default='')
args = parser.parse_args()


def update_refresh_token_on_notebook(refresh_token):
    core_site_path = '/opt/hadoop/etc/hadoop/core-site.xml'
    doc = parse(core_site_path)
    root = doc.getroot()
    for child in root:
        for i in child._children:
            if i.tag == 'name' and i.text == 'fs.adl.oauth2.refresh.token':
                for j in child._children:
                    if j.tag == 'value':
                        j.text = refresh_token
    doc.write(core_site_path)


if __name__ == "__main__":
    update_refresh_token_on_notebook(args.refresh_token)

