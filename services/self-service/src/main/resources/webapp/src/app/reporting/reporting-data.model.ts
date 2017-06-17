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

export class ReportingConfigModel {

  static getDefault(): ReportingConfigModel {
    return new ReportingConfigModel([], [], [], [], '', '');
  }

  constructor(
    public user: Array<string>,
    public product: Array<string>,
    public resource_type: Array<string>,
    public shape: Array<string>,
    public date_start: string,
    public date_end: string,
  ) { }

  defaultConfigurations(): void {
    this.user = [];
    this.product = [];
    this.resource_type = [];
    this.shape = [];
    this.date_start = '';
    this.date_end = '';
  }
}