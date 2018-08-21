/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.epam.dlab.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
public class AnnotationUtils {

	private static final String ANNOTATION_METHOD = "annotationData";
	private static final String ANNOTATIONS = "annotations";

	private AnnotationUtils() {
	}

	public static void updateAnnotation(Class<?> targetClass, Class<? extends Annotation> targetAnnotation,
										Annotation targetValue) {
		alterAnnotationValue(targetClass, targetAnnotation, targetValue);
	}

	@SuppressWarnings("unchecked")
	private static void alterAnnotationValue(Class<?> targetClass, Class<? extends Annotation> targetAnnotation,
											 Annotation targetValue) {
		try {
			Method method = Class.class.getDeclaredMethod(ANNOTATION_METHOD);
			method.setAccessible(true);

			Object annotationData = method.invoke(targetClass);

			Field annotations = annotationData.getClass().getDeclaredField(ANNOTATIONS);
			annotations.setAccessible(true);

			Map<Class<? extends Annotation>, Annotation> map = (Map<Class<? extends Annotation>, Annotation>)
					annotations.get(annotationData);
			map.put(targetAnnotation, targetValue);
		} catch (Exception e) {
			log.error("An exception occured: {}", e.getLocalizedMessage());
		}
	}
}
