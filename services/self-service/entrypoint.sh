#!/bin/sh

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

checkfile () {
if [ -s /root/step-certs/ca.crt ]
then
  RUN="true"
else
  RUN="false"
  sleep 5
fi
}

/bin/mkdir -p /root/keys

if [ -d "/root/step-certs" ]; then
  while checkfile
  do
    if [ "$RUN" = "false" ];
    then
        echo "Waiting..."
    else
        echo "CA exist!"
        break
    fi
  done
  /usr/bin/keytool -importcert -trustcacerts -alias step-ca -file /root/step-certs/ca.crt -noprompt -storepass changeit -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts
  /usr/bin/keytool -importcert -trustcacerts -alias step-crt -file /root/step-certs/tls.crt -noprompt -storepass changeit -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts
fi



/usr/bin/openssl pkcs12 -export -in /root/step-certs/tls.crt -inkey /root/step-certs/tls.key -name ssn -out ssn.p12 -password pass:${SSN_KEYSTORE_PASSWORD}
/usr/bin/keytool -importkeystore -srckeystore ssn.p12 -srcstoretype PKCS12 -alias ssn -destkeystore /root/keys/ssn.keystore.jks -deststorepass "${SSN_KEYSTORE_PASSWORD}" -srcstorepass "${SSN_KEYSTORE_PASSWORD}"
/usr/bin/keytool -keystore /root/keys/ssn.keystore.jks -alias step-ca -import -file /root/step-certs/ca.crt  -deststorepass "${SSN_KEYSTORE_PASSWORD}" -srcstorepass "${SSN_KEYSTORE_PASSWORD}" -noprompt
/usr/bin/java -Xmx2048M -jar -Duser.timezone=UTC -Dfile.encoding=UTF-8 -DDATALAB_CONF_DIR=/root/ /root/self-service-2.4.jar server /root/self-service.yml