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

import { Component, OnInit, Inject, Input, Output, EventEmitter, ViewEncapsulation } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { ConfirmationDialogModel } from './confirmation-dialog.model';
import { UserResourceService, HealthStatusService, ManageEnvironmentsService } from '../../../core/services';
import { HTTP_STATUS_CODES } from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';

@Component({
  selector: 'confirmation-dialog',
  templateUrl: 'confirmation-dialog.component.html',
  styleUrls: ['./confirmation-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})

export class ConfirmationDialogComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;

  model: ConfirmationDialogModel;
  isAliveResources: boolean;
  onlyKilled: boolean = false;
  notebook: any;
  dataengines: Array<any> = [];
  dataengineServices: Array<any> = [];
  confirmationType: number = 0;
  public isClusterLength: boolean;

  @Input() manageAction: boolean = false;
  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public dialogRef: MatDialogRef<ConfirmationDialogComponent>,
    private userResourceService: UserResourceService,
    private healthStatusService: HealthStatusService,
    private manageEnvironmentsService: ManageEnvironmentsService,
    public toastr: ToastrService
  ) {
    this.model = ConfirmationDialogModel.getDefault();
  }

  ngOnInit() {
    if (this.data.type !== 5) {
      this.confirmationType = this.data.type;
      this.notebook = this.data.notebook;
      this.model = new ConfirmationDialogModel(this.confirmationType, this.notebook,
        response => {
          if (response.status === HTTP_STATUS_CODES.OK) this.dialogRef.close(true);
        },
        error => this.toastr.error(error.message || 'Action failed!', 'Oops'),
        this.data.manageAction,
        this.userResourceService,
        this.healthStatusService,
        this.manageEnvironmentsService);

      if (!this.confirmationType) this.filterResourcesByType(this.data.compute);
      this.isAliveResources = this.model.isAliveResources(this.notebook.resources);
      this.onlyKilled = this.notebook.resources ?
        !this.notebook.resources.some(el => el.status !== 'terminated' && el.status !== 'failed')
        : false;
    }

    if (this.data.type === 0 || this.data.type === 1) {
      if (this.data.compute.length) {
        this.isClusterLength = true;
      }
    }
  }

  public confirm() {
    this.model.confirmAction();
  }

  private filterResourcesByType(resources) {
    resources.forEach(resource => {
        (resource.image === 'docker.datalab-dataengine') ? this.dataengines.push(resource) : this.dataengineServices.push(resource);
      });
  }
}
