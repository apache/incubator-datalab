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

import com.epam.datalab.core.BillingUtils;
import com.epam.datalab.exceptions.InitializationException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Map;
import java.util.Set;

/**
 * Json properties validator.
 *
 * @param <T> Class for validation.
 */
public class ConfigurationValidator<T> {

    /**
     * Error messages.
     */
    private static Map<String, String> messages = BillingUtils.stringsToMap(
            "{javax.validation.constraints.NotNull.message}", "Property \"%s\" may not be null");

    /**
     * Return the list of error messages.
     *
     * @param violation constraint violations.
     */
    public String getMessage(ConstraintViolation<T> violation) {
        return String.format(
                messages.get(violation.getMessageTemplate()),
                violation.getPropertyPath(),
                violation.getInvalidValue());
    }

    /**
     * Validate properties in instance and throw exception if it have not valid property.
     *
     * @param clazz instance for validation.
     * @throws InitializationException
     */
    public void validate(T clazz) throws InitializationException {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        Set<ConstraintViolation<T>> violations = validator.validate(clazz);
        for (ConstraintViolation<T> violation : violations) {
            throw new InitializationException(getMessage(violation));
        }
    }
}
