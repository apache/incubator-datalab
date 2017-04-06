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

import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.epam.dlab.auth.UserInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.*;

public class LoginConveyorTest {

    LoginConveyor lc = new LoginConveyor();

    @Before
    public void createCOnveyor(){
    }

    @After
    public void stopConveyor() {
    }

    @Test
    public void setUserInfoDao() throws Exception {

    }

    @SuppressWarnings("serial")
	@Test
    public void startUserInfoBuild() throws Exception {
        CompletableFuture<UserInfo> uf = lc.startUserInfoBuild("1","test");
        UserInfo uiSource = new UserInfo("a","b");
        uiSource.setFirstName("test");
        uiSource.setLastName("user");
        uiSource.addRole("admin");

        lc.add("1","127.0.0.1",LoginStep.REMOTE_IP);
        lc.add("1","OK",LoginStep.LDAP_LOGIN);
        lc.add("1",uiSource,LoginStep.LDAP_USER_INFO);
        lc.add("1",true,LoginStep.AWS_USER);
        lc.add("1",new ArrayList<AccessKeyMetadata>() {
        		{	add(new AccessKeyMetadata()
        				.withAccessKeyId("a")
        				.withStatus("Active"));
        		}} ,LoginStep.AWS_KEYS);

        UserInfo ui = uf.get(5, TimeUnit.SECONDS);
        System.out.println("Future now: "+ui);
    }

    @Test(expected = CancellationException.class)
    public void cacheTest() throws ExecutionException, InterruptedException, TimeoutException {
        LoginCache cache = LoginCache.getInstance();
System.out.println("---cacheTest");
        //Just for this test
        cache.setDefaultBuilderTimeout(1,TimeUnit.SECONDS);
        cache.setExpirationPostponeTime(1,TimeUnit.SECONDS);

        UserInfo userInfo = new UserInfo("test","user");
        userInfo.setFirstName("Mike");
        userInfo.setLastName("T");
        userInfo.addRole("tr");
        userInfo.setAwsUser(true);
        userInfo.addKey("a","Active");

        CompletableFuture<Boolean> f = cache.createBuild("2", CacheableReference.newInstance(userInfo));
        CompletableFuture<UserInfo> uif = cache.getFuture("2");
        f.get();
        //this will take at least 2 seconds
        for(int i = 0; i < 10; i++) {
            UserInfo ui = cache.getUserInfo("2");
            System.out.println(i+": "+ui);
            Thread.sleep(200);
        }
        //and finally will exit with timeout
        uif.get(5,TimeUnit.SECONDS);
    }

    @Test
    public void add() throws Exception {

    }

    @Test
    public void cancel() throws Exception {

    }

}