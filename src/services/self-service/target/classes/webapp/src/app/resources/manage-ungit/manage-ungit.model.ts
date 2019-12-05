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
import { ManageUngitService } from '../../core/services';

export interface AccountCredentials {
  hostname: string;
  username: string;
  email: string;
  login: string;
  password: string;
}

export class MangeUngitModel {
  confirmAction: Function;

  public accoutCredsList: Array<AccountCredentials> = [];
  private continueWith: Function;
  private manageUngitService: ManageUngitService;

  static getDefault(manageUngitService): MangeUngitModel {
    return new MangeUngitModel(() => {}, () => {}, null, manageUngitService);
  }

  constructor(
    fnProcessResults: any,
    fnProcessErrors: any,
    continueWith: Function,
    manageUngitService: ManageUngitService
  ) {
    this.continueWith = continueWith;
    this.manageUngitService = manageUngitService;
    this.prepareModel(fnProcessResults, fnProcessErrors);

    if (this.continueWith) this.continueWith();
  }

  public getGitCredentials(): Observable<{}> {
    return this.manageUngitService.getGitCreds();
  }

  private updateGitCredentials(gitCreds): Observable<{}> {
    return this.manageUngitService.updateGitCredentials({ git_creds: gitCreds });
  }

  private prepareModel(fnProcessResults: any, fnProcessErrors: any): void {
    this.confirmAction = (data?) =>
      this.updateGitCredentials(data).subscribe(
        response => fnProcessResults(response),
        error => fnProcessErrors(error)
      );
  }
}
