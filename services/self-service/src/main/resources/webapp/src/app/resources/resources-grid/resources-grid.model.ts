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

export class ResourcesGridRowModel {
  constructor(
    public name: Array<any>,
    public template_name: string,
    public image: string,
    public status: string,
    public shape: string,
    public resources: Array<any>,
    public time: string,
    public url: Array<any>,
    public ip: string,
    public username: string,
    public password: string,
    public bucket_name: string,
    public shared_bucket_name: string,
    public error_message: string,
    public cost: number,
    public currency_code: string,
    public billing: Array<any>,
    public libs: Array<any>,
    public account_name: string,
    public shared_account_name: string,
    public datalake_name: string,
    public datalake_directory: string,
    public datalake_shared_directory: string
  ) { }
}
