/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.validation;

import com.epam.dlab.ServiceConfiguration;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

public class CloudConfigurationSequenceProvider<T extends ServiceConfiguration> implements DefaultGroupSequenceProvider<T> {
    @Override
    public List<Class<?>> getValidationGroups(T c) {
        List<Class<?>> sequence = new ArrayList<>();

        sequence.add(initialSequenceGroup());

        if (c == null) {
            return sequence;
        } else {
            switch (c.getCloudProvider()) {
                case AWS:
                    sequence.add(AwsValidation.class);
                    break;
                case AZURE:
                    sequence.add(AzureValidation.class);
                    break;
                case GCP:
                    sequence.add(GcpValidation.class);
                    break;
                default:
                    throw new IllegalArgumentException("Cloud provider is not supported" + c.getCloudProvider());
            }
        }

        return sequence;
    }

    private Class<T> initialSequenceGroup() {
        ParameterizedType parameterizedType = (ParameterizedType) getClass()
                .getGenericSuperclass();

        @SuppressWarnings("unchecked")
        Class<T> ret = (Class<T>) parameterizedType.getActualTypeArguments()[0];

        return ret;
    }
}
