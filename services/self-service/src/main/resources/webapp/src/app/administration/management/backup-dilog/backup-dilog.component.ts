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

import { Component, OnInit, Output, EventEmitter, Inject } from '@angular/core';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { ToastrService } from 'ngx-toastr';

import { BackupOptionsModel } from '../management.model';
import { BackupService } from '../../../core/services';

@Component({
  selector: 'dlab-backup-dilog',
  templateUrl: './backup-dilog.component.html',
  styleUrls: ['./backup-dilog.component.scss']
})
export class BackupDilogComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  public backupOptions: BackupOptionsModel = new BackupOptionsModel([], [], [], [], false, false);
  public valid: boolean = true;

  private clear = undefined;
  @Output() preventbackup: EventEmitter<{}> = new EventEmitter();

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public dialogRef: MatDialogRef<BackupDilogComponent>,
    public toastr: ToastrService,
    private backupService: BackupService,
  ) { }

  ngOnInit() {
    this.backupOptions.setDegault();
    this.valid = true;
  }

  public onHoldChanged($event, key): void {
    this.backupOptions[key] instanceof Array
      ? (this.backupOptions[key][0] = $event.checked ? 'all' : 'skip')
      : (this.backupOptions[key] = !this.backupOptions[key]);

    this.checkValidity();
  }

  public applyOptions(): void {
    this.backupService.createBackup(this.backupOptions).subscribe(result => {
      this.getBackupStatus(result);
      this.toastr.success('Backup configuration is processing!', 'Processing!');
      this.clear = window.setInterval(() => this.getBackupStatus(result), 3000);
      this.dialogRef.close(this.backupOptions);
    },
      error => this.toastr.error(error.message, 'Oops!'));
  }

  private checkValidity(): void {
    const items = [];

    Object.keys(this.backupOptions).forEach(el => {
      if (this.backupOptions[el] instanceof Array) {
        if (this.backupOptions[el][0] && this.backupOptions[el][0] !== 'skip') items.push(this.backupOptions[el][0]);
      } else {
        if (this.backupOptions[el]) items.push(this.backupOptions[el]);
      }
    });

    this.valid = items.length > 0;
  }

  private getBackupStatus(result) {
    const uuid = result.body;
    this.backupService.getBackupStatus(uuid)
      .subscribe((backupStatus: any) => {
        if (!this.creatingBackup) {
          backupStatus.status === 'FAILED'
            ? this.toastr.error('Backup configuration failed!', 'Oops!')
            : this.toastr.success('Backup configuration completed!', 'Success!');
          clearInterval(this.clear);
        }
      }, () => {
        clearInterval(this.clear);
        this.toastr.error('Backup configuration failed!', 'Oops!');
      });
  }

  get creatingBackup(): boolean {
    return this.backupService.inProgress;
  }
}
