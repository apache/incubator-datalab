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

export interface ComputationalResourceModel {
  project_computations: string[];
  templates: ComputationalTemplate[];
  user_computations: string[];
}

export interface ComputationalTemplate {
  image: string;
  description: string;
  templates: TemplatesVersion[];
  computationGPU: string[];
  image_type: string;
  template_name: string;
  environment_type: string;
  request_id: string;
  computation_resources_shapes: ComputationResourcesShapes;
  limits: {[key: string]: number };
}

export interface ComputationResourcesShapes {
  ['GPU optimized']: Shape[];
  ['Compute optimized']: Shape[];
  ['Memory optimized']: Shape[];
  ['For testing']: Shape[];
}

export interface Shape {
  Type: string;
  Size: string;
  Description: string;
  Ram: string;
  Cpu: number;
  Spot: boolean;
  SpotPctPrice: number;
}

export interface TemplatesVersion {
  applications: Application[];
  version: string;
}

export interface Application {
  Version: string;
  Name: string;
}
