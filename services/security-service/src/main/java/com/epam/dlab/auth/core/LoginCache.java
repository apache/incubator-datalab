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

import com.aegisql.conveyor.cart.command.CancelCommand;
import com.aegisql.conveyor.utils.caching.CachingConveyor;
import com.epam.dlab.auth.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class LoginCache extends CachingConveyor<String,String,UserInfo> {

    private final static Logger LOG = LoggerFactory.getLogger(LoginCache.class);

    private final static LoginCache INSTANCE = new LoginCache();

    public static LoginCache getInstance() {
        return INSTANCE;
    }

    private LoginCache() {
        super();
        this.setName("UserInfoCache");
        this.setIdleHeartBeat(1, TimeUnit.SECONDS);
        this.setDefaultBuilderTimeout(60, TimeUnit.MINUTES);
        this.enablePostponeExpirationOnTimeout(false);
        this.enablePostponeExpiration(true);
        this.setExpirationPostponeTime(60,TimeUnit.MINUTES);
        this.setDefaultCartConsumer((b,l,s)-> LOG.debug("UserInfoCache consume {} {}",l,s.get()));
        this.setOnTimeoutAction((s)->{
            LOG.trace("UserInfoCache Timeout {}",s.get());
        });
        this.setScrapConsumer(bin->{
            LOG.debug("UserInfoCache {}: {}", bin.failureType, bin.scrap);
        });
    }

    public void removeUserInfo(String token) {
        this.addCommand(new CancelCommand<>(token));
    }

    public UserInfo getUserInfo(String token) {
        Supplier<? extends UserInfo> s = this.getProductSupplier(token);
        if( s == null ) {
            return null;
        } else {
            return s.get();
        }
    }

    public void save(UserInfo userInfo) {
        CompletableFuture<Boolean> cacheFuture = LoginCache.getInstance().createBuild(userInfo.getAccessToken(), CacheableReference.newInstance(userInfo));
        try {
            if(! cacheFuture.get() ) {
                throw new Exception("Offer future returned 'false' for "+userInfo);
            }
        } catch (Exception e) {
            throw new RuntimeException("User Info cache offer failure for "+userInfo,e);
        }
    }

}
