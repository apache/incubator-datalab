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

import {Component, OnInit, Inject, HostListener} from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import {HealthStatusService, AppRoutingService} from '../../core/services';
import {MatTabChangeEvent} from '@angular/material/tabs';
import {Router} from '@angular/router';
import {ConfigurationService} from '../../core/services/configutration.service';
import 'brace';
import 'brace/mode/yaml';

@Component({
  selector: 'datalab-configuration',
  templateUrl: './configuration.component.html',
  styleUrls: ['./configuration.component.scss']
})
export class ConfigurationComponent implements OnInit {
  private healthStatus: any;
  public activeTab = {index: 0};
  public activeService: string;
  public services = {
    'self-service': {label: 'Self service', selected: false, config: '', serverConfig: '', isConfigChanged: false},
    'provisioning-service': {label: 'Provisioning service', selected: false, config: '', serverConfig: '', isConfigChanged: false},
    'billing': {label: 'Billing', selected: false, config: '', serverConfig: '', isConfigChanged: false},
  };

  private confirmMessages = {
    restartService: 'Restarting services will make DataLab unavailable for some time.',
    discardChanges: 'Discard all unsaved changes.',
    saveChanges: 'After you save changes you need to restart service.',
  };

  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    if ((event.metaKey || event.ctrlKey) &&
      event.key === 's' &&
      this.activeTab.index !== 0 &&
      this.router.url === '/configuration' &&
      this.services[this.activeService].config !== this.services[this.activeService].serverConfig
    ) {
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
    this.getServicesConfig(...Object.keys(this.services));
  }

  private getEnvironmentHealthStatus() {
    this.healthStatusService.getEnvironmentHealthStatus()
      .subscribe((result: any) => {
          this.healthStatus = result;
          !this.healthStatus.admin && !this.healthStatus.projectAdmin && this.appRoutingService.redirectToHomePage();
          }
      );
  }

  public refreshConfig() {
    this.getServicesConfig(...Object.keys(this.services));
  }

  public action(action) {
    this.dialog.open(SettingsConfirmationDialogComponent, { data: {
        action: action, message: action === 'discard' ? this.confirmMessages.discardChanges : this.confirmMessages.saveChanges
      }, panelClass: 'modal-sm' })
      .afterClosed().subscribe(result => {
      if (result && action === 'save') this.setServiceConfig(this.activeService, this.services[this.activeService].config);
      if (result && action === 'discard') this.services[this.activeService].config = this.services[this.activeService].serverConfig;
      this.configUpdate(this.activeService);
    });
  }

  private getServicesConfig(...services) {
    services.forEach(service => {
      this.configurationService.getServiceSettings(service).subscribe(config => {
          this.services[service].config = config;
          this.services[service].serverConfig = config;
        this.configUpdate(service);
        }
      );
    });
    this.clearSelectedServices();
  }

  private setServiceConfig(service, config) {
    this.configurationService.setServiceConfig(service, config).subscribe(res => {
      this.getServicesConfig(service);
      }
    );
  }

  public tabChanged(tabChangeEvent: MatTabChangeEvent): void {
    this.activeTab = tabChangeEvent;
    if (this.activeTab.index === 1) {
      this.activeService = 'provisioning-service';
    } else if (this.activeTab.index === 2) {
      this.activeService = 'self-service';
    } else if (this.activeTab.index === 3) {
      this.activeService = 'billing';
    } else {
      this.activeService = '';
    }

    if (!!this.activeService) {
      if (this.services[this.activeService].config !== this.services[this.activeService].serverConfig) {
        this.dialog.open(SettingsConfirmationDialogComponent, { data: {
            action: 'Was changed'
          }, panelClass: 'modal-sm' })
          .afterClosed().subscribe(result => {
          if (result) {
            this.services[this.activeService].serverConfig = this.services[this.activeService].config;
          } else {
            this.services[this.activeService].config = this.services[this.activeService].serverConfig;
          }
        });
      }
    }
    this.clearSelectedServices();
  }

  private clearSelectedServices() {
    Object.keys(this.services).forEach(service => this.services[service].selected = false);
  }

  public toggleSetings(service) {
    this.services[service].selected = !this.services[service].selected;
  }

  public restartServices() {
    this.dialog.open(SettingsConfirmationDialogComponent, { data: {
        action: 'Restart services', message: this.confirmMessages.restartService
      }, panelClass: 'modal-sm' })
      .afterClosed().subscribe(result => {
      this.configurationService.restartServices(this.services['self-service'].selected,
        this.services['provisioning-service'].selected,
        this.services['billing'].selected
      )
        .subscribe(res => {
          this.clearSelectedServices();
        }
      );

    });
  }

  public configUpdate(service: string) {
    this.services[service].isConfigChanged = this.services[service].config !== this.services[service].serverConfig;
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
      header { display: flex; justify-content: space-between; color: #607D8B; }
      header h4 i { vertical-align: bottom; }
      header a i { font-size: 20px; }
      header a:hover i { color: #35afd5; cursor: pointer; }
      .content{padding: 35px 30px 30px 30px;}
      label{cursor: pointer}`
  ]
})

export class SettingsConfirmationDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<SettingsConfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {

  }
}


