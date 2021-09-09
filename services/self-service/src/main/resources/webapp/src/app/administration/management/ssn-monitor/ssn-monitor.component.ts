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

import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { HealthStatusService } from '../../../core/services';

@Component({
  selector: 'datalab-ssn-monitor',
  templateUrl: './ssn-monitor.component.html',
  styleUrls: ['./ssn-monitor.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class SsnMonitorComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;

  public errorMessage: string = '';
  public data: any = null;

  constructor(
    public toastr: ToastrService,
    public dialogRef: MatDialogRef<SsnMonitorComponent>,
    private healthStatusService: HealthStatusService,
  ) { }

  ngOnInit() {
    this.healthStatusService.getSsnMonitorData()
    .subscribe(
      monitorData => this.data = monitorData,
      () => this.toastr.error('Failed ssn data loading!', 'Oops!')
    );
  }

  public isEmpty(obj) {
    if (obj) return Object.keys(obj).length === 0;
  }

  public convertSize(bytes) {
    if (Number(bytes) === 0) return '0 Byte';

    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return parseFloat((bytes / Math.pow(1024, i)).toFixed(3)) + ' ' + sizes[i];
  }
}
