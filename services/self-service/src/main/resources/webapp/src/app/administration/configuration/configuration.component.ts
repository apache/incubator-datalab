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

import { Component, OnInit, Inject, HostListener, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatTabChangeEvent } from '@angular/material/tabs';
import 'brace';
import 'brace/mode/yaml';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ToastrService } from 'ngx-toastr';

import { HealthStatusService, AppRoutingService, EndpointService } from '../../core/services';
import { ConfigurationService } from '../../core/services/configutration.service';
import { EnvironmentsDataService } from '../management/management-data.service';
import { EnvironmentModel } from '../management/management.model';

@Component({
  selector: 'datalab-configuration',
  templateUrl: './configuration.component.html',
  styleUrls: ['./configuration.component.scss']
})
export class ConfigurationComponent implements OnInit, OnDestroy {
  @ViewChild('selfEditor') selfEditor: ElementRef<HTMLElement>;
  @ViewChild('provEditor') provEditor: ElementRef<HTMLElement>;
  @ViewChild('billingEditor') billingEditor: ElementRef<HTMLElement>;
  private unsubscribe$ = new Subject();
  private healthStatus: any;
  public activeTab = { index: 0 };
  public activeService: string;
  public services = {
    'self-service': { label: 'Self-service', selected: false, config: '', serverConfig: '', isConfigChanged: false },
    'provisioning': { label: 'Provisioning', selected: false, config: '', serverConfig: '', isConfigChanged: false },
    'billing': { label: 'Billing', selected: false, config: '', serverConfig: '', isConfigChanged: false },
  };

  public messagesStatus = {
    success: [],
    error: [],
    counter: 0
  };

