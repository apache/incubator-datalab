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

root_crt_path=STEP_ROOT_CERT_PATH
crt_path=STEP_CERT_PATH
key_path=STEP_KEY_PATH
ca_url=STEP_CA_URL
resource_type=RESOURCE_TYPE
renew_status=0
sans='SANS'
cn=CN
kid=KID
provisioner_password_path=STEP_PROVISIONER_PASSWORD_PATH

function log() {
    dt=$(date '+%d/%m/%Y %H:%M:%S');
    echo "[${dt} | ${1}]"
}

function renew_cert() {
  log "Trying to renew certificate ${crt_path}"
  if [ $resource_type = 'edge' ]; then
    step ca renew ${crt_path} ${key_path} --exec 'nginx -s reload' --ca-url ${ca_url} --root ${root_crt_path} --force --expires-in 8h
  elif [ $resource_type = 'endpoint' ]; then
    step ca renew ${crt_path} ${key_path} --exec "/usr/local/bin/renew_certificates.sh" --ca-url ${ca_url} --root ${root_crt_path} --force --expires-in 8h
  elif [ $resource_type = 'ssn' ]; then
    step ca renew ${crt_path} ${key_path} --exec "/usr/local/bin/renew_certificates.sh" --ca-url ${ca_url} --root ${root_crt_path} --force --expires-in 8h && nginx -s reload
  else
    log "Wrong resource type. Aborting..."
    exit 1
  fi
}

function recreate_cert() {
  log "Trying to recreate certificate ${crt_path}"
  step ca token ${cn} --kid ${kid} --ca-url "${ca_url}" --root ${root_crt_path} --password-file ${provisioner_password_path} ${sans} --output-file /tmp/step_token --force
  token=$(cat /tmp/step_token)
  step ca certificate ${cn} ${crt_path} ${key_path} --token "${token}" --kty=RSA --size 2048 --provisioner ${kid} --force
  if [ $resource_type = 'edge' ]; then
    nginx -s reload
  elif [ $resource_type = 'endpoint' ]; then
    /usr/local/bin/renew_certificates.sh
  elif [ $resource_type = 'ssn' ]; then
    /usr/local/bin/renew_certificates.sh
    nginx -s reload
  else
    log "Wrong resource type. Aborting..."
    exit 1
  fi
}
renew_cert
if [ $? -eq 0 ]; then
  log "Certificate ${crt_path} has been renewed or hasn't been expired"
else
  renew_status=1
fi

if [ $renew_status -ne 0 ]; then
  recreate_cert
  if [ $? -eq 0 ]; then
    log "Certificate ${crt_path} has been recreated"
  else
    log "Failed to recreate the certificate ${crt_path}"
  fi
fi