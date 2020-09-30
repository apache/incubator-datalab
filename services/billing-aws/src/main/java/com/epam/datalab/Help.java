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

package com.epam.datalab;

import com.epam.datalab.core.BillingUtils;
import com.epam.datalab.core.ModuleType;
import com.epam.datalab.exceptions.InitializationException;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Print help for billing tool.
 */
public class Help {

    private Help() {
    }

    /**
     * Print help to console.
     *
     * @param resourceName the name of resource.
     * @param substitute   - map for substitution in help content.
     * @throws InitializationException
     */
    private static void printHelp(String resourceName, Map<String, String> substitute) throws InitializationException {
        List<String> list = BillingUtils.getResourceAsList("/" + Help.class.getName() + "." + resourceName + ".txt");
        String help = StringUtils.join(list, System.lineSeparator());

        if (substitute == null) {
            substitute = new HashMap<>();
        }
        substitute.put("classname", BillingServiceImpl.class.getName());

        for (String key : substitute.keySet()) {
            help = StringUtils.replace(help, "${" + key.toUpperCase() + "}", substitute.get(key));
        }
        System.out.println(help);
    }

    /**
     * Create and return substitutions for names of modules.
     *
     * @return
     * @throws InitializationException
     */
    private static Map<String, String> findModules() throws InitializationException {
        List<Class<?>> modules = BillingUtils.getModuleClassList();
        Map<String, String> substitute = new HashMap<>();

        for (Class<?> module : modules) {
            ModuleType type = BillingUtils.getModuleType(module);
            JsonTypeName typeName = module.getAnnotation(JsonTypeName.class);
            if (typeName != null) {
                String typeNames = substitute.get(type.toString() + "s");
                typeNames = (typeNames == null ? typeName.value() : typeNames + ", " + typeName.value());
                substitute.put(type.toString() + "s", typeNames);
            }
        }

        return substitute;
    }

    /**
     * Find and return help for module.
     *
     * @param type the type of module.
     * @param name the name of module.
     * @throws InitializationException
     */
    private static String findModuleHelp(ModuleType type, String name) throws InitializationException {
        List<Class<?>> modules = BillingUtils.getModuleClassList();
        String typeNames = null;
        for (Class<?> module : modules) {
            ModuleType t = BillingUtils.getModuleType(module);
            if (t == type) {
                JsonTypeName typeName = module.getAnnotation(JsonTypeName.class);
                if (typeName != null) {
                    if (name.equals(typeName.value())) {
                        JsonClassDescription description = module.getAnnotation(JsonClassDescription.class);
                        if (description != null) {
                            return description.value();
                        }
                        throw new InitializationException("Help for " + type + " " + name + " not found");
                    } else {
                        typeNames = (typeNames == null ? typeName.value() : typeNames + ", " + typeName.value());
                    }
                }
            }
        }
        throw new InitializationException("Module for " + type + " " + name + " not found." +
                (typeNames == null ? "" : " Module type must be one of next: " + typeNames));
    }

    /**
     * Print help screen for billing tool.
     *
     * @throws InitializationException
     */
    public static void usage(String... args) throws InitializationException {
        if (args == null || args.length == 0) {
            printHelp("usage", null);
        } else if ("conf".equalsIgnoreCase(args[0])) {
            printHelp("conf", findModules());
        } else {
            ModuleType type = ModuleType.of(args[0]);
            if (type == null) {
                System.out.println("Unknown --help " + args[0] + " command.");
            } else if (args.length < 2) {
                System.out.println("Missing the type of module.");
                String typeNames = findModules().get(type.toString() + "s");
                if (typeNames != null) {
                    System.out.println("Must be one of next: " + typeNames);
                }
            } else if (args.length > 2) {
                System.out.println("Extra arguments in command: " +
                        StringUtils.join(Arrays.copyOfRange(args, 2, args.length), " "));
            } else {
                System.out.println(findModuleHelp(type, args[1]));
            }
        }
    }
}
