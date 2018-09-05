/*
Copyright 2016 EPAM Systems, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.epam.dlab.process.model;

import com.aegisql.conveyor.SmartLabel;
import com.epam.dlab.process.builder.ProcessInfoBuilder;

import java.util.function.BiConsumer;

public enum ProcessStep implements SmartLabel<ProcessInfoBuilder> {
    START(ProcessInfoBuilder::start),
    STOP(ProcessInfoBuilder::stop),
    KILL(ProcessInfoBuilder::kill),
    FINISH(ProcessInfoBuilder::finish),
    STD_OUT(ProcessInfoBuilder::stdOut),
    STD_ERR(ProcessInfoBuilder::stdErr),
    FAILED(ProcessInfoBuilder::failed),
    FUTURE(ProcessInfoBuilder::future),
;
    private BiConsumer<ProcessInfoBuilder, Object> consumer;

    @SuppressWarnings("unchecked")
	<T> ProcessStep(BiConsumer<ProcessInfoBuilder, T> consumer) {
        this.consumer = (BiConsumer<ProcessInfoBuilder, Object>) consumer;
    }

    @Override
    public BiConsumer<ProcessInfoBuilder, Object> get() {
        return consumer;
    }
}
