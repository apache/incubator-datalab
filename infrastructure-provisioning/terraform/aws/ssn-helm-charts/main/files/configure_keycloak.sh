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

      # 6 spaces needed as this file will be pasted in keycloak_values.yaml by Terraform
      set -x
      auth () {
          RUN=$(/opt/jboss/keycloak/bin/kcadm.sh config credentials --server http://127.0.0.1:8080/auth --realm master \
          --user ${keycloak_user} --password ${keycloak_password} > /dev/null && echo "true" || echo "false")
      }
      check_realm () {
          RUN=$(/opt/jboss/keycloak/bin/kcadm.sh get realms/dlab > /dev/null && echo "true" || echo "false")
      }
      configure_keycloak () {
          # Create Realm
          /opt/jboss/keycloak/bin/kcadm.sh create realms -s realm=dlab -s enabled=true -s loginTheme=dlab \
          -s sslRequired=none
          # Get realm ID
          dlab_realm_id=$(/opt/jboss/keycloak/bin/kcadm.sh get realms/dlab | /usr/bin/jq -r '.id')
          # Create user federation
          /opt/jboss/keycloak/bin/kcadm.sh create components -r dlab -s name=dlab-ldap -s providerId=ldap \
          -s providerType=org.keycloak.storage.UserStorageProvider -s parentId=$dlab_realm_id  -s 'config.priority=["1"]' \
          -s 'config.fullSyncPeriod=["-1"]' -s 'config.changedSyncPeriod=["-1"]' -s 'config.cachePolicy=["DEFAULT"]' \
          -s config.evictionDay=[] -s config.evictionHour=[] -s config.evictionMinute=[] -s config.maxLifespan=[] -s \
          'config.batchSizeForSync=["1000"]' -s 'config.editMode=["READ_ONLY"]' -s 'config.syncRegistrations=["false"]' \
          -s 'config.vendor=["other"]' -s 'config.usernameLDAPAttribute=["${ldap_usernameAttr}"]' \
          -s 'config.rdnLDAPAttribute=["${ldap_rdnAttr}"]' -s 'config.uuidLDAPAttribute=["${ldap_uuidAttr}"]' \
          -s 'config.userObjectClasses=["inetOrgPerson, organizationalPerson"]' \
          -s 'config.connectionUrl=["ldap://${ldap_host}:389"]'  -s 'config.usersDn=["${ldap_users_group},${ldap_dn}"]' \
          -s 'config.authType=["simple"]' -s 'config.bindDn=["${ldap_user},${ldap_dn}"]' \
          -s 'config.bindCredential=["${ldap_bind_creds}"]' -s 'config.searchScope=["1"]' \
          -s 'config.useTruststoreSpi=["ldapsOnly"]' -s 'config.connectionPooling=["true"]' \
          -s 'config.pagination=["true"]' --server http://127.0.0.1:8080/auth
          # Get user federation ID
          user_f_id=$(/opt/jboss/keycloak/bin/kcadm.sh get components -r dlab --query name=dlab-ldap | /usr/bin/jq -er '.[].id')
          # Create user federation email mapper
          /opt/jboss/keycloak/bin/kcadm.sh create components -r dlab -s name=uid-attribute-to-email-mapper \
          -s providerId=user-attribute-ldap-mapper -s providerType=org.keycloak.storage.ldap.mappers.LDAPStorageMapper \
          -s parentId=$user_f_id -s 'config."user.model.attribute"=["email"]' \
          -s 'config."ldap.attribute"=["uid"]' -s 'config."read.only"=["false"]' \
          -s 'config."always.read.value.from.ldap"=["false"]' -s 'config."is.mandatory.in.ldap"=["false"]'
          # Create user federation group mapper
          /opt/jboss/keycloak/bin/kcadm.sh create components -r dlab -s name=group_mapper -s providerId=group-ldap-mapper \
          -s providerType=org.keycloak.storage.ldap.mappers.LDAPStorageMapper -s parentId=$user_f_id \
          -s 'config."groups.dn"=["ou=Groups,${ldap_dn}"]' -s 'config."group.name.ldap.attribute"=["cn"]' \
          -s 'config."group.object.classes"=["posixGroup"]' -s 'config."preserve.group.inheritance"=["false"]' \
          -s 'config."membership.ldap.attribute"=["memberUid"]' -s 'config."membership.attribute.type"=["UID"]' \
          -s 'config."groups.ldap.filter"=[]' -s 'config.mode=["IMPORT"]' \
          -s 'config."user.roles.retrieve.strategy"=["LOAD_GROUPS_BY_MEMBER_ATTRIBUTE"]' \
          -s 'config."mapped.group.attributes"=[]' -s 'config."drop.non.existing.groups.during.sync"=["false"]'
          # Create client
          /opt/jboss/keycloak/bin/kcadm.sh create clients -r dlab -s clientId=dlab-ui -s enabled=true -s \
          'redirectUris=["http://${ssn_k8s_alb_dns_name}/"]' -s secret=${keycloak_client_secret}
          # Get clint ID
          client_id=$(/opt/jboss/keycloak/bin/kcadm.sh get clients -r dlab --query clientId=dlab-ui | /usr/bin/jq -er '.[].id')
          # Create client mapper
          /opt/jboss/keycloak/bin/kcadm.sh create clients/$client_id/protocol-mappers/models \
          -r dlab -s name=group_mapper -s protocol=openid-connect -s protocolMapper="oidc-group-membership-mapper" \
          -s 'config."full.path"="false"' -s 'config."id.token.claim"="true"' -s 'config."access.token.claim"="true"' \
          -s 'config."claim.name"="groups"' -s 'config."userinfo.token.claim"="true"'
      }
      main_func () {
          hostname=$(hostname)
          # Authentication
          count=0
          if [[ $hostname != "keycloak-0" ]];
          then
            echo "Skipping startup script!"
            exit 0
          fi
          while auth
          do
          if [[ $RUN == "false" ]] && (( $count < 120 ));
          then
              echo "Waiting for Keycloak..."
              sleep 5
              count=$((count + 1))
          elif [[ $RUN == "true" ]];
          then
              echo "Authenticated!"
              break
          else
              echo "Timeout error!"
              exit 1
          fi
          done
          # Check if resource is already exist
          check_realm
          # Create resource if it isn't created
          if [[ $RUN == "false" ]];
          then
              configure_keycloak
          else
              echo "Realm is already exist!"
          fi
      }
      main_func &