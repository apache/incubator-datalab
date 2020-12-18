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

import {Component, OnInit, Output, EventEmitter, Inject, ViewChild, HostListener} from '@angular/core';
import { ValidatorFn, FormControl } from '@angular/forms';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import {RolesGroupsService, HealthStatusService, ApplicationSecurityService, AppRoutingService} from '../../core/services';
import {CheckUtils, SortUtils} from '../../core/util';
import { DICTIONARY } from '../../../dictionary/global.dictionary';
import {ProgressBarService} from '../../core/services/progress-bar.service';
import {ConfirmationDialogComponent, ConfirmationDialogType} from '../../shared/modal-dialog/confirmation-dialog';
import {MatTabChangeEvent} from '@angular/material/tabs';
import {Router} from '@angular/router';
import {ConfigurationService} from '../../core/services/configutration.service';
import {NotificationDialogComponent} from '../../shared/modal-dialog/notification-dialog';
import {logger} from 'codelyzer/util/logger';

@Component({
  selector: 'datalab-configuration',
  templateUrl: './configuration.component.html',
  styleUrls: ['./configuration.component.scss']
})
export class ConfigurationComponent implements OnInit {
  private healthStatus: any;
  editorOptions = {theme: 'vs-dark', language: 'javascript'};
  code: string = 'function x() {console.log("Hello world!");}';
  text: any;
  public activeTab: number = 0;
  selvServiceSource;
  provisioningSource;
  billingSource;
  serverConfigs = {
    selvServiceSource: {},
    provisioningSource: {},
    billingSource: {},
  };
  public services = [
    {label: 'Self service', selected: false},
    {label: 'Provisioning', selected: false},
    {label: 'Billing', selected: false},
  ];

  private confirmMessages = {
    restartService: 'Restarting services will make DataLab unavailable for some time.',
    discardChanges: 'Discard all unsaved changes.',
    saveChanges: 'After you save changes you need to restart service.',
  };

  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    if ((event.metaKey || event.ctrlKey) && event.key === 's' && this.activeTab !== 0 && this.router.url === '/configuration') {
      this.action('save');
      event.preventDefault();
    }
  }

  constructor(
    private healthStatusService: HealthStatusService,
    private appRoutingService: AppRoutingService,
    private configurationService: ConfigurationService,
    private router: Router,
    public dialog: MatDialog
  ) { }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
    this.getSettings();
  }
  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe((result: any) => {
          this.healthStatus = result;
          if (!this.healthStatus.admin && !this.healthStatus.projectAdmin) {
            this.appRoutingService.redirectToHomePage();
          } else {

          }
        }
      );
  }

  refresh() {
    console.log('Refresh');
  }

  action(action) {
    this.dialog.open(SettingsConfirmationDialogComponent, { data: {
        action: action, message: action === 'discard' ? this.confirmMessages.discardChanges : this.confirmMessages.saveChanges
      }, panelClass: 'modal-sm' })
      .afterClosed().subscribe(result => {
      console.log(action, this.activeTab);
    });
  }

  getSettings() {
    this.configurationService.getServiceSettings('self-service').subscribe(v => {
      this.serverConfigs.billingSource = v;
      this.serverConfigs.selvServiceSource = v;
      this.serverConfigs.provisioningSource = v;
      this.selvServiceSource = v;
      this.provisioningSource = v;
      this.billingSource = v;
    }
    );
  }

  public tabChanged(tabChangeEvent: MatTabChangeEvent): void {
    this.activeTab = tabChangeEvent.index;
    if (this.selvServiceSource !== this.serverConfigs.selvServiceSource) {
      this.dialog.open(SettingsConfirmationDialogComponent, { data: {
          action: 'Was changed'
        }, panelClass: 'modal-sm' })
        .afterClosed().subscribe(result => {
        if (result) {
          this.serverConfigs.selvServiceSource = this.selvServiceSource;
        } else {
          this.selvServiceSource = this.serverConfigs.selvServiceSource;
        }
        });
    }
  }

  toggleSetings(service) {
      service.selected = !service.selected
  }

  restartServices() {
    this.dialog.open(SettingsConfirmationDialogComponent, { data: {
        action: 'Restart services', message: this.confirmMessages.restartService
      }, panelClass: 'modal-sm' })
      .afterClosed().subscribe(result => {
    });
  }
}

@Component({
  selector: 'confirm-dialog',
  template: `
  <div id="dialog-box">
    <div class="dialog-header">
      <h4 class="modal-title"><span class="capitalize">{{ data.action }}</span> <span *ngIf="data.action === 'save' || data.action === 'discard'"> changes</span></h4>
      <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
    </div>

    <div mat-dialog-content class="content">
      {{data.message}}
    </div>
    <div class="text-center ">
      <p class="strong">Do you want to proceed?</p>
    </div>
    <div class="text-center m-top-20 pb-25">
      <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
      <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(true)">Yes</button>
    </div>
  </div>
  `,
  styles: [
    `
      .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400; margin: 0; }
      .info { color: #35afd5; }
      .info .confirm-dialog { color: #607D8B; }
      header { display: flex; justify-content: space-between; color: #607D8B; }
      header h4 i { vertical-align: bottom; }
      header a i { font-size: 20px; }
      header a:hover i { color: #35afd5; cursor: pointer; }
      .content{padding: 35px 30px 30px 30px;}
      .plur { font-style: normal; }
      .scrolling-content{overflow-y: auto; max-height: 200px; }
      .cluster { width: 50%; text-align: left;}
      .status { width: 50%;text-align: left;}
      .label { font-size: 15px; font-weight: 500; font-family: "Open Sans",sans-serif;}
      .node { font-weight: 300;}
      .resource-name { width: 40%;text-align: left; padding: 10px 0;line-height: 26px;}
      .clusters-list { width: 60%;text-align: left; padding: 10px 0;line-height: 26px;}
      .clusters-list-item { width: 100%;text-align: left;display: flex}
      .resource-list{max-width: 100%; margin: 0 auto;margin-top: 20px; }
      .resource-list-header{display: flex; font-weight: 600; font-size: 16px;height: 48px; border-top: 1px solid #edf1f5; border-bottom: 1px solid #edf1f5; padding: 0 20px;}
      .resource-list-row{display: flex; border-bottom: 1px solid #edf1f5;padding: 0 20px;}
      .confirm-resource-terminating{text-align: left; padding: 10px 20px;}
      .confirm-message{color: #ef5c4b;font-size: 13px;min-height: 18px; text-align: center; padding-top: 20px}
      .checkbox{margin-right: 5px;vertical-align: middle; margin-bottom: 3px;}
      label{cursor: pointer}
      .bottom-message{padding-top: 15px;}
      .table-header{padding-bottom: 10px;}`
  ]
})

export class SettingsConfirmationDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<SettingsConfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {

  }
}


