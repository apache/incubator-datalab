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

package com.epam.datalab.backendapi.core;

import com.epam.datalab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.datalab.backendapi.core.commands.ICommandExecutor;
import com.epam.datalab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.datalab.backendapi.core.response.handlers.dao.CallbackHandlerDao;
import com.epam.datalab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.datalab.dto.imagemetadata.ComputationalResourceShapeDto;
import com.epam.datalab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.datalab.dto.imagemetadata.ImageMetadataDTO;
import com.epam.datalab.dto.imagemetadata.ImageType;
import com.epam.datalab.process.model.ProcessInfo;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.dropwizard.util.Duration;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DockerWarmuperTest {
    @Inject
    private DockerWarmuper warmuper;
    private ExploratoryMetadataDTO exploratoryMetadata = new ExploratoryMetadataDTO(
            "executeResult");
    private ComputationalMetadataDTO computationalMetadata = new
            ComputationalMetadataDTO("executeResult");
    private static final String EXPLORATORY_TEST_JSON = "{\"exploratory_environment_shapes\" : { \"Category\" : [ " +
            "{\"Size\":\"L\", \"Type\":\"cg1.4xlarge\",\"Ram\": \"22.5 GB\",\"Cpu\": \"16\"}]}}";
    private static final String COMPUTATIONAL_TEST_JSON = "{\"template_name\":\"EMR cluster\"}";

    @Before
    public void setup() {
        createInjector().injectMembers(this);
        ComputationalResourceShapeDto computationalResourceShapeDto = new ComputationalResourceShapeDto();
        computationalResourceShapeDto.setSize("L");
        computationalResourceShapeDto.setType("cg1.4xlarge");
        computationalResourceShapeDto.setRam("22.5 GB");
        computationalResourceShapeDto.setCpu(16);
        List<ComputationalResourceShapeDto> metadataArray = new ArrayList<>();
        metadataArray.add(computationalResourceShapeDto);
        HashMap<String, List<ComputationalResourceShapeDto>> map = new HashMap<>();
        map.put("Category", metadataArray);
        exploratoryMetadata.setExploratoryEnvironmentShapes(map);
        computationalMetadata.setTemplateName("EMR cluster");
    }

    @Test
    public void warmupSuccess() throws Exception {
        warmuper.start();
        warmuper.getFileHandlerCallback(getFirstUUID())
                .handle(getFileName(), EXPLORATORY_TEST_JSON.getBytes());
        warmuper.getFileHandlerCallback(getFirstUUID())
                .handle(getFileName(), COMPUTATIONAL_TEST_JSON.getBytes());

        ImageMetadataDTO testExploratory = warmuper.getMetadata(ImageType.EXPLORATORY)
                .toArray(new ImageMetadataDTO[1])[0];
        testExploratory.setImage("executeResult");
        assertEquals(exploratoryMetadata.getImageType(), testExploratory.getImageType());

        ImageMetadataDTO testComputational = warmuper.getMetadata(ImageType.COMPUTATIONAL)
                .toArray(new ImageMetadataDTO[1])[0];
        testComputational.setImage("executeResult");
        assertEquals(computationalMetadata.getImageType(), testComputational.getImageType());
    }

    private String getFirstUUID() {
        return warmuper.getUuids().keySet().toArray(new String[1])[0];
    }

    private String getFileName() {
        return getFirstUUID() + ".json";
    }

    private Injector createInjector() {
        return Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(FolderListenerExecutor.class).toInstance(mock(FolderListenerExecutor.class));
                bind(ProvisioningServiceApplicationConfiguration.class).toInstance(createConfiguration());
                bind(ICommandExecutor.class).toInstance(createCommandExecutor());
                bind(CallbackHandlerDao.class).toInstance(mock(CallbackHandlerDao.class));
            }
        });
    }

    private ProvisioningServiceApplicationConfiguration createConfiguration() {
        ProvisioningServiceApplicationConfiguration result = mock(ProvisioningServiceApplicationConfiguration.class);
        when(result.getWarmupDirectory()).thenReturn("/tmp");
        when(result.getWarmupPollTimeout()).thenReturn(Duration.seconds(3));
        return result;
    }

    private ICommandExecutor createCommandExecutor() {
        ICommandExecutor result = mock(ICommandExecutor.class);
        try {
            final ProcessInfo pi = mock(ProcessInfo.class);
            when(pi.getStdOut()).thenReturn("executeResult");
            when(result.executeSync(anyString(), anyString(), anyString())).thenReturn(pi);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
