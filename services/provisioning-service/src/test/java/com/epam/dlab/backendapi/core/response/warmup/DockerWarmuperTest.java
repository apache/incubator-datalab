/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.core.response.warmup;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.CommandExecutor;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.dto.imagemetadata.ImageMetadataDTO;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.dropwizard.util.Duration;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DockerWarmuperTest {
    @Inject
    private DockerWarmuper warmuper;
    private ImageMetadataDTO metadata = new ImageMetadataDTO("executeResult");

    @Before
    public void setup() {
        createInjector().injectMembers(this);
    }

    @Test
    public void warmupSuccess() throws Exception {
        warmuper.start();
        warmuper.getFileHandlerCallback().handle(getFileName(), "{}".getBytes());
        assertEquals(metadata, warmuper.getMetadatas().toArray(new ImageMetadataDTO[1])[0]);
    }

    private String getFileName() {
        return warmuper.getUuids().keySet().toArray(new String[1])[0] + ".json";
    }

    private Injector createInjector() {
        return Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(FolderListenerExecutor.class).toInstance(mock(FolderListenerExecutor.class));
                bind(ProvisioningServiceApplicationConfiguration.class).toInstance(createConfiguration());
                bind(CommandExecutor.class).toInstance(createCommandExecuter());
            }
        });
    }

    private ProvisioningServiceApplicationConfiguration createConfiguration() {
        ProvisioningServiceApplicationConfiguration result = mock(ProvisioningServiceApplicationConfiguration.class);
        when(result.getWarmupDirectory()).thenReturn("/tmp");
        when(result.getWarmupPollTimeout()).thenReturn(Duration.seconds(3));
        return result;
    }

    private CommandExecutor createCommandExecuter() {
        CommandExecutor result = mock(CommandExecutor.class);
        try {
            when(result.executeSync(anyString())).thenReturn(Collections.singletonList("executeResult"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
