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

import com.aegisql.conveyor.utils.parallel.KBalancedParallelConveyor;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LoginConveyor extends KBalancedParallelConveyor<String,LoginStep,UserInfo>{

    private final static Logger LOG = LoggerFactory.getLogger(LoginConveyor.class);

    private UserInfoDAO userInfoDao;

    public LoginConveyor() {
        super(4);
        this.setName("LoginConveyor");
        this.setIdleHeartBeat(1, TimeUnit.SECONDS);
        this.setDefaultBuilderTimeout(10,TimeUnit.SECONDS);
        this.setResultConsumer(res->{
            LOG.debug("UserInfo Build Success: {}",res);
            LoginCache.getInstance().save(res.product);
            if(userInfoDao != null) {
                userInfoDao.saveUserInfo(res.product);
            } else {
                LOG.warn("UserInfo Build not saved: {}",res);
            }
        });
        this.setScrapConsumer(bin-> LOG.error("UserInfo Build Failed: {}",bin));
    }

    public void setUserInfoDao(UserInfoDAO userInfoDao) {
        this.userInfoDao = userInfoDao;
    }

    public CompletableFuture<UserInfo> startUserInfoBuild(String token, String username) {
        LOG.debug("startUserInfoBuild {} {} {}",token,username);
        return this.createBuildFuture(token,UserInfoBuilder.supplier(token,username));
    }

    public void cancel(String token, LoginStep step, String errorMessage) {
        LOG.debug("Canceling {}: {}",token,errorMessage);
        this.add(token,new RuntimeException(errorMessage),step);
    }
}
