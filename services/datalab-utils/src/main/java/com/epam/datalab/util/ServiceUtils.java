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

package com.epam.datalab.util;

import com.epam.datalab.exceptions.DatalabException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@Slf4j
public class ServiceUtils {

    private static String includePath = null;

    static {
        includePath = System.getenv("DATALAB_CONF_DIR");
        if (includePath == null || includePath.isEmpty()) {
            includePath = getUserDir();
        }
    }

    /* Return working directory.
     */
    public static String getUserDir() {
        return System.getProperty("user.dir");
    }

    /**
     * Return path to DataLab configuration directory.
     *
     * @return
     */
    public static String getConfPath() {
        return includePath;
    }


    /**
     * Return manifest for given class or empty manifest if {@link JarFile#MANIFEST_NAME} not found.
     *
     * @param clazz class.
     * @throws IOException
     */
    private static Manifest getManifestForClass(Class<?> clazz) throws IOException {
        URL url = clazz.getClassLoader().getResource(JarFile.MANIFEST_NAME);
        return (url == null ? new Manifest() : new Manifest(url.openStream()));
    }

    /**
     * Return manifest from JAR file.
     *
     * @param classPath path to class in JAR file.
     * @throws IOException
     */
    private static Manifest getManifestFromJar(String classPath) throws IOException {
        URL url = new URL(classPath);
        JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
        return jarConnection.getManifest();
    }

    /**
     * Return manifest map for given class or empty map if manifest not found or cannot be read.
     *
     * @param clazz class.
     */
    public static Map<String, String> getManifest(Class<?> clazz) {
        String className = "/" + clazz.getName().replace('.', '/') + ".class";
        String classPath = clazz.getResource(className).toString();

        Map<String, String> map = new HashMap<>();
        try {
            Manifest manifest = (classPath.startsWith("jar:file:") ? getManifestFromJar(classPath) : getManifestForClass(clazz));
            Attributes attributes = manifest.getMainAttributes();
            for (Object key : attributes.keySet()) {
                map.put(key.toString(), (String) attributes.get(key));
            }
        } catch (IOException e) {
            log.error("Cannot found or open manifest for class {}", className, e);
            throw new DatalabException("Cannot read manifest file", e);
        }

        return map;
    }

    /**
     * Print to standard output the manifest info about application. If parameter <b>args</b> is not
     * <b>null</b> and one or more arguments have value -v or --version then print version and return <b>true<b/>
     * otherwise <b>false</b>.
     *
     * @param mainClass the main class of application.
     * @param args      the arguments of main class function or null.
     * @return if parameter <b>args</b> is not null and one or more arguments have value -v or --version
     * then return <b>true<b/> otherwise <b>false</b>.
     */
    public static boolean printAppVersion(Class<?> mainClass, String... args) {
        boolean result = false;
        if (args != null) {
            for (String arg : args) {
                if ("-v".equals(arg) ||
                        "--version".equals(arg)) {
                    result = true;
                }
            }
            if (!result) {
                return result;
            }
        }

        Map<String, String> manifest = getManifest(mainClass);
        if (manifest.isEmpty()) {
            return result;
        }

        log.info("Title       {}", manifest.get("Implementation-Title"));
        log.info("Version     {}", manifest.get("Implementation-Version"));
        log.info("Created By  {}", manifest.get("Created-By"));
        log.info("Vendor      {}", manifest.get("Implementation-Vendor"));
        log.info("GIT-Branch  {}", manifest.get("GIT-Branch"));
        log.info("GIT-Commit  {}", manifest.get("GIT-Commit"));
        log.info("Build JDK   {}", manifest.get("Build-Jdk"));
        log.info("Build OS    {}", manifest.get("Build-OS"));
        log.info("Built Time  {}", manifest.get("Build-Time"));
        log.info("Built By    {}", manifest.get("Built-By"));

        return result;
    }
}
