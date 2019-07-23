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

    # 4 spaces needed as this file will be pasted in keycloak_values.yaml by Terraform
    set -x
    auth () {
        RUN=`/opt/jboss/keycloak/bin/kcadm.sh config credentials --server http://127.0.0.1:8080/auth --realm master \
        --user dlab-admin --password 12345o > /dev/null && echo "true" || echo "false"`
    }
    check_realm () {
        RUN=`/opt/jboss/keycloak/bin/kcadm.sh get realms/dlab > /dev/null && echo "true" || echo "false"`
    }
    configure_keycloak () {
        # Create Realm
        /opt/jboss/keycloak/bin/kcadm.sh create realms -s realm=dlab -s enabled=true
        # Get realm ID
        dlab_realm_id=`/opt/jboss/keycloak/bin/kcadm.sh get realms/dlab | /usr/bin/jq -r '.id'`
        # Create user federation
        /opt/jboss/keycloak/bin/kcadm.sh create components -r dlab -s name=dlab-ldap -s providerId=ldap \
        -s providerType=org.keycloak.storage.UserStorageProvider -s parentId=$dlab_realm_id  -s 'config.priority=["1"]' \
        -s 'config.fullSyncPeriod=["-1"]' -s 'config.changedSyncPeriod=["-1"]' -s 'config.cachePolicy=["DEFAULT"]' \
        -s config.evictionDay=[] -s config.evictionHour=[] -s config.evictionMinute=[] -s config.maxLifespan=[] -s \
        'config.batchSizeForSync=["1000"]' -s 'config.editMode=["WRITABLE"]' -s 'config.syncRegistrations=["false"]' \
        -s 'config.vendor=["other"]' -s 'config.usernameLDAPAttribute=["uid"]' -s 'config.rdnLDAPAttribute=["uid"]' \
        -s 'config.uuidLDAPAttribute=["entryUUID"]' -s 'config.userObjectClasses=["inetOrgPerson, organizationalPerson"]' \
        -s 'config.connectionUrl=["ldap://52.36.63.161:389"]'  -s 'config.usersDn=["ou=People,dc=example,dc=com"]' \
        -s 'config.authType=["simple"]' -s 'config.bindDn=["cn=admin,dc=example,dc=com"]' \
        -s 'config.bindCredential=["LdaPStr0ngPaSSw0rd123"]' -s 'config.searchScope=["1"]' \
        -s 'config.useTruststoreSpi=["ldapsOnly"]' -s 'config.connectionPooling=["true"]' -s 'config.pagination=["true"]'  \
        --server http://127.0.0.1:8080/auth
        # Get user federation ID
        user_f_id=`/opt/jboss/keycloak/bin/kcadm.sh get components -r dlab --query name=dlab-ldap | /usr/bin/jq -er '.[].id'`
        # Create user federation mapper
        /opt/jboss/keycloak/bin/kcadm.sh create components -r dlab -s name=uid-attribute-to-email-mapper \
        -s providerId=user-attribute-ldap-mapper -s providerType=org.keycloak.storage.ldap.mappers.LDAPStorageMapper \
        -s parentId=$user_f_id -s 'config."user.model.attribute"=["email"]' \
        -s 'config."ldap.attribute"=["uid"]' -s 'config."read.only"=["false"]' \
        -s 'config."always.read.value.from.ldap"=["false"]' -s 'config."is.mandatory.in.ldap"=["false"]'
        # Create client
        /opt/jboss/keycloak/bin/kcadm.sh create clients -r dlab -s clientId=dlab-ui -s enabled=true -s \
        'redirectUris=["http://dlab-ui:58080/"]'
    }
    main_func () {
        # Authentication
        count=0
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