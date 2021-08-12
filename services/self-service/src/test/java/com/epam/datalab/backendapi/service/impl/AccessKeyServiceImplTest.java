/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.resources.TestBase;
import com.epam.datalab.backendapi.resources.dto.KeysDTO;
import com.epam.datalab.exceptions.DatalabException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AccessKeyServiceImplTest extends TestBase {

    @Mock
    private SelfServiceApplicationConfiguration conf;

    @InjectMocks
    private AccessKeyServiceImpl accessKeyService;


    @Test
    public void generateKeys() {
        UserInfo userInfo = getUserInfo();
        when(conf.getPrivateKeySize()).thenReturn(2048);

        KeysDTO keysDTO = accessKeyService.generateKeys(userInfo);

        assertEquals("Usernames are not equal", USER.toLowerCase(), keysDTO.getUsername());
        assertNotNull("Public key is null", keysDTO.getPublicKey());
        assertNotNull("Private key is null", keysDTO.getPrivateKey());
    }

    @Test(expected = DatalabException.class)
    public void generateKeysWithException() {
        UserInfo userInfo = getUserInfo();
        when(conf.getPrivateKeySize()).thenReturn(0);

        accessKeyService.generateKeys(userInfo);
    }
}