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

KEYSTORE_PASS=$(cat /opt/datalab/conf/CONF_FILE.yml  | grep '<#assign KEY_STORE_PASSWORD' | awk -F  '\"' '{print $2}')

# Removing old certificates
keytool -delete -alias RESOURCE_TYPE -keystore /home/OS_USER/keys/RESOURCE_TYPE.keystore.jks -storepass "${KEYSTORE_PASS}"
keytool -delete -alias step-ca -keystore /home/OS_USER/keys/RESOURCE_TYPE.keystore.jks -storepass "${KEYSTORE_PASS}"
keytool -delete -alias step-ca -keystore JAVA_HOME/lib/security/cacerts -storepass changeit
keytool -delete -alias RESOURCE_TYPE -keystore JAVA_HOME/lib/security/cacerts -storepass changeit

# Importing new certificates to keystore
openssl pkcs12 -export -in /etc/ssl/certs/datalab.crt -inkey /etc/ssl/certs/datalab.key -name RESOURCE_TYPE -out /home/OS_USER/keys/RESOURCE_TYPE.p12 -password pass:${KEYSTORE_PASS}
keytool -importkeystore -srckeystore /home/OS_USER/keys/RESOURCE_TYPE.p12 -srcstoretype PKCS12 -alias RESOURCE_TYPE -destkeystore /home/OS_USER/keys/RESOURCE_TYPE.keystore.jks -deststorepass "${KEYSTORE_PASS}" -srcstorepass "${KEYSTORE_PASS}"
keytool -keystore /home/OS_USER/keys/RESOURCE_TYPE.keystore.jks -alias step-ca -import -file  /etc/ssl/certs/root_ca.crt  -deststorepass "${KEYSTORE_PASS}" -noprompt


# Adding new certificates
keytool -importcert -trustcacerts -alias RESOURCE_TYPE -file /etc/ssl/certs/datalab.crt -noprompt -storepass changeit -keystore JAVA_HOME/lib/security/cacerts
keytool -importcert -trustcacerts -alias step-ca -file /etc/ssl/certs/root_ca.crt -noprompt -storepass changeit -keystore JAVA_HOME/lib/security/cacerts

# Restarting service
supervisorctl restart all