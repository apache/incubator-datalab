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

import { Observable } from 'rxjs';

import { LibrariesInstallationService } from '../../../core/services';

interface Library {
  group: string;
  name: string;
  version: string;
}

export class InstallLibrariesModel {
  confirmAction: Function;
  notebook: any;
  computational_name: string;

  public selectedLibs: Array<Library> = [];
  private continueWith: Function;
  private librariesInstallationService: LibrariesInstallationService;

  static getDefault(librariesInstallationService): InstallLibrariesModel {
    return new InstallLibrariesModel('', () => {}, () => {}, null, librariesInstallationService);
  }

  constructor(
    notebook: any,
    fnProcessResults: any,
    fnProcessErrors: any,
    continueWith: Function,
    librariesInstallationService: LibrariesInstallationService
  ) {
    this.notebook = notebook;
    this.continueWith = continueWith;
    this.librariesInstallationService = librariesInstallationService;
    this.prepareModel(fnProcessResults, fnProcessErrors);

    if (this.continueWith) this.continueWith();
  }

  public getLibrariesList(group: string, query: string): Observable<{}> {
    const lib_query: any = {
      project_name: this.notebook.project,
      exploratory_name: this.notebook.name,
      group: group,
      start_with: query
    };
    if (this.computational_name) {
      lib_query.computational_name = this.computational_name;
    }

    return this.librariesInstallationService.getAvailableLibrariesList(lib_query);
  }

  public getDependencies(query: string): Observable<{}> {
    return this.librariesInstallationService.getAvailableDependencies(query);
  }

  public getInstalledLibrariesList(notebook): Observable<{}> {
    return this.librariesInstallationService.getInstalledLibrariesList(
      notebook.project, notebook.name
    );
  }

  private installLibraries(retry?: Library, item?): Observable<{}> {
    const lib_list: any = {
      project_name: this.notebook.project,
      exploratory_name: this.notebook.name,
      libs: retry ? retry : this.selectedLibs
    };
    if (this.computational_name) {
      lib_list.computational_name = this.computational_name;
    }

    if (item) {
      lib_list.computational_name = item;
    } 

    return this.librariesInstallationService.installLibraries(lib_list);
  }

  public isEmpty(obj) {
    if (obj) return Object.keys(obj).length === 0;
  }

  private prepareModel(fnProcessResults: any, fnProcessErrors: any): void {
    this.confirmAction = (retry?: Library, item?) =>
      this.installLibraries(retry, item).subscribe(
        response => fnProcessResults(response),
        error => fnProcessErrors(error)
      );
  }
}
