#!/bin/bash
KEYSTORE_PASS=$(cat /opt/dlab/conf/provisioning.yml  | grep '<#assign KEY_STORE_PASSWORD' | awk -F  '\"' '{print $2}')

# Removing old certificates
keytool -delete -alias endpoint -keystore /home/OS_USER/keys/endpoint.keystore.jks -storepass "${KEYSTORE_PASS}"
keytool -delete -alias CARoot -keystore /home/OS_USER/keys/endpoint.keystore.jks -storepass "${KEYSTORE_PASS}"
keytool -delete -alias mykey -keystore JAVA_HOME/lib/security/cacerts -storepass changeit
keytool -delete -alias endpoint -keystore JAVA_HOME/lib/security/cacerts -storepass changeit

# Importing new certificates to keystore
openssl pkcs12 -export -in /home/OS_USER/keys/endpoint.crt -inkey /home/OS_USER/keys/endpoint.key -name endpoint -out /home/OS_USER/keys/endpoint.p12 -password pass:${KEYSTORE_PASS}
keytool -importkeystore -srckeystore /home/OS_USER/keys/endpoint.p12 -srcstoretype PKCS12 -alias endpoint -destkeystore /home/OS_USER/keys/endpoint.keystore.jks -deststorepass "${KEYSTORE_PASS}" -srcstorepass "${KEYSTORE_PASS}"
keytool -keystore /home/OS_USER/keys/endpoint.keystore.jks -alias CARoot -import -file  /home/OS_USER/keys/root_ca.crt  -deststorepass "${KEYSTORE_PASS}" -noprompt


# Adding new certificates
keytool -importcert -trustcacerts -alias endpoint -file /home/OS_USER/keys/endpoint.crt -noprompt -storepass changeit -keystore JAVA_HOME/lib/security/cacerts
keytool -importcert -trustcacerts -file /home/OS_USER/keys/root_ca.crt -noprompt -storepass changeit -keystore JAVA_HOME/lib/security/cacerts

# Restarting service
supervisorctl restart provserv