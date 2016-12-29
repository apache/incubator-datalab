/***************************************************************************

 Copyright (c) 2016, EPAM SYSTEMS INC

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 ****************************************************************************/

package com.epam.dlab.auth.core;

import com.aegisql.conveyor.SmartLabel;

import java.util.function.BiConsumer;

public enum LoginStep implements SmartLabel<UserInfoBuilder> {
    LDAP_LOGIN(UserInfoBuilder::ldapLoginPassed),
    LDAP_USER_INFO(UserInfoBuilder::ldapUserInfo),
    AWS_USER(UserInfoBuilder::awsUser),
    AWS_KEYS(UserInfoBuilder::awsKeys),
    REMOTE_IP(UserInfoBuilder::remoteIp),
    LDAP_USER_INFO_ERROR(UserInfoBuilder::ldapUserInfoError),
    LDAP_GROUP_INFO_ERROR(UserInfoBuilder::ldapGroupInfoError),
    AWS_USER_ERROR(UserInfoBuilder::awsUserError),
    AWS_KEYS_ERROR(UserInfoBuilder::awsKeysError),
    ;
    BiConsumer<UserInfoBuilder, Object> setter;
    <T> LoginStep (BiConsumer<UserInfoBuilder,T> setter) {
        this.setter = (BiConsumer<UserInfoBuilder, Object>) setter;
    }
    @Override
    public BiConsumer<UserInfoBuilder, Object> get() {
        return setter;
    }
}
