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

package com.epam.dlab.backendapi.core;

public interface Directories {
    String WARMUP_DIRECTORY = "/result";
    String IMAGES_DIRECTORY = "/result";
    String KEY_LOADER_DIRECTORY = "/result";
    String EDGE_LOG_DIRECTORY = "edge";
    String NOTEBOOK_LOG_DIRECTORY = "notebook";
    String EMR_LOG_DIRECTORY = "emr";
}
