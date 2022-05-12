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

package com.epam.datalab.backendapi.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotebookTemplate {
    JUPYTER("Jupyter notebook 6.1.6"),
    JUPYTER_GPU("Jupyter notebook 6.1.6 with GPU"),
    JUPYTER_LAB("JupyterLab 0.35.6"),
    ZEPPELIN("Apache Zeppelin 0.9.1"),
    DEEP_LEARNING("Deep Learning  2.4"),
    TENSOR("Jupyter with TensorFlow 2.5.0"),
    TENSOR_JUPYTERLAB("JupyterLab with TensorFlow 2.5.0"),
    TENSOR_RSTUDIO("RStudio with TensorFlow 2.3.2"),
    RSTUDIO("RStudio 2021.09.1-372"),
    TENSOR_GCP("Jupyter with TensorFlow 2.1.0"),
    DEEP_LEARNING_GCP("Deeplearning notebook"),
    DEEP_LEARNING_AWS("Deep Learning AMI Version 42.1"),
    DEEP_LEARNING_AZURE("Data Science Virtual Machine - Ubuntu 18.04");


    private final String name;
}
