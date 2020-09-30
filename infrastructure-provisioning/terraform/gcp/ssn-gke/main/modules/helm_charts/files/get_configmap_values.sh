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

creds_file_path=$1
gke_name=$2
region=$3
project_id=$4

gcloud auth activate-service-account --key-file "$creds_file_path"
export KUBECONFIG=/tmp/config; gcloud beta container clusters get-credentials "$gke_name" --region "$region" --project "$project_id"
ROOT_CA=$(kubectl get -o jsonpath="{.data['root_ca\.crt']}" configmaps/step-certificates-certs -ndatalab | base64 | tr -d '\n')
KID=$(kubectl get -o jsonpath="{.data['ca\.json']}" configmaps/step-certificates-config -ndatalab | jq -r .authority.provisioners[].key.kid)
KID_NAME=$(kubectl get -o jsonpath="{.data['ca\.json']}" configmaps/step-certificates-config -ndatalab | jq -r .authority.provisioners[].name)
jq -n --arg rootCa "$ROOT_CA" --arg kid "$KID" --arg kidName "$KID_NAME" '{rootCa: $rootCa, kid: $kid, kidName: $kidName}'
unset KUBECONFIG
rm /tmp/config
