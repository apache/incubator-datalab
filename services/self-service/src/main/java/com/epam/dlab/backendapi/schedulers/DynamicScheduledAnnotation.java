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
package com.epam.dlab.backendapi.schedulers;

import com.fiestacabin.dropwizard.quartz.Scheduled;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ClassExplicitlyAnnotation")
@Setter
public class DynamicScheduledAnnotation implements Scheduled {

	private int interval;
	private TimeUnit timeUnit;

	@Override
	public String cron() {
		return StringUtils.EMPTY;
	}

	@Override
	public int interval() {
		return interval;
	}

	@Override
	public TimeUnit unit() {
		return timeUnit;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return this.getClass();
	}
}
