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

package com.epam.dlab.configuration;

import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.epam.dlab.core.BillingUtils;
import com.epam.dlab.exceptions.InitializationException;

/** Json properties validator.
 * @param <T> Class for validation.
 */
public class ConfigurationValidator<T> {
	
	/** Error messages. */
	private static Map<String, String> messages = BillingUtils.stringsToMap(
			"{javax.validation.constraints.NotNull.message}", "Property \"%s\" may not be null"); 

	/** Return the list of error messages.
	 * @param violation constraint violations.
	 */
	public String getMessage(ConstraintViolation<T> violation) {
		String template = messages.get(violation.getMessageTemplate());
		if (template == null) {
			template = "Property \"%s\" %s";
		}
		return String.format(
					messages.get(violation.getMessageTemplate()),
					violation.getPropertyPath(),
					violation.getInvalidValue());
	}

	/** Validate properties in instance and throw exception if it have not valid property.
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
