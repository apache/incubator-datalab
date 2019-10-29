#!/bin/sh

/bin/mkdir -p /root/keys

#/usr/bin/aws s3 cp s3://${SSN_BUCKET_NAME}/dlab/certs/ssn/ssn.keystore.jks /root/keys/ssn.keystore.jks
#/usr/bin/aws s3 cp s3://${SSN_BUCKET_NAME}/dlab/certs/ssn/ssn.crt /root/keys/ssn.crt
#/usr/bin/aws s3 cp s3://${SSN_BUCKET_NAME}/dlab/certs/endpoint/endpoint.crt /root/keys/endpoint.crt



#/usr/bin/keytool -importcert -trustcacerts -alias dlab -file /root/keys/ssn.crt -noprompt -storepass changeit -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts
#/usr/bin/keytool -importcert -trustcacerts -file /root/keys/endpoint.crt -noprompt -storepass changeit -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts
#
#if [ -d "/root/step-certs" ]; then
#  /usr/bin/keytool -importcert -trustcacerts -alias step-ca -file /root/step-certs/ca.crt -noprompt -storepass changeit -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts
#  /usr/bin/keytool -importcert -trustcacerts -alias step-crt -file /root/step-certs/tls.crt -noprompt -storepass changeit -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts
#fi



/usr/bin/openssl pkcs12 -export -in /root/step-certs/tls.crt -inkey /root/step-certs/tls.key -name ssn -out ssn.p12 -password pass:${SSN_KEYSTORE_PASSWORD}
/usr/bin/keytool -importkeystore -srckeystore ssn.p12 -srcstoretype PKCS12 -alias ssn -destkeystore /root/keys/ssn.keystore.jks -deststorepass "${SSN_KEYSTORE_PASSWORD}" -srcstorepass "${SSN_KEYSTORE_PASSWORD}"
/usr/bin/keytool -keystore /root/keys/ssn.keystore.jks -alias CARoot -import -file /root/step-certs/ca.crt  -deststorepass "${SSN_KEYSTORE_PASSWORD}" -srcstorepass "${SSN_KEYSTORE_PASSWORD}" -noprompt

/usr/bin/java -Xmx1024M -jar -Duser.timezone=UTC -Dfile.encoding=UTF-8 -DDLAB_CONF_DIR=/root/ /root/self-service-2.1.jar server /root/self-service.yml