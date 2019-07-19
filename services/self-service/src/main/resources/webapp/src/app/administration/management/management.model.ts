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

export class EnvironmentModel {
  constructor(
    public name: string,
    public status: string,
    public shape: string,
    public resources: Array<any>,
    public user: string,
    public ip: string,
    public type?: string,
    public project?: string,
  ) { }

  public static loadEnvironments(data: Array<any>) {
    if (data) {
      return data.map(value => new EnvironmentModel(
        value.resource_name,
        value.status,
        value.shape,
        value.computational_resources,
        value.user,
        value.public_ip,
        value.resource_type,
        value.project,
      ));
    }
  }
}

export class BackupOptionsModel {
  constructor(
    public configFiles: Array<string>,
    public keys: Array<string>,
    public certificates: Array<string>,
    public jars: Array<string>,
    public databaseBackup: boolean,
    public logsBackup: boolean
  ) { }

  setDegault(): void {
    this.configFiles = ['all'];
    this.keys = ['all'];
    this.certificates = ['skip'];
    this.jars = ['skip'];
    this.databaseBackup = true;
    this.logsBackup = false;
  }
}

export interface GeneralEnvironmentStatus {
  admin: boolean;
  billingEnabled: boolean;
  billingQuoteUsed: number;
  list_resources: any;
  status: string;
  projectAssigned: boolean;
}
