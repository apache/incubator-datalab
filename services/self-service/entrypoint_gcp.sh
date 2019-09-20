#!/bin/sh

#mkdir -p /root/keys
#/usr/bin/keytool -genkeypair -alias dlab -keyalg RSA -validity 730 -storepass password \
#  -keypass password -keystore /root/keys/ssn.keystore.jks \
#  -keysize 2048 -dname "CN=35.237.224.151" -ext SAN=dns:localhost,ip:35.237.224.151
#/usr/bin/keytool -exportcert -alias dlab -storepass password -file /root/keys/ssn.crt \
#  -keystore /root/keys/ssn.keystore.jks

/usr/bin/keytool -importcert -trustcacerts -alias dlab -file /root/keys/ssn.crt -noprompt -storepass changeit -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts
/usr/bin/keytool -importcert -trustcacerts -alias endpoint1 -file /root/keys/endpoint1.crt -noprompt -storepass changeit -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts
/usr/bin/keytool -importcert -trustcacerts -alias endpoint2 -file /root/keys/endpoint2.crt -noprompt -storepass changeit -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts

/usr/bin/java -Xmx1024M -jar -Duser.timezone=UTC -Dfile.encoding=UTF-8 -DDLAB_CONF_DIR=/root/ /root/self-service-2.1.jar server /root/self-service.yml