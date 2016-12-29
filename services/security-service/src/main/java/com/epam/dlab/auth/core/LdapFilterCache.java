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
import com.aegisql.conveyor.utils.caching.ImmutableReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class LdapFilterCache extends CachingConveyor<String,String,Map<String,Object>> {

    private final static Logger LOG = LoggerFactory.getLogger(LdapFilterCache.class);

    private final static LdapFilterCache INSTANCE = new LdapFilterCache();

    public static LdapFilterCache getInstance() {
        return INSTANCE;
    }

    private LdapFilterCache() {
        super();
        this.setName("LdapFilterCache");
        this.setIdleHeartBeat(1, TimeUnit.SECONDS);
        this.setDefaultCartConsumer((b,l,s)-> LOG.debug("LdapFilterCache consume {} {}",l,s.get()));
    }

    public void removeLdapFilterInfo(String token) {
        this.addCommand(new CancelCommand<>(token));
    }

    public Map<String,Object> getLdapFilterInfo(String token) {
        Supplier<? extends Map<String,Object>> s = this.getProductSupplier(token);
        if( s == null ) {
            return null;
        } else {
            return s.get();
        }
    }

    public void save(String token, Map<String,Object> ldapInfo,long expTimeMsec) {
        CompletableFuture<Boolean> cacheFuture = LdapFilterCache.getInstance().createBuild(token, new ImmutableReference<>(ldapInfo),expTimeMsec,TimeUnit.MILLISECONDS);
        try {
            if(! cacheFuture.get() ) {
                throw new Exception("Cache offer future returned 'false' for "+ldapInfo);
            }
        } catch (Exception e) {
            throw new RuntimeException("Cache offer failed for "+ldapInfo,e);
        }
    }

}
