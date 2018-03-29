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

import { Component, OnInit, EventEmitter, Output, ViewChild } from '@angular/core';
import { Response } from '@angular/http';

import { KeyUploadDialogModel } from './key-upload.model';
import { UserAccessKeyService } from '../../../core/services';
import { ErrorMapUtils, HTTP_STATUS_CODES } from '../../../core/util';

@Component({
  moduleId: module.id,
  selector: 'key-upload-dialog',
  templateUrl: 'key-upload-dialog.component.html'
})

export class UploadKeyDialogComponent implements OnInit {
  model: KeyUploadDialogModel;
  processError: boolean = false;
  errorMessage: string = '';
  
  @ViewChild('bindDialog') bindDialog;
  @ViewChild('userAccessKeyUploadControl') userAccessKeyUploadControl;
  @Output() checkInfrastructureCreationProgress: EventEmitter<{}> = new EventEmitter();
  @Output() generateUserKey: EventEmitter<{}> = new EventEmitter();

  constructor(private userAccessKeyService: UserAccessKeyService) {
    this.model = KeyUploadDialogModel.getDefault();
  }

  ngOnInit() {
    this.bindDialog.onClosing = () => this.resetDialog();
  }

  public uploadUserAccessKey_onChange($event) {
    if ($event.target.files.length > 0) {
      this.model.setUserAccessKey($event.target.files[0]);
    }
  }

  public generateUserAccessKey_btnClick($event) {
    this.generateUserKey.emit($event);
    this.close();
  }

  public uploadUserAccessKey_btnClick($event) {
    this.model.confirmAction();
    $event.preventDefault();
    return false;
  }

  public open(params) {
    if (!this.bindDialog.isOpened) {
      this.model = new KeyUploadDialogModel(null, (response: Response) => {
        if (response.status === HTTP_STATUS_CODES.OK) {
          this.close();
          this.checkInfrastructureCreationProgress.emit();
        }
      },
        (response: Response) => {
          this.processError = true;
          this.errorMessage = ErrorMapUtils.setErrorMessage(response);
        },
        this.userAccessKeyService);

      this.bindDialog.open(params);
    }
  }

  public close() {
    if (this.bindDialog.isOpened)
      this.bindDialog.close();
  }

  private resetDialog(): void {
    this.userAccessKeyUploadControl.nativeElement.value = '';

    this.processError = false;
    this.errorMessage = '';
  }
}
