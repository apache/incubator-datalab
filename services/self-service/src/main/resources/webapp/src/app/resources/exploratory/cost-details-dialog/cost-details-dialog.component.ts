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

import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';

@Component({
  selector: 'cost-details-dialog',
  templateUrl: 'cost-details-dialog.component.html',
  styleUrls: ['cost-details-dialog.component.scss']
})
export class CostDetailsDialogComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  public notebook: any;
  public provider: string;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public dialogRef: MatDialogRef<CostDetailsDialogComponent>
  ) { }

  ngOnInit() {
    this.notebook = this.data;
    this.provider = this.notebook.cloud_provider;
  }
}
