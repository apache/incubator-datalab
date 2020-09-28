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

package com.epam.datalab.configuration;

import com.google.common.base.Throwables;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides Apache FreeMarker the template engine for the billing configuration.
 */
public class FreeMarkerConfig {

    /**
     * Create and return the input stream for the configuration file.
     *
     * @param filename the name of configuration file.
     * @throws IOException
     */
    public InputStream getInputStream(final String filename) throws IOException {
        try {
            Configuration conf = new Configuration(Configuration.VERSION_2_3_22);
            Template template = new Template("billing-config", new FileReader(new File(filename)), conf);
            Map<String, Object> dataModel = getDataModel();

            ByteArrayOutputStream streamBuffer = new ByteArrayOutputStream();
            template.process(dataModel, new OutputStreamWriter(streamBuffer, StandardCharsets.UTF_8));
            byte[] buffer = streamBuffer.toByteArray();

            return new ByteArrayInputStream(buffer);
        } catch (TemplateException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Create and return JVM and OS properties.
     */
    private Map<String, Object> getDataModel() {
        Map<String, Object> dataModel = new HashMap<>();

        Iterator<Object> sysProps = System.getProperties().keySet().iterator();
        while (sysProps.hasNext()) {
            String key = (String) sysProps.next();
            dataModel.put(key, System.getProperties().getProperty(key));
        }
        dataModel.putAll(System.getenv());

        dataModel.put("env", System.getenv());
        dataModel.put("sys", System.getProperties());

        return dataModel;
    }
}
