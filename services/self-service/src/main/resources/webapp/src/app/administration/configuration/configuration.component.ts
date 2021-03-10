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

import {Component, OnInit, Inject, HostListener, OnDestroy} from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import {HealthStatusService, AppRoutingService, EndpointService} from '../../core/services';
import {MatTabChangeEvent} from '@angular/material/tabs';
import {Router} from '@angular/router';
import {ConfigurationService} from '../../core/services/configutration.service';
import 'brace';
import 'brace/mode/yaml';
import {ToastrService} from 'ngx-toastr';
import {Observable, Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {Endpoint} from '../management/endpoints/endpoints.component';

@Component({
  selector: 'datalab-configuration',
  templateUrl: './configuration.component.html',
  styleUrls: ['./configuration.component.scss']
})
export class ConfigurationComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject();
  private healthStatus: any;
  public activeTab = {index: 0};
  public activeService: string;
  public services = {
    'self-service': {label: 'Self-service', selected: false, config: '', serverConfig: '', isConfigChanged: false},
    'provisioning': {label: 'Provisioning', selected: false, config: '', serverConfig: '', isConfigChanged: false},
    'billing': {label: 'Billing', selected: false, config: '', serverConfig: '', isConfigChanged: false},
  };

  private confirmMessages = {
    restartService: 'Restarting services will make DataLab unavailable for some time.',
    discardChanges: 'Discard all unsaved changes.',
    saveChanges: 'After you save changes you need to restart service.',
  };
  public activeEndpoint: string;
  public endpoints: Array<string> | any;

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
    private endpointService: EndpointService,
    private configurationService: ConfigurationService,
    private router: Router,
    public dialog: MatDialog,
    public toastr: ToastrService,
  ) { }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
    this.getEndpoints()
      .subscribe(endpoints => {
      this.endpoints = endpoints;
      
      this.endpoints = this.endpoints
        .filter(endpoint => endpoint.status === 'ACTIVE')
        .map(endpoint => endpoint.name);

      this.activeEndpoint = this.endpoints[0];
      this.getServicesConfig(this.activeEndpoint);
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  private getEnvironmentHealthStatus(): void {
    this.healthStatusService.getEnvironmentHealthStatus()
      .pipe(
        takeUntil(this.unsubscribe$)
      )
      .subscribe((result: any) => {
          this.healthStatus = result;
          !this.healthStatus.admin && !this.healthStatus.projectAdmin && this.appRoutingService.redirectToHomePage();
          }
      );
  }

  private getEndpoints(): Observable<{}> {
    return this.endpointService.getEndpointsData();
  }

  public refreshConfig(): void {
    this.getServicesConfig(this.activeEndpoint);
  }

  public action(action: string): void {
    this.dialog.open(SettingsConfirmationDialogComponent, { data: {
        action: action, message: action === 'discard' ? this.confirmMessages.discardChanges : this.confirmMessages.saveChanges
      }, panelClass: 'modal-sm' })
      .afterClosed().subscribe(result => {
      if (result && action === 'save') this.setServiceConfig(this.activeService, this.services[this.activeService].config);
      if (result && action === 'discard') this.services[this.activeService].config = this.services[this.activeService].serverConfig;
      this.configUpdate(this.activeService);
    });
  }

  private getServicesConfig(endpoint): void {
      this.configurationService.getServiceSettings(endpoint)
        .pipe(
          takeUntil(this.unsubscribe$)
        )
        .subscribe(config => {
          for (const service in this.services) {
            const file = `${service}.yml`;
            this.services[service].config = config[file];
            this.services[service].serverConfig = config[file];
            this.configUpdate(service);
          }
        }
      );

    this.clearSelectedServices();
  }

  private setServiceConfig(service: string, config: string): void {
    this.configurationService.setServiceConfig(service, config, this.activeEndpoint)
      .pipe(
        takeUntil(this.unsubscribe$)
      )
      .subscribe(res => {
      this.getServicesConfig(this.activeEndpoint);
      this.toastr.success('Service configuration saved!', 'Success!');
      },
      error => this.toastr.error( error.message || 'Service configuration is not saved', 'Oops!')
    );
  }

  public tabChanged(tabChangeEvent: MatTabChangeEvent): void {
    this.activeTab = tabChangeEvent;
    if (this.activeTab.index === 1) {
      this.activeService = 'self-service';
    } else if (this.activeTab.index === 2) {
      this.activeService = 'provisioning';
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

  private clearSelectedServices(): void {
    Object.keys(this.services).forEach(service => this.services[service].selected = false);
  }

  public toggleSettings(service): void {
    this.services[service].selected = !this.services[service].selected;
  }

  public restartServices(): void  {
    const selectedServices = [];
    for (const service in this.services) {
      if (this.services[service].selected) {
        selectedServices.push(service);
      }
    }

    this.dialog.open(SettingsConfirmationDialogComponent, { data: {
        action: 'restart', services: selectedServices
      }, panelClass: 'modal-sm' })
      .afterClosed().subscribe(result => {
        if (result) {
          this.configurationService.restartServices(this.services['self-service'].selected,
            this.services['provisioning'].selected,
            this.services['billing'].selected,
            this.activeEndpoint
          )
            .pipe(
              takeUntil(this.unsubscribe$),
            )
            .subscribe(res => {
                this.clearSelectedServices();
                this.toastr.success('Service restarting started!', 'Success!');
              },
              error => {
              if (this.services['self-service'].selected) {
                this.clearSelectedServices();
                return this.toastr.success('Service restarting started!', 'Success!');
              } else {
                this.toastr.error('Service restarting failed', 'Oops!');
              }
              }
            );
        } else {
          this.clearSelectedServices();
        }
    });
  }

  public configUpdate(service: string): void {
    this.services[service].isConfigChanged = this.services[service].config !== this.services[service].serverConfig;
  }

  public isServiceSelected(): boolean {
    return Object.keys(this.services).every(service => !this.services[service].selected);
  }


  public setActiveEndpoint(endpoint) {
    this.activeEndpoint = endpoint;
    this.getServicesConfig(this.activeEndpoint);
    this.activeTab.index = 0;
  }
}

@Component({
  selector: 'confirm-dialog',
  template: `
  <div id="dialog-box">
    <div class="dialog-header">
      <h4 class="modal-title"><span class="capitalize">{{ data.action }}</span> <span *ngIf="data.action === 'save' || data.action === 'discard'"> changes</span><span *ngIf="data.action === 'restart'">
        service<span *ngIf="data.services.length > 1">s</span>
      </span></h4>
      <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
    </div>

    <div mat-dialog-content class="content">
      <ng-template [ngIf]="data.action === 'restart'" ]>
        Restarting <span class="strong">{{data.services.join(', ')}}</span> <span *ngIf="data.services.length > 1 || (data.services.length === 1 && data.services[0] !== 'self-service')">
        service</span><span [hidden]="(data.services.length < 2) || data.services.length === 2 && data.services[0] === 'self-service'">s</span> will make DataLab unavailable for some time.
      </ng-template>
      <ng-template [ngIf]="data.action === 'discard'" ]>Discard all unsaved changes.</ng-template>
      <ng-template [ngIf]="data.action === 'save'" ]>After you save changes you need to restart service.</ng-template>
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
      .content{padding: 35px 30px 30px 30px; text-align: center;}
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


