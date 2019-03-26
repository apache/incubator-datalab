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

USAGE="Usage: /bin/bash script.sh admin_pass service_user service_pass initial_pass ldap_dn ldap_dc \nAll parameters should be passed!"

if [[ $# == 0 ]]; then
    echo -e ${USAGE}
    exit 1;
fi
if [[ $# != 6 ]]; then
    echo -e ${USAGE}
    exit 1;
fi


# Password to LDAP admin user
ADMIN_USER_PASS=$1
# Name of service user
SERVICE_USER_NAME=$2
# Password for LDAP service user
SERVICE_USER_PASS=$3
# Password for initial user
INITIAL_USER_PASS=$4
# LDAP DN
LDAP_DN=$5
# LDAP DC
LDAP_DC=$6

# Installing LDAP
apt-get update
export DEBIAN_FRONTEND=noninteractive
debconf-set-selections <<< "slapd/root_password password ${ADMIN_USER_PASS}"
debconf-set-selections <<< "slapd/root_password_again password ${ADMIN_USER_PASS}"
apt-get install -y slapd ldap-utils


cat > db.ldif << 'EOF'
dn: olcDatabase={1}mdb,cn=config
changetype: modify
replace: olcSuffix
olcSuffix: LDAP_DN

dn: olcDatabase={1}mdb,cn=config
changetype: modify
replace: olcRootDN
olcRootDN: cn=admin,LDAP_DN

dn: olcDatabase={1}mdb,cn=config
changetype: modify
replace: olcRootPW
olcRootPW: PASSWORD
EOF

cat > monitor.ldif << 'EOF'
dn: olcDatabase={1}monitor,cn=config
changetype: modify
replace: olcAccess
olcAccess: {0}to * by dn.base="gidNumber=0+uidNumber=0,cn=peercred,cn=external, cn=auth" read by dn.base="cn=admin,LDAP_DN" read by * none
EOF

cat > base.ldif << 'EOF'
dn: LDAP_DN
dc: LDAP_DC
objectClass: top
objectClass: domain

dn: cn=admin,LDAP_DN
objectClass: organizationalRole
cn: admin
description: LDAP Manager

dn: ou=People,LDAP_DN
objectClass: organizationalUnit
ou: People

dn: ou=Group,LDAP_DN
objectClass: organizationalUnit
ou: Group

dn: cn=Read-Only,ou=Group,LDAP_DN
objectClass: posixGroup
objectClass: top
gidNumber: 500
cn: Read-Only

dn: cn=initial,ou=Group,LDAP_DN
objectClass: posixGroup
objectClass: top
gidNumber: 501
cn: initial

dn: cn=initial-user,ou=People,LDAP_DN
objectClass: top
objectClass: account
objectClass: posixAccount
objectClass: shadowAccount
cn: initial-user
uid: initial-user
uidNumber: 1001
gidNumber: 501
gecos: initial-user
homeDirectory: /home/initial-user
userPassword: {crypt}x
shadowLastChange: 0
shadowMax: 0
shadowWarning: 0

dn: cn=LDAP_AUTH_USER,LDAP_DN
objectClass: top
objectClass: account
objectClass: posixAccount
objectClass: shadowAccount
cn: LDAP_AUTH_USER
uid: LDAP_AUTH_USER
uidNumber: 1000
gidNumber: 500
gecos: LDAP_AUTH_USER
homeDirectory: /home/LDAP_AUTH_USER
userPassword: {crypt}x
shadowLastChange: 0
shadowMax: 0
shadowWarning: 0
EOF

cat > disable_anonymous.ldif << 'EOF'
dn: cn=config
changetype: modify
add: olcDisallows
olcDisallows: bind_anon

dn: cn=config
changetype: modify
add: olcRequires
olcRequires: authc

dn: olcDatabase={-1}frontend,cn=config
changetype: modify
add: olcRequires
olcRequires: authc
EOF

cat > access.ldif << 'EOF'
dn: olcDatabase={1}mdb,cn=config
changetype: modify
add: olcAccess
olcAccess: {3}to * by dn="cn=LDAP_AUTH_USER,LDAP_DN" read by * read

EOF
password=`slappasswd -s ${ADMIN_USER_PASS}`
sed -i "s|PASSWORD|$password|g" db.ldif
sed -i "s|LDAP_DN|${LDAP_DN}|g" db.ldif
ldapmodify -Y EXTERNAL  -H ldapi:/// -f db.ldif
sed -i "s|LDAP_DN|${LDAP_DN}|g" monitor.ldif
#ldapadd -Y EXTERNAL  -H ldapi:/// -f monitor.ldif
cp /usr/share/slapd/DB_CONFIG /var/lib/ldap/DB_CONFIG
chown openldap:openldap /var/lib/ldap/*
#ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/ldap/schema/cosine.ldif
#ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/ldap/schema/nis.ldif
#ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/ldap/schema/inetorgperson.ldif
sed -i "s|LDAP_DN|${LDAP_DN}|g" base.ldif
sed -i "s|LDAP_DC|${LDAP_DC}|g" base.ldif
sed -i "s|LDAP_AUTH_USER|${SERVICE_USER_NAME}|g" base.ldif
ldapadd -x -w ${ADMIN_USER_PASS} -D "cn=admin,${LDAP_DN}" -f base.ldif
ldapadd -Y EXTERNAL -H ldapi:/// -f disable_anonymous.ldif
sed -i "s|LDAP_DN|${LDAP_DN}|g" access.ldif
sed -i "s|LDAP_AUTH_USER|${SERVICE_USER_NAME}|g" access.ldif
ldapmodify  -Y EXTERNAL -H ldapi:/// -f access.ldif
ldappasswd -s ${SERVICE_USER_PASS} -w ${ADMIN_USER_PASS} -D "cn=admin,${LDAP_DN}" -x "cn=${SERVICE_USER_NAME},${LDAP_DN}"
ldappasswd -s ${INITIAL_USER_PASS} -w ${ADMIN_USER_PASS} -D "cn=admin,${LDAP_DN}" -x "cn=initial-user,ou=People,${LDAP_DN}"

# Installing and configuring PHPLDAPAdmin
apt-get install -y phpldapadmin
sed -i "s@$servers->setValue('server','name'.*@$servers->setValue('server','name','Lilly LDAP');@" /etc/phpldapadmin/config.php
sed -i "s@$servers->setValue('server','base'.*@$servers->setValue('server','base',array('${LDAP_DN}'));@" /etc/phpldapadmin/config.php
sed -i "s@$servers->setValue('login','bind_id'.*@$servers->setValue('login','bind_id','cn=admin,${LDAP_DN}');@" /etc/phpldapadmin/config.php

# Installing and configuring Self-Service-Password application
apt-get -y install php php7.0 php7.0-mbstring
wget https://ltb-project.org/archives/self-service-password_1.2-1_all.deb -O self-service-password_1.2-1_all.deb
dpkg -i self-service-password_1.2-1_all.deb
cat > config.inc.php << 'EOF'
<?php
$debug = false;
$ldap_url = "ldap://localhost";
$ldap_starttls = false;
$ldap_binddn = "cn=LDAP_AUTH_USER,LDAP_DN";
$ldap_bindpw = "AUTH_USER_PASS";
$ldap_base = "LDAP_DN";
$ldap_login_attribute = "uid";
$ldap_fullname_attribute = "cn";
$ldap_filter = "(&(objectClass=posixAccount)(uid={login}))";
$ad_mode = false;
$samba_mode = false;
$hash = "SHA";
$hash_options['crypt_salt_prefix'] = "$6$";
$hash_options['crypt_salt_length'] = "6";
$pwd_min_length = 6;
$pwd_max_length = 0;
$pwd_min_lower = 0;
$pwd_min_upper = 1;
$pwd_min_digit = 1;
$pwd_min_special = 0;
$pwd_special_chars = "^a-zA-Z0-9";
$pwd_no_reuse = true;
$pwd_diff_login = true;
$pwd_complexity = 0;
$pwd_show_policy = "never";
$pwd_show_policy_pos = "above";
$who_change_password = "user";
$use_change = true;
$change_sshkey = false;
$change_sshkey_attribute = "sshPublicKey";
$who_change_sshkey = "user";
$notify_on_sshkey_change = false;
$use_questions = false;
$answer_objectClass = "extensibleObject";
$answer_attribute = "info";
$use_tokens = false;
$crypt_tokens = true;
$token_lifetime = "3600";
$use_sms = false;
$keyphrase = "V)F74:zqpv,aLY$%";
$show_help = true;
$lang = "en";
$allowed_lang = array();
$show_menu = true;
$login_forbidden_chars = "*()&|";
$use_recaptcha = false;
$default_action = "change";
EOF
sed -i "s|LDAP_AUTH_USER|${SERVICE_USER_NAME}|g; s|LDAP_DN|${LDAP_DN}|g; s|AUTH_USER_PASS|${SERVICE_USER_PASS}|g;" config.inc.php
cp -f config.inc.php /usr/share/self-service-password/conf/config.inc.php

# Configuring Apache
rm /etc/apache2/sites-enabled/000-default.conf
cat > applications.conf << 'EOF'
<Directory /usr/share/phpldapadmin/htdocs>
  <IfModule mod_authz_core.c>
    # Apache 2.4
    Require all granted
  </IfModule>
  <IfModule !mod_authz_core.c>
    # Apache 2.2
    Order Deny,Allow
    Deny from all
    Allow from 127.0.0.1
    Allow from ::1
  </IfModule>
</Directory>

<Directory /usr/share/self-service-password>
    AllowOverride None
    <IfVersion >= 2.3>
        Require all granted
    </IfVersion>
    <IfVersion < 2.3>
        Order Deny,Allow
        Allow from all
    </IfVersion>
</Directory>

<VirtualHost *:80>
    DocumentRoot /usr/share/phpldapadmin/htdocs
    Alias /ssp /usr/share/self-service-password
    Alias /phpldapadmin /usr/share/phpldapadmin/htdocs
</VirtualHost>

EOF
cp -f applications.conf /etc/apache2/sites-enabled/applications.conf
systemctl restart apache2

# Printing result
public_ip=`curl -sS http://169.254.169.254/latest/meta-data/ | grep public-ipv4`
if [[ $? == 0 ]];then
 ip_address=`curl -sS http://169.254.169.254/latest/meta-data/public-ipv4`
else
 ip_address=`curl -sS http://169.254.169.254/latest/meta-data/local-ipv4`
fi
echo "#--------------------------------------------------------------------#"
echo "#----------------------Configuration completed-----------------------#"
echo "#---------Go to http://${ip_address}/ to access PHPLDAPAdmin---------#"
echo "#---Go to http://${ip_address}/ssp to access Self-Service-Password---#"
echo "#--------------------------------------------------------------------#"