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

import com.epam.datalab.core.parser.ParserBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * Abstract class for modules: adapter, filter, parser.<br>
 * To create your adapter:<br>
 * 1. Create a class which extends one of {@link AdapterBase}, {@link FilterBase} or {@link ParserBase} classes.<br>
 * 2. Annotate it with {@link JsonTypeName} annotation and give it a unique type name for this type of modules.<br>
 * 3. Add full the name of your class to main/resources/com.epam.datalab.configuration.BillingToolConfigurationFactory file.
 * 4. Annotate it with {@link JsonClassDescription] annotation and describe all properties of module.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class ModuleBase {

    /**
     * Working data of module.
     */
    @JsonIgnore
    private ModuleData moduleData;

    /**
     * Return the name of type for appender.
     */
    @JsonIgnore
    public String getType() {
        Class<? extends ModuleBase> clazz = this.getClass();
        return (clazz.isAnnotationPresent(JsonTypeName.class) ?
                clazz.getAnnotation(JsonTypeName.class).value() : clazz.getName());
    }

    /**
     * Return the working data of module.
     */
    public ModuleData getModuleData() {
        return moduleData;
    }

    /**
     * Set the working data of module.
     */
    public void setModuleData(ModuleData moduleData) {
        this.moduleData = moduleData;
    }

    /**
     * Returns a string representation of the object.
     *
     * @param self the object to generate the string for (typically this), used only for its class name.
     */
    public ToStringHelper toStringHelper(Object self) {
        return MoreObjects.toStringHelper(self)
                .add("type", getType());
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
