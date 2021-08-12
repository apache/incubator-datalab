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

export class FilterAuditModel {

  static getDefault(): FilterAuditModel {
    return new FilterAuditModel([], [], [], [], [], '', '');
  }

  constructor(
    public users: Array<string>,
    public resources: Array<string>,
    public resource_types: Array<string>,
    public projects: Array<string>,
    public actions: Array<string>,
    public date_start: string,
    public date_end: string,
  ) { }

  defaultConfigurations(): void {
    this.users = [];
    this.projects = [];
    this.resource_types = [];
    this.resources = [];
    this.actions = [];
    this.date_start = '';
    this.date_end = '';

  }
}
