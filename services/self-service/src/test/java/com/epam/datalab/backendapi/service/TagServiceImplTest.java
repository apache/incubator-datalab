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

package com.epam.datalab.backendapi.service;

import com.epam.datalab.backendapi.resources.TestBase;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TagServiceImplTest extends TestBase {

    private static final String PROJECT = "project";
    private static final String ENDPOINT = "endpoint";
    private static final String CUSTOM_TAG = "customTag";

    TagService tagService = new TagServiceImpl();

    @Test
    public void getResourceTags() {
        Map<String, String> actualResourceTags = tagService.getResourceTags(getUserInfo(), ENDPOINT, PROJECT, CUSTOM_TAG,
                false);
        assertEquals("maps of tags are not equals", getExpectedResourceTags(), actualResourceTags);
    }

    @Test
    public void getResourceTagsWithNullCustomTag() {
        Map<String, String> actualResourceTags = tagService.getResourceTags(getUserInfo(), ENDPOINT, PROJECT, null
                , false);
        assertEquals("maps of tags are not equals", getExpectedResourceTagsWithNullCustomTag(), actualResourceTags);
    }

    private Map<String, String> getExpectedResourceTags() {
        Map<String, String> resourceTags = new HashMap<>();
        resourceTags.put("user_tag", USER.toLowerCase());
        resourceTags.put("endpoint_tag", ENDPOINT);
        resourceTags.put("project_tag", PROJECT);
        resourceTags.put("custom_tag", CUSTOM_TAG);

        return resourceTags;
    }

    private Map<String, String> getExpectedResourceTagsWithNullCustomTag() {
        Map<String, String> resourceTags = new HashMap<>();
        resourceTags.put("user_tag", USER.toLowerCase());
        resourceTags.put("endpoint_tag", ENDPOINT);
        resourceTags.put("project_tag", PROJECT);

        return resourceTags;
    }
}