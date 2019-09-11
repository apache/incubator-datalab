#!/bin/sh

mkdir -p /root/keys
keytool -genkeypair -alias dlab -keyalg RSA -validity 730 -storepass password \
  -keypass password -keystore /root/keys/ssn.keystore.jks \
  -keysize 2048 -dname "CN=localhost"
keytool -exportcert -alias dlab -storepass password -file /root/keys/ssn.crt \
  -keystore /root/keys/ssn.keystore.jks

/usr/bin/java -Xmx1024M -jar -Duser.timezone=UTC -Dfile.encoding=UTF-8 -DDLAB_CONF_DIR=/root/ /root/self-service-2.1.jar server /root/self-service.yml