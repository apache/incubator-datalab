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
import { DICTIONARY } from '../../../dictionary/global.dictionary';

export class ExploratoryModel {
  readonly DICTIONARY = DICTIONARY;

  constructor(
    public cloud_provider: string,
    public name: Array<any>,
    public template_name: string,
    public image: string,
    public status: string,
    public shape: string,
    public resources: Array<any>,
    public time: string,
    public url: Array<any>,
    public node_ip: string,
    public private_ip: string,
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
    public datalake_shared_directory: string,
    public project: string,
    public endpoint: string,
    public tags: any,
    public edgeNodeStatus: string
  ) { }

  public static loadEnvironments(data: Array<any>) {
    if (data) {
      return data.map((value) => {
        return {
          project: value.project,
          exploratory: value.exploratory.map(el => {
            const provider = el.cloud_provider.toLowerCase();
            const billing = value.exploratoryBilling.filter(res => res.tag === el.exploratory_id)[0];
            return new ExploratoryModel(
            provider,
            el.exploratory_name,
            el.template_name,
            el.image,
            el.status,
            el.shape,
            el.computational_resources,
            el.up_time,
            el.exploratory_url,
            value.shared[el.endpoint].edge_node_ip,
            el.private_ip,
            el.exploratory_user,
            el.exploratory_pass,
            value.shared[el.endpoint][DICTIONARY[provider].bucket_name],
            value.shared[el.endpoint][DICTIONARY[provider].shared_bucket_name],
            el.error_message,
            billing.cost,
            billing.currency,
            el.billing,
            el.libs,
            value.shared[el.endpoint][DICTIONARY[provider].user_storage_account_name],
            value.shared[el.endpoint][DICTIONARY[provider].shared_storage_account_name],
            value.shared[el.endpoint][DICTIONARY[provider].datalake_name],
            value.shared[el.endpoint][DICTIONARY[provider].datalake_user_directory_name],
            value.shared[el.endpoint][DICTIONARY[provider].datalake_shared_directory_name],
            el.project,
            el.endpoint,
            el.tags,
            value.shared[el.endpoint].status
          )})
        };
      });
    }
  }
}

export interface Exploratory {
  project: string;
  exploratory: ExploratoryModel[];
}
