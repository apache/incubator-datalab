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
import { MatDialogRef, MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';

import { DateUtils, CheckUtils, HelpUtils } from '../../../core/util';
import { DICTIONARY } from '../../../../dictionary/global.dictionary';
import { DataengineConfigurationService } from '../../../core/services';
import { CLUSTER_CONFIGURATION } from '../../computational/computational-resource-create-dialog/cluster-configuration-templates';
import { CopyPathUtils } from '../../../core/util/copyPathUtils';
import { AuditService } from '../../../core/services/audit.service';
import { BucketBrowserComponent } from '../../bucket-browser/bucket-browser.component';

@Component({
  selector: 'detail-dialog',
  templateUrl: 'detail-dialog.component.html',
  styleUrls: ['./detail-dialog.component.scss']
})

export class DetailDialogComponent implements OnInit {
  readonly DICTIONARY = DICTIONARY;
  readonly PROVIDER = this.data.pro?.cloud_provider?.toLowerCase() || this.data.odahu?.cloud_provider?.toLowerCase();
  public isCopied: boolean = true;
  notebook: any;
  upTimeInHours: number;
  tooltip: boolean = false;
  config: Array<{}> = [];
  bucketStatus: object = {};
  isBucketAllowed = true;
  isCopyIconVissible: {bucket} = {bucket: false};
  public odahu: any;
  public configurationForm: FormGroup;
  @ViewChild('configurationNode') configuration;
  urlMaxLength: number = 38;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private dataengineConfigurationService: DataengineConfigurationService,
    private _fb: FormBuilder,
    public dialogRef: MatDialogRef<DetailDialogComponent>,
    private dialog: MatDialog,
    public toastr: ToastrService,
    public auditService: AuditService
  ) {
    if (data.notebook) {
      this.notebook = data.notebook;
      this.PROVIDER = this.data.notebook.cloud_provider;
    }

    if (data.odahu) {
      this.odahu = data.odahu;
      this.PROVIDER = this.data.odahu.cloud_provider || 'azure';
    }
  }

  ngOnInit() {
    this.bucketStatus = this.data.bucketStatus;
    this.notebook = this.data.notebook;
    if (this.notebook) {
      this.tooltip = false;
      this.upTimeInHours = (this.notebook.time) ? DateUtils.diffBetweenDatesInHours(this.notebook.time) : 0;
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
        error => this.toastr.error(error.message || 'Configuration loading failed!', 'Oops!')
      );
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
      .subscribe(
        () => {
          this.dialogRef.close();
        },
        error => this.toastr.error(error.message || 'Edit onfiguration failed!', 'Oops!')
      );
  }

  public resetDialog() {
    this.initFormModel();
    if (this.configuration) {
      this.configuration.nativeElement['checked'] = false;
    }
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
    if (!permition) {
      return;
    }
    bucketName = this.isBucketAllowed ? bucketName : this.data.buckets[0].children[0].name;
    // bucketName = 'ofuks-1304-pr2-local-bucket';
    this.dialog.open(BucketBrowserComponent, { data:
      {
        bucket: bucketName,
        endpoint: endpoint,
        bucketStatus: this.bucketStatus,
        buckets: this.data.buckets
      },
      panelClass: 'modal-fullscreen' }
    ).afterClosed().subscribe();
  }

  public showCopyIcon(element) {
    this.isCopyIconVissible[element] = true;
  }

  public hideCopyIcon() {
    for (const key in this.isCopyIconVissible) {
      this.isCopyIconVissible[key] = false;
    }
    this.isCopied = true;
  }

  public copyLink(copyValue, isBucket?) {
    const protocol = isBucket ? HelpUtils.getBucketProtocol(this.PROVIDER) : '';
    CopyPathUtils.copyPath(protocol + copyValue);
  }

  public logAction(name: any, description: string, copy?: string) {
    if (copy) {
      this.auditService.sendDataToAudit({resource_name: name, info: `Copy ${description} link`, type: 'NOTEBOOK'}).subscribe();
    } else {
      this.auditService.sendDataToAudit({resource_name: name, info: `Follow ${description} link`, type: 'NOTEBOOK'}).subscribe();
    }
  }
}
