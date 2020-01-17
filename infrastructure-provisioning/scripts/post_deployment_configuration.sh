#!/bin/bash

server_external_ip=$(curl -H "Metadata-Flavor: Google" http://metadata/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip)
sed -i "s|SERVER_IP|$server_external_ip|g" /etc/nginx/conf.d/nginx_proxy.conf

dlab_sbn=$(curl -H "Metadata-Flavor: Google" http://metadata/computeMetadata/v1/instance/name)

KEYCLOAK_REDIRECTURI='http://'$server_external_ip
KEYCLOAK_REALM_NAME='dlab'
KEYCLOAK_AUTH_SERVER_URL='https://idp.demo.dlabanalytics.com/auth'
KEYCLOAK_CLIENT_NAME=$dlab_sbn'-ui'
KEYCLOAK_CLIENT_SECRET=$(uuidgen)

sed -i "s|KEYCLOAK_REDIRECTURI|$KEYCLOAK_REDIRECTURI|g" /opt/dlab/conf/self-service.yml
sed -i "s|KEYCLOAK_REALM_NAME|$KEYCLOAK_REALM_NAME|g" /opt/dlab/conf/self-service.yml
sed -i "s|KEYCLOAK_AUTH_SERVER_URL|$KEYCLOAK_AUTH_SERVER_URL|g" /opt/dlab/conf/self-service.yml
sed -i "s|KEYCLOAK_CLIENT_NAME|$KEYCLOAK_CLIENT_NAME|g" /opt/dlab/conf/self-service.yml
sed -i "s|KEYCLOAK_CLIENT_SECRET|$KEYCLOAK_CLIENT_SECRET|g" /opt/dlab/conf/self-service.yml

sed -i "s|KEYCLOAK_REALM_NAME|$KEYCLOAK_REALM_NAME|g" /opt/dlab/conf/provisioning.yml
sed -i "s|KEYCLOAK_AUTH_SERVER_URL|$KEYCLOAK_AUTH_SERVER_URL|g" /opt/dlab/conf/provisioning.yml
sed -i "s|KEYCLOAK_CLIENT_NAME|$KEYCLOAK_CLIENT_NAME|g" /opt/dlab/conf/provisioning.yml
sed -i "s|KEYCLOAK_CLIENT_SECRET|$KEYCLOAK_CLIENT_SECRET|g" /opt/dlab/conf/provisioning.yml

ssn_subnetId=$



sed -i "s|DLAB_SBN|$dlab_sbn|g" /opt/dlab/conf/provisioning.yml
sed -i "s|DLAB_SBN|$dlab_sbn|g" /opt/dlab/conf/provisioning.yml