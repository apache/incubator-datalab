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
import com.epam.datalab.backendapi.resources.dto.KeysDTO;
import com.epam.datalab.backendapi.service.AccessKeyService;
import com.epam.datalab.exceptions.DatalabException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Singleton
@Slf4j
public class AccessKeyServiceImpl implements AccessKeyService {
    @Inject
    private SelfServiceApplicationConfiguration configuration;


    @Override
    public KeysDTO generateKeys(UserInfo userInfo) {
        log.debug("Generating new key pair for user {}", userInfo.getName());
        try (ByteArrayOutputStream publicKeyOut = new ByteArrayOutputStream();
             ByteArrayOutputStream privateKeyOut = new ByteArrayOutputStream()) {
            KeyPair pair = KeyPair.genKeyPair(new JSch(), KeyPair.RSA, configuration.getPrivateKeySize());
            pair.writePublicKey(publicKeyOut, userInfo.getName());
            pair.writePrivateKey(privateKeyOut);
            return new KeysDTO(new String(publicKeyOut.toByteArray()),
                    new String(privateKeyOut.toByteArray()), userInfo.getName());
        } catch (JSchException | IOException e) {
            log.error("Can not generate private/public key pair due to: {}", e.getMessage());
            throw new DatalabException("Can not generate private/public key pair due to: " + e.getMessage(), e);
        }
    }
}
