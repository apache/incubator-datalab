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

package com.epam.dlab.backendapi.domain;

public enum NotebookTemplate {
	JUPYTER("Jupyter notebook 6.0.2"),
	JUPYTER_LAB("JupyterLab 0.35.6"),
	ZEPPELIN("Apache Zeppelin 0.8.2"),
	DEEP_LEARNING("Deep Learning  2.4"),
	TENSOR("Jupyter with TensorFlow 2.1.0"),
	TENSOR_RSTUDIO("RStudio with TensorFlow 2.1.0"),
	RSTUDIO("RStudio 1.2.5033");

	private String name;

	NotebookTemplate(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
