#!/bin/bash

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

ip="IP_ADRESS"
jps -m | grep spark | \
while read i
do
  pid="$(echo $i | sed -n 's/\(^[0-9]*\) .*/\1/p')"
  port="$(ss -tlpn | cat | grep ${pid} | grep ':40.. ' | sed -n 's/.*:\(40..\) .*/\1/p')"
  app_master_check="$(curl http://${ip}:${port}/environment/ 2>&1 | grep spark.master > /dev/null && echo check || echo emr)"
  if [[ $app_master_check == "check" ]]
  then
  	parse_master="$(curl http://${ip}:${port}/environment/ 2>&1 | sed -n 's/.*spark\.master<\/td><td>\([^<]*\)<\/td>.*/\1/p')"
    if [[ $parse_master == *"local"* ]]
    then
      master="local"
    elif [[ $parse_master == *"spark://"* ]]
    then
      master="$(curl http://${ip}:${port}/environment/ 2>&1 | sed -n 's/.*spark\.master<\/td><td>\([^<]*\)<\/td>.*/\1/p' | sed -n 's/spark\:\/\/\([0-9.]*\):7077/\1/p' | sed -n 's/\./\-/gp')"
    fi
    app="$(curl http://${ip}:${port}/api/v1/applications/ 2>&1 | sed -n 's/\.*  "id" : "\(.*\)",/\1/p')"
    check="$(curl http://${ip}:${port}/api/v1/applications/${app}/jobs 2>&1 | grep RUNNING > /dev/null && echo 1 || echo 0)"
    if [[ $check == "1" ]]
    then
  	  date +%s > /opt/inactivity/${master}_inactivity
    fi
  fi
done