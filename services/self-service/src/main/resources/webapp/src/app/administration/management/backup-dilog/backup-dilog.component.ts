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

import { Component, OnInit, ViewChild, Output, Inject } from '@angular/core';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { BackupOptionsModel } from '../management.model';

@Component({
  selector: 'dlab-backup-dilog',
  templateUrl: './backup-dilog.component.html',
  styleUrls: ['./backup-dilog.component.scss']
})
export class BackupDilogComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  public backupOptions: BackupOptionsModel = new BackupOptionsModel([], [], [], [], false, false);
  public valid: boolean = true;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public dialogRef: MatDialogRef<BackupDilogComponent>,
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
    this.backupOptions.setDegault();
    this.dialogRef.close();
  }

  private checkValidity(): void {
    const items = [];

    Object.keys(this.backupOptions).forEach(el => {
      if (this.backupOptions[el] instanceof Array) {
        if (this.backupOptions[el][0] && this.backupOptions[el][0] !== 'skip') items.push(this.backupOptions[el][0]);
      } else {
        if (this.backupOptions[el]) items.push(this.backupOptions[el]) ;
      }
    });

    this.valid = items.length > 0;
  }
}
