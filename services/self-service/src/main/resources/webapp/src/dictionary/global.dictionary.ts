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

  constructor(
    public user: Array<string>,
    public service: Array<string>,
    public resource_type: Array<string>,
    public status: Array<string>,
    public shape: Array<string>,
    public date_start: string,
    public date_end: string,
    public dlab_id: string,
    public project?: Array<string>
  ) { }

  defaultConfigurations(): void {
    this.user = [];
    this.service = [];
    this.resource_type = [];
    this.status = [];
    this.shape = [];
    this.date_start = '';
    this.date_end = '';
    this.dlab_id = '';
    this.project = [];
  }
}