  public environmentStatuses = {};
  public environments = [];
  public ingStatuses = ['creating', 'configuring', 'reconfiguring', 'creating image', 'stopping', 'starting', 'terminating'];

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
    private environmentsDataService: EnvironmentsDataService,
    private router: Router,
    public dialog: MatDialog,
    public toastr: ToastrService,
  ) { }

  ngOnInit() {
    this.getEnvironmentHealthStatus();
    this.getEndpoints()
      .subscribe(endpoints => {
        this.endpoints = endpoints;

        this.endpoints = this.endpoints.map(endpoint => ({ name: endpoint.name, status: endpoint.status }));

        this.activeEndpoint = this.endpoints[0].name;
        this.getServicesConfig(this.activeEndpoint);
      });

    this.environmentsDataService.getEnvironmentDataDirect()
      .subscribe((data: any) => {
        this.environments = EnvironmentModel.loadEnvironments(data);
        this.environments.map(env => {
          this.checkResource(env.status, env.endpoint);
          if (env.resources?.length > 0) {
            env.resources.map(resource => {
              this.checkResource(resource.status, env.endpoint);
            });
          }
        });
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  public checkResource(resourceStatus: string, endpoint: string) {
    if (this.ingStatuses.includes(resourceStatus)) {
      if (!this.environmentStatuses[endpoint]) {
        this.environmentStatuses[endpoint] = [];
        this.environmentStatuses[endpoint].push(resourceStatus);
      } else {
        this.environmentStatuses[endpoint].push(resourceStatus);
      }
    }
  }

  private getEnvironmentHealthStatus(): void {
    this.healthStatusService.getEnvironmentHealthStatus()
      .pipe(
        takeUntil(this.unsubscribe$)
      )
      .subscribe((result: any) => {
        this.healthStatus = result;
        !this.healthStatus.admin && !this.healthStatus.projectAdmin && this.appRoutingService.redirectToHomePage();
      });
  }

  private getEndpoints(): Observable<{}> {
    return this.endpointService.getEndpointsData();
  }

  public refreshConfig(): void {
    this.getServicesConfig(this.activeEndpoint);
  }

  public action(action: string): void {
    this.dialog.open(SettingsConfirmationDialogComponent, {
      data: {
        action: action,
        message: action === 'discard' ? this.confirmMessages.discardChanges : this.confirmMessages.saveChanges,
        environmentStatuses: this.environmentStatuses,
        activeEndpoint: this.activeEndpoint
      }, panelClass: 'modal-sm'
    })
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
      });
    this.clearSelectedServices();
  }

  private setServiceConfig(service: string, config: string): void {
    this.configurationService.setServiceConfig(service, config, this.activeEndpoint)
      .pipe(
        takeUntil(this.unsubscribe$)
      )
      .subscribe(
        res => {
          this.getServicesConfig(this.activeEndpoint);
          this.toastr.success('Service configuration saved!', 'Success!');
        },
        error => this.toastr.error(error.message || 'Service configuration is not saved', 'Oops!')
      );
  }

  refreshServiceEditor(editor: ElementRef<HTMLElement>) {
    if (editor) {
      editor.nativeElement.children[3].scrollTop = 100;
      editor.nativeElement.children[3].scrollTop = 0;
    }
  }

  public tabChanged(tabChangeEvent: MatTabChangeEvent): void {
    this.activeTab = tabChangeEvent;

    if (this.activeTab.index === 1 && this.activeEndpoint === 'local') {
      this.activeService = 'self-service';
      this.refreshServiceEditor(this.selfEditor);
    } else if ((this.activeEndpoint !== 'local' && this.activeTab.index === 1) ||
              (this.activeTab.index === 2 && this.activeEndpoint === 'local')) {
      this.activeService = 'provisioning';
      this.refreshServiceEditor(this.provEditor);
    } else if ((this.activeEndpoint !== 'local' && this.activeTab.index === 2) ||
              (this.activeTab.index === 3 && this.activeEndpoint === 'local')) {
      this.activeService = 'billing';
      this.refreshServiceEditor(this.billingEditor);
    } else {
      this.activeService = '';
    }

    if (!!this.activeService) {
      if (this.services[this.activeService].config !== this.services[this.activeService].serverConfig) {
        this.dialog.open(SettingsConfirmationDialogComponent, {
          data: {
            action: 'Was changed',
            environmentStatuses: this.environmentStatuses,
            activeEndpoint: this.activeEndpoint
          }, panelClass: 'modal-sm'
        })
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

  public restartSingleService(ui: boolean, prov: boolean, billing: boolean, serviceName) {
    this.configurationService.restartServices(ui, prov, billing, this.activeEndpoint)
      .pipe(
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        res => {
          this.messagesStatus.success.push(serviceName);
          this.messagesStatus.counter -= 1;
        },
        error => {
          if (serviceName === 'Self-service') {
            this.messagesStatus.success.push(serviceName);
          } else {
            this.messagesStatus.error.push(serviceName);
          }
          this.messagesStatus.counter -= 1;
        }
      );
  }

  public restartServices(): void {
    const selectedServices = [];
    for (const service in this.services) {
      if (this.services[service].selected) {
        selectedServices.push(service);
      }
    }

    this.dialog.open(
      SettingsConfirmationDialogComponent,
      {
        data: {
          action: 'restart',
          services: selectedServices,
          environmentStatuses: this.environmentStatuses,
          activeEndpoint: this.activeEndpoint
        },
        panelClass: 'modal-sm'
      })
      .afterClosed().subscribe(result => {
        if (result) {
          this.messagesStatus.error = [];
          this.messagesStatus.success = [];

          if (this.environmentStatuses[this.activeEndpoint] && this.services['provisioning'].selected) {
            this.services['provisioning'].selected = false;
          }

          if (this.services['self-service'].selected) {
            this.messagesStatus.counter += 1;
            this.restartSingleService(true, false, false, 'Self-service');
          }
          if (this.services['provisioning'].selected) {
            this.messagesStatus.counter += 1;
            this.restartSingleService(false, true, false, 'Provisioning service');
          }
          if (this.services['billing'].selected) {
            this.messagesStatus.counter += 1;
            this.restartSingleService(false, false, true, 'Billing service');
          }

          let timer = setInterval(() => {

            if (this.messagesStatus.counter === 0) {
              for (let key in this.messagesStatus) {
                if (key === 'error' && this.messagesStatus[key].length > 0) {
                  this.toastr.error(`${this.messagesStatus[key].join(', ')} restarting failed`, 'Oops!');
                } else if (key === 'success' && this.messagesStatus[key].length > 0) {
                  this.toastr.success(`${this.messagesStatus[key].join(', ')} restarting started!`, 'Success!');
                }
              }
              clearInterval(timer);
            }
          }, 200);
          this.clearSelectedServices();
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

  get isEndpointsMoreThanOne() {
    return this.endpoints.length > 1;
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
      <ng-template [ngIf]="data.action === 'restart' && !data.environmentStatuses[data.activeEndpoint]?.length" ]>
        <span class="strong">{{data.services.join(', ') | titlecase}}</span>
        <span class="strong" *ngIf="data.services.length > 1 || (data.services.length === 1 && data.services[0] !== 'self-service')"> service</span>
        <span class="strong" [hidden]="(data.services.length < 2) || data.services.length === 2 && data.services[0] === 'self-service'">s</span>: restarting will make DataLab unavailable for some time.
      </ng-template>

      <ng-template [ngIf]="data.action === 'restart' && data.environmentStatuses[data.activeEndpoint]?.length && filterProvisioning.length" ]>
        <span class="strong" >{{filterProvisioning.join(', ') | titlecase}}</span>
        <span class="strong" *ngIf="filterProvisioning.length > 1 || (filterProvisioning.length === 1 && filterProvisioning[0] !== 'self-service')"> service</span>
        <span [hidden]="(filterProvisioning.length < 2) || filterProvisioning.length === 2 && filterProvisioning[0] === 'self-service'">s</span>: restarting will make DataLab unavailable for some time.
      </ng-template>

      <ng-template [ngIf]="data.action === 'restart' && data.environmentStatuses[data.activeEndpoint]?.length && (data?.services?.includes('provisioning') || !filterProvisioning.length)">
        <div class="warning" [ngStyle]="data?.services?.includes('provisioning') && data.services?.length > 1 && {'margin-top': '10px'}">
        <span>Provisioning service: </span>can not be restarted because one of resources is in processing stage. Please try again later.
        </div>
      </ng-template>

      <ng-template [ngIf]="data.action === 'discard'" ]>Discard all unsaved changes.</ng-template>
      <ng-template [ngIf]="data.action === 'save'" ]>After you save changes you need to restart service.</ng-template>
    </div>
    <div class="text-center " *ngIf="!data.environmentStatuses[data.activeEndpoint]?.length || (data.environmentStatuses[data.activeEndpoint]?.length && (!data?.services?.includes('provisioning') || filterProvisioning?.length))">
      <p class="strong">Do you want to proceed?</p>
    </div>
    <div class="text-center m-top-20 pb-25" *ngIf="!data.environmentStatuses[data.activeEndpoint]?.length || (data.environmentStatuses[data.activeEndpoint]?.length && (!data?.services?.includes('provisioning') || filterProvisioning.length))">
      <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
      <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(true)">Yes</button>
    </div>
    <div class="text-center m-top-20 pb-25" *ngIf="data.action === 'restart' && data.environmentStatuses[data.activeEndpoint]?.length && data?.services?.includes('provisioning') && data.services.length === 1">
      <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">Close</button>
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
      label{cursor: pointer;}
      .warning{color: #EF5C4B;}
      .warning span {font-weight: 600;}`
  ]
})

export class SettingsConfirmationDialogComponent implements OnInit {
  filterProvisioning = [];
  constructor(
    public dialogRef: MatDialogRef<SettingsConfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}
  ngOnInit() {
    this.filterProvisioning = this.data?.services?.filter(service => service !== 'provisioning');
  }
}


