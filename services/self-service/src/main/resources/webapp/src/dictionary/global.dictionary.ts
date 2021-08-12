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

// from './{{ aws | gcp | azure }}.dictionary';

import { NAMING_CONVENTION_AWS } from './aws.dictionary';
import { NAMING_CONVENTION_GCP } from './gcp.dictionary';
import { NAMING_CONVENTION_AZURE } from './azure.dictionary';

export const DICTIONARY = Object.freeze({
  aws: NAMING_CONVENTION_AWS,
  gcp: NAMING_CONVENTION_GCP,
  azure: NAMING_CONVENTION_AZURE
});

export class ReportingConfigModel {

  static getDefault(): ReportingConfigModel {
    return new ReportingConfigModel([], [], [], [], [], '', '', '', []);
  }

  defaultConfigurations(): void {
    this.users = [];
    this.products = [];
    this.resource_type = [];
    this.statuses = [];
    this.shapes = [];
    this.date_start = '';
    this.date_end = '';
    this.datalabId = '';
    this.projects = [];
  }

  constructor(
    public users: Array<string>,
    public products: Array<string>,
    public resource_type: Array<string>,
    public statuses: Array<string>,
    public shapes: Array<string>,
    public date_start: string,
    public date_end: string,
    public datalabId: string,
    public projects: Array<string>,
    public locale?: string
  ) { }
}


