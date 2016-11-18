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

package com.epam.dlab.backendapi.core.response.folderlistener;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.util.Duration;

import java.util.concurrent.CompletableFuture;

@Singleton
public class FolderListenerExecutor {
    @Inject
    private ProvisioningServiceApplicationConfiguration configuration;


    public void start(String directory, Duration timeout, FileHandlerCallback fileHandlerCallback) {
        CompletableFuture.runAsync(new FolderListener(directory, timeout, fileHandlerCallback, configuration.getFileLengthCheckDelay()));
    }
}
