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

export class EnvironmentStatusModel {
  constructor(
    public type: string,
    public resource_id: string,
    public status: string
  ) { }
}

export class BackupOptionsModel {
  constructor(
    public configFiles: Array<string>,
    public keys:  Array<string>,
    public certificates:  Array<string>,
    public jars:  Array<string>,
    public databaseBackup: boolean,
    public logsBackup: boolean
  ) { }

  setDegault(): void {
    this.configFiles = ['all'];
    this.keys =['all'];
    this.certificates = ['skip'];
    this.jars = ['skip'];
    this.databaseBackup = true;
    this.logsBackup = false;
  }
}

export interface GeneralEnvironmentStatus {
  admin: boolean,
  billingEnabled: boolean,
  list_resources: Array<EnvironmentStatusModel>,
  status: string
}