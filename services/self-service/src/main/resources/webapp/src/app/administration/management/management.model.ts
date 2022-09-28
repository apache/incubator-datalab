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
    public endpoint?: string,
    public cloud_provider?: string,
    public gpu_type?: string,
    public gpu_count?: string,
    public exploratory_urls?: Array<any>
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
        value.endpoint,
        value.cloud_provider,
        value.gpu_type,
        value.gpu_count,
        value.exploratory_urls
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
  auditEnabled: boolean;
  projectAdmin: boolean;
  billingEnabled: boolean;
  billingQuoteUsed: number;
  list_resources: any;
  status: string;
  projectAssigned: boolean;
  bucketBrowser: object;
  connectedPlatforms: ConnectedPlatformsStatus;
}

export interface ConnectedPlatformsStatus {
  add: boolean;
  disconnect: boolean;
  view: boolean;
}

export class ManagementConfigModel {

  static getDefault(): ManagementConfigModel {
    return new ManagementConfigModel([], '', [], [], [], [], []);
  }

  constructor(
    public users: Array<string>,
    public type: string,
    public projects: Array<string>,
    public shapes: Array<string>,
    public statuses: Array<string>,
    public resources: Array<string>,
    public endpoints: Array<string>,
  ) { }

  defaultConfigurations(): void {
    this.users = [];
    this.type = '';
    this.projects = [];
    this.shapes = [];
    this.statuses = [];
    this.resources = [];
    this.endpoints = [];
  }
}

export interface ModalData {
  action: ActionsType;
  resource_name?: any;
  user?: any;
  type: string;
  notebooks?: any;
}

export enum ModalDataType {
  cluster = 'cluster',
  notebook = 'notebook',
}

export enum ActionsType {
  stop = 'stop',
  terminate = 'terminate',
  start = 'start',
  run = 'run',
  recreate = 'recreate',
  createImage = 'create image'
}

export type ActionTypeOptions = 'stop' | 'terminate' | 'start' | 'createImage';

