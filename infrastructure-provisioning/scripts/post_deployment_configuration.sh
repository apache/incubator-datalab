#!/bin/bash

server_external_ip=$(curl -H "Metadata-Flavor: Google" http://metadata/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip)
sed -i "s|SERVER_IP|$server_external_ip|g" /etc/nginx/conf.d/nginx_proxy.conf
systemctl restart nginx

dlab_sbn=$(curl -H "Metadata-Flavor: Google" http://metadata/computeMetadata/v1/instance/name)

KEYCLOAK_REDIRECTURI='http://'$server_external_ip
KEYCLOAK_REALM_NAME='dlab'
KEYCLOAK_AUTH_SERVER_URL='https://idp.demo.dlabanalytics.com/auth'
KEYCLOAK_CLIENT_NAME=$dlab_sbn'-ui'
KEYCLOAK_CLIENT_SECRET='e235f2b6-a5e0-448a-837d-465d1a4990f7'
KEYCLOAK_USER='admin'
KEYCLOAK_USER_PASSWORD='v7rdj2ckHgAdJj54'

sed -i "s|DLAB_SBN|$dlab_sbn|g" /opt/dlab/conf/self-service.yml
sed -i "s|KEYCLOAK_REDIRECTURI|$KEYCLOAK_REDIRECTURI|g" /opt/dlab/conf/self-service.yml
sed -i "s|KEYCLOAK_REALM_NAME|$KEYCLOAK_REALM_NAME|g" /opt/dlab/conf/self-service.yml
sed -i "s|KEYCLOAK_AUTH_SERVER_URL|$KEYCLOAK_AUTH_SERVER_URL|g" /opt/dlab/conf/self-service.yml
sed -i "s|KEYCLOAK_CLIENT_NAME|$KEYCLOAK_CLIENT_NAME|g" /opt/dlab/conf/self-service.yml
sed -i "s|KEYCLOAK_CLIENT_SECRET|$KEYCLOAK_CLIENT_SECRET|g" /opt/dlab/conf/self-service.yml
sed -i "s|KEYCLOAK_REALM_NAME|$KEYCLOAK_REALM_NAME|g" /opt/dlab/conf/provisioning.yml
sed -i "s|KEYCLOAK_AUTH_SERVER_URL|$KEYCLOAK_AUTH_SERVER_URL|g" /opt/dlab/conf/provisioning.yml
sed -i "s|KEYCLOAK_CLIENT_NAME|$KEYCLOAK_CLIENT_NAME|g" /opt/dlab/conf/provisioning.yml
sed -i "s|KEYCLOAK_CLIENT_SECRET|$KEYCLOAK_CLIENT_SECRET|g" /opt/dlab/conf/provisioning.yml

ssn_subnetId=$(sudo gcloud compute instances describe $dlab_sbn --zone us-west1-a | awk -F/ '/subnetwork: / {print $11}')
dlab_zone=$(curl -H "Metadata-Flavor: Google" http://metadata/computeMetadata/v1/instance/zone | awk -F/ '{print $4}')
dlab_region=$(echo $dlab_zone | awk '{print substr($0, 1, length($0)-2)}')
ssn_vpcId=$(curl -H "Metadata-Flavor: Google" http://metadata/computeMetadata/v1/instance/network-interfaces/0/network | awk -F/ '{print $4}')
gcp_projectId=$(curl -H "Metadata-Flavor: Google" http://metadata/computeMetadata/v1/project/project-id)

sed -i "s|DLAB_SBN|$dlab_sbn|g" /opt/dlab/conf/provisioning.yml
sed -i "s|SUBNET_ID|$ssn_subnetId|g" /opt/dlab/conf/provisioning.yml
sed -i "s|DLAB_REGION|$dlab_region|g" /opt/dlab/conf/provisioning.yml
sed -i "s|DLAB_ZONE|$dlab_zone|g" /opt/dlab/conf/provisioning.yml
sed -i "s|SSN_VPC_ID|$ssn_vpcId|g" /opt/dlab/conf/provisioning.yml
sed -i "s|GCP_PROJECT_ID|$gcp_projectId|g" /opt/dlab/conf/provisioning.yml
sed -i "s|KEYCLOAK_USER|$KEYCLOAK_USER|g" /opt/dlab/conf/provisioning.yml
sed -i "s|KEYCLOAK_USER_PASSWORD|$KEYCLOAK_USER_PASSWORD|g" /opt/dlab/conf/provisioning.yml

sed -i "s|DLAB_SBN|$dlab_sbn|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini
sed -i "s|GCP_PROJECT_ID|$gcp_projectId|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini
sed -i "s|DLAB_REGION|$dlab_region|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini
sed -i "s|DLAB_ZONE|$dlab_zone|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini
sed -i "s|KEYCLOAK_REALM_NAME|$KEYCLOAK_REALM_NAME|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini
sed -i "s|KEYCLOAK_AUTH_SERVER_URL|$KEYCLOAK_AUTH_SERVER_URL|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini
sed -i "s|KEYCLOAK_CLIENT_NAME|$KEYCLOAK_CLIENT_NAME|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini
sed -i "s|KEYCLOAK_CLIENT_SECRET|$KEYCLOAK_CLIENT_SECRET|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini
sed -i "s|KEYCLOAK_USER|$KEYCLOAK_USER|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini
sed -i "s|KEYCLOAK_USER_PASSWORD|$KEYCLOAK_USER_PASSWORD|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini

supervisorctl restart all

cd /opt/dlab/sources/infrastructure-provisioning/src/ && docker-build all