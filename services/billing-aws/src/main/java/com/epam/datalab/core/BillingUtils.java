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

package com.epam.datalab.core;

import com.epam.datalab.configuration.BillingToolConfigurationFactory;
import com.epam.datalab.core.parser.ParserBase;
import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.logging.AppenderBase;
import com.google.common.io.Resources;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Billing toll utilities.
 */
public class BillingUtils {

    /**
     * Name of resource with the names of module classes.
     */
    private static final String RESOURCE_MODULE_NAMES = "/" + BillingToolConfigurationFactory.class.getName();

    private BillingUtils() {
    }

    /**
     * Create and return map from given key/values.
     *
     * @param keyValues the key/value pairs.
     */
    public static Map<String, String> stringsToMap(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        if (keyValues == null) {
            return map;
        }
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Missing key or value in arguments");
        }

        for (int i = 1; i < keyValues.length; i += 2) {
            map.put(keyValues[i - 1], keyValues[i]);
        }
        return map;
    }

    /**
     * Read and return content as string list from resource.
     *
     * @param resourceName the name of resource.
     * @return list of strings.
     * @throws InitializationException
     */
    public static List<String> getResourceAsList(String resourceName) throws InitializationException {
        try {
            URL url = BillingToolConfigurationFactory.class.getResource(resourceName);
            if (url == null) {
                throw new InitializationException("Resource " + resourceName + " not found");
            }
            return Resources.readLines(url, Charset.forName("utf-8"));
        } catch (IllegalArgumentException | IOException e) {
            throw new InitializationException("Cannot read resource " + resourceName + ": " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Return the list of billing tool modules.
     *
     * @throws InitializationException
     */
    public static List<Class<?>> getModuleClassList() throws InitializationException {
        List<String> modules = getResourceAsList(RESOURCE_MODULE_NAMES);
        List<Class<?>> classes = new ArrayList<>();

        for (String className : modules) {
            try {
                classes.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                throw new InitializationException("Cannot add the sub type " + className +
                        " from resource " + RESOURCE_MODULE_NAMES + ": " + e.getLocalizedMessage(), e);
            }
        }
        return classes;
    }

    /**
     * Check for child class is belong to parent by hierarchy.
     *
     * @param child  the child class for check.
     * @param parent the parent class from hierarchy.
     */
    public static boolean classChildOf(Class<?> child, Class<?> parent) {
        return child != null && parent != null && parent.isAssignableFrom(child);
    }

    /**
     * Return the type of module if class is module otherwise <b>null</b>.
     *
     * @param moduleClass the class.
     */
    public static ModuleType getModuleType(Class<?> moduleClass) {
        if (classChildOf(moduleClass, AdapterBase.class)) {
            return ModuleType.ADAPTER;
        } else if (classChildOf(moduleClass, FilterBase.class)) {
            return ModuleType.FILTER;
        } else if (classChildOf(moduleClass, ParserBase.class)) {
            return ModuleType.PARSER;
        } else if (classChildOf(moduleClass, AppenderBase.class)) {
            return ModuleType.LOGAPPENDER;
        }
        return null;
    }

    /**
     * Return the name of user without domain.
     *
     * @param value the value.
     */
    public static String getSimpleUserName(String username) {
        return (username == null ? null : username.replaceAll("@.*", ""));
    }
}
