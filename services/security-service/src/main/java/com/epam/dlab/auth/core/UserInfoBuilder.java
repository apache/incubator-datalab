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

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Testing;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.epam.dlab.auth.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

public class UserInfoBuilder implements Supplier<UserInfo>, Testing {

    private static final Logger LOG = LoggerFactory.getLogger(UserInfoBuilder.class);

    private UserInfo userInfo;

    private RuntimeException ldapError      = null;
    private RuntimeException ldapGroupError = null;
    private RuntimeException awsUserError   = null;
    private RuntimeException awsKeyError    = null;


    private int readinessStatus = 0b00000000;

    private static final int FIRST_NAME      = 0b0000001;
    private static final int LAST_NAME       = 0b0000010;
    private static final int AWS_USER_SET    = 0b0000100;
    private static final int ROLE_SET        = 0b0001000;
    private static final int REMOTE_IP       = 0b0010000;
    private static final int AWS_KEYS        = 0b0100000;
    private static final int LOGIN           = 0b1000000;

    private static final int READYNESS_MASK  = 0b1111111;

    public static boolean testMask(Supplier<? extends UserInfo> supplier, int mask) {
        UserInfoBuilder builder = (UserInfoBuilder) supplier;
        LOG.debug("testing {} vs {} = {}",builder.readinessStatus,mask,(builder.readinessStatus & mask) == mask);
        return (builder.readinessStatus & mask) == mask;
    }

    public void setMask(int mask) {
        this.readinessStatus |= mask;
    }

    public static BuilderSupplier<UserInfo> supplier(final String token, final String username ) {
        LOG.debug("supplier requested {} {}",token, username);
        return () -> new UserInfoBuilder(token,username);
    }

    public static void ldapLoginPassed(UserInfoBuilder b) {
        b.setMask( LOGIN );
    }

    public static void firstName(UserInfoBuilder b, String firstName) {
        LOG.debug("firstName {}",firstName);

        b.userInfo.setFirstName(firstName);
        b.setMask( FIRST_NAME );
    }

    public static void lastName(UserInfoBuilder b, String lastName) {
        LOG.debug("lastName {}",lastName);

        b.userInfo.setLastName(lastName);
        b.setMask( LAST_NAME );
    }

    public static void remoteIp(UserInfoBuilder b, String remoteIp) {
        LOG.debug("remoteIp {}",remoteIp);

        b.userInfo.setRemoteIp(remoteIp);
        b.setMask( REMOTE_IP );
    }

    public static void awsUser(UserInfoBuilder b, Boolean awsUser) {
        LOG.debug("awsUser {}",awsUser);

        b.userInfo.setAwsUser(awsUser);
        b.setMask( AWS_USER_SET );
    }

    public static void roles(UserInfoBuilder b, Collection<String> roles) {
        LOG.debug("roles {}",roles);
        roles.forEach( role -> b.userInfo.addRole(role) );
        b.setMask( ROLE_SET );
    }

    public static void ldapUserInfo(UserInfoBuilder b, UserInfo ui) {
        LOG.debug("merge user info{}",ui);
        UserInfoBuilder.firstName(b,ui.getFirstName());
        UserInfoBuilder.lastName(b,ui.getLastName());
        UserInfoBuilder.roles(b,ui.getRoles());
    }

    public UserInfoBuilder(String token, String username) {
        this.userInfo = new UserInfo(username, token);
    }

    public UserInfoBuilder() {

    }

    @Override
    public UserInfo get() {
        if( ldapError != null )      throw ldapError;
        if( ldapGroupError != null ) throw ldapGroupError;
        if( awsUserError != null )   throw awsUserError;
        if( awsKeyError != null )    throw awsKeyError;
        return userInfo;
    }

    @Override
    public String toString() {
        return "UserInfoBuilder{" +
                "userInfo=" + userInfo +
                ", readinessStatus=" + readinessStatus +
                '}';
    }

    @Override
    public boolean test() {
        return UserInfoBuilder.testMask(this,UserInfoBuilder.READYNESS_MASK);
    }

    public static void awsKeys(UserInfoBuilder b, List<AccessKeyMetadata> keyMetadata) {
        LOG.debug("AWS Keys {}",keyMetadata);
        LongAdder counter = new LongAdder();
        if(keyMetadata != null) {
            keyMetadata.forEach(k -> {
                String key = k.getAccessKeyId();
                String status = k.getStatus();
                if ("Active".equalsIgnoreCase(status)) {
                    counter.increment();
                }
                b.userInfo.addKey(key, status);
            });
        }

        if( counter.intValue() == 0 ) {
            b.awsKeyError = new RuntimeException("Please contact AWS administrator to activate your Access Key");
        }
        b.setMask( AWS_KEYS );
    }

    public static void awsKeysEmpty(UserInfoBuilder b, List<AccessKeyMetadata> keyMetadata) {
        LOG.debug("AWS Keys {}",keyMetadata);
        b.setMask( AWS_KEYS );
    }

    public static void ldapUserInfoError(UserInfoBuilder b, RuntimeException t) {
        LOG.error("ldapUserInfoError {}", t.getMessage());
        b.ldapError = t;
        b.setMask( LOGIN );
    }

    public static void ldapGroupInfoError(UserInfoBuilder b, RuntimeException t) {
        LOG.error("ldapGroupInfoError {}", t.getMessage());
        b.ldapGroupError = t;
        b.setMask( FIRST_NAME | LAST_NAME | ROLE_SET );
    }

    public static void awsUserError(UserInfoBuilder b, RuntimeException t) {
        LOG.error("awsUserError {}", t.getMessage());
        b.awsUserError = t;
        b.setMask( AWS_USER_SET );
    }

    public static void awsKeysError(UserInfoBuilder b, RuntimeException t) {
        LOG.error("awsKeysError {}", t.getMessage());
        b.awsKeyError = t;
        b.setMask( AWS_KEYS );
    }

}
