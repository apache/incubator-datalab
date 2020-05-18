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

import { Component, ViewChild, OnInit, Inject } from '@angular/core';
import { FormGroup, FormBuilder } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import {MatDialogRef, MAT_DIALOG_DATA, MatDialog} from '@angular/material/dialog';

import { DateUtils, CheckUtils } from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { DataengineConfigurationService } from '../../../core/services';
import { CLUSTER_CONFIGURATION } from '../../computational/computational-resource-create-dialog/cluster-configuration-templates';
import {BucketBrowserComponent} from '../../bucket-browser/bucket-browser.component';
import {CopyPathUtils} from '../../../core/util/copyPathUtils';

@Component({
  selector: 'detail-dialog',
  templateUrl: 'detail-dialog.component.html',
  styleUrls: ['./detail-dialog.component.scss']
})

export class DetailDialogComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  readonly PROVIDER = this.data.notebook.cloud_provider;
  notebook: any;
  upTimeInHours: number;
  upTimeSince: string = '';
  tooltip: boolean = false;
  config: Array<{}> = [];
  bucketStatus: object = {};
  isBucketAllowed = true;
  isCopyIconVissible = {
    project: false,
    shared: false
  };

  public configurationForm: FormGroup;

  @ViewChild('configurationNode', { static: false }) configuration;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private dataengineConfigurationService: DataengineConfigurationService,
    private _fb: FormBuilder,
    public dialogRef: MatDialogRef<DetailDialogComponent>,
    private dialog: MatDialog,
    public toastr: ToastrService,
  ) {

  }

  ngOnInit() {
    this.bucketStatus = this.data.bucketStatus;
    this.notebook = this.data.notebook;
    if (this.notebook) {
      this.tooltip = false;

      this.upTimeInHours = (this.notebook.time) ? DateUtils.diffBetweenDatesInHours(this.notebook.time) : 0;
      this.upTimeSince = (this.notebook.time) ? new Date(this.notebook.time).toString() : '';
      this.initFormModel();
      this.getClusterConfiguration();
    if (this.notebook.edgeNodeStatus === 'terminated' ||
      this.notebook.edgeNodeStatus === 'terminating' ||
      this.notebook.edgeNodeStatus === 'failed') {
      this.isBucketAllowed = false;
    }
    }
  }

  public isEllipsisActive($event): void {
    if ($event.target.offsetWidth < $event.target.scrollWidth)
      this.tooltip = true;
  }

  public getClusterConfiguration(): void {
    this.dataengineConfigurationService
      .getExploratorySparkConfiguration(this.notebook.project, this.notebook.name)
      .subscribe(
        (result: any) => this.config = result,
        error => this.toastr.error(error.message || 'Configuration loading failed!', 'Oops!'));
  }

  public selectConfiguration() {
    if (this.configuration.nativeElement.checked) {

      this.configurationForm.controls['configuration_parameters']
        .setValue(JSON.stringify(this.config.length ? this.config : CLUSTER_CONFIGURATION.SPARK, undefined, 2));
      document.querySelector('#config').scrollIntoView({ block: 'start', behavior: 'smooth' });
    } else {
      this.configurationForm.controls['configuration_parameters'].setValue('');
    }
  }

  public editClusterConfiguration(data): void {
    this.dataengineConfigurationService
      .editExploratorySparkConfiguration(data.configuration_parameters, this.notebook.project, this.notebook.name)
      .subscribe(() => {
        this.dialogRef.close();
      },
        error => this.toastr.error(error.message || 'Edit onfiguration failed!', 'Oops!'));
  }

  public resetDialog() {
    this.initFormModel();

    if (this.configuration) this.configuration.nativeElement['checked'] = false;
  }

  private initFormModel(): void {
    this.configurationForm = this._fb.group({
      configuration_parameters: ['', [this.validConfiguration.bind(this)]]
    });
  }

  private validConfiguration(control) {
    if (this.configuration)
      return this.configuration.nativeElement['checked']
        ? (control.value && control.value !== null && CheckUtils.isJSON(control.value) ? null : { valid: false })
        : null;
  }

  public bucketBrowser(bucketName, endpoint, permition): void {
    permition && this.dialog.open(BucketBrowserComponent, { data:
        {bucket: bucketName, endpoint: endpoint, bucketStatus: this.bucketStatus, buckets: this.data.buckets},
      panelClass: 'modal-fullscreen' })
    .afterClosed().subscribe();
  }

  protected showCopyIcon(bucket) {
    this.isCopyIconVissible[bucket] = true;
  }

  protected copyBucketName(copyValue) {
    CopyPathUtils.copyPath(copyValue);
    this.toastr.success('Bucket name successfully copied!', 'Success!');
  }
}
