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
/* tslint:disable:no-empty */

import { Observable } from 'rxjs/Observable';
import { Response } from '@angular/http';
import { UserAccessKeyService } from '../../../core/services';

export class KeyUploadDialogModel {

  confirmAction: Function;
  private userAccessKeyService: UserAccessKeyService;

  private accessKeyLabel: string;
  private accessKeyFormValid: boolean;
  private newAccessKeyForUpload: File;

  static getDefault(): KeyUploadDialogModel {
    return new KeyUploadDialogModel(null, () => { }, () => { }, null);
  }

  constructor(
    newAccessKeyForUpload: any,
    fnProcessResults: any,
    fnProcessErrors: any,
    userAccessKeyService: UserAccessKeyService
  ) {
    this.userAccessKeyService = userAccessKeyService;
    this.prepareModel(newAccessKeyForUpload, fnProcessResults, fnProcessErrors);
  }

  setUserAccessKey(accessKey: File): void {
    if (accessKey && (this.accessKeyFormValid = this.isValidKey(accessKey.name))) {
      this.newAccessKeyForUpload = accessKey;
    }
    this.accessKeyLabel = this.getLabel(accessKey);
  }

  private uploadUserAccessKey(primary?: boolean): Observable<Response | {}> {
    const formData = new FormData();
    formData.append('file', this.newAccessKeyForUpload);

    return primary ? this.userAccessKeyService.uploadUserAccessKey(formData) : this.userAccessKeyService.reuploadUserAccessKey(formData)
  }

  private prepareModel(newAccessKeyForUpload: any, fnProcessResults: any, fnProcessErrors: any): void {
    this.setUserAccessKey(newAccessKeyForUpload);
    this.confirmAction = (primary?: boolean) => this.uploadUserAccessKey(primary)
      .subscribe(
      (response: Response) => fnProcessResults(response),
      (response: Response) => fnProcessErrors(response));
  }

  private getLabel(file: File): string {
    if (file)
      return !this.accessKeyFormValid ? 'Public key is required.' : file.name;
    return '';
  }

  private isValidKey(value): boolean {
    if (value.toLowerCase().endsWith('.pub'))
      return true;
    return false;
  }
}
