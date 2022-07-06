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
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { map } from 'rxjs/operators';

import { EndpointService } from '../../../core/services';
import { NotificationDialogComponent } from '../../../shared/modal-dialog/notification-dialog';
import { PATTERNS } from '../../../core/util';

export interface Endpoint {
  name: string;
  url: string;
  account: string;
}

@Component({
  selector: 'endpoints',
  templateUrl: './endpoints.component.html',
  styleUrls: ['./endpoints.component.scss']
})
export class EndpointsComponent implements OnInit {
  public createEndpointForm: FormGroup;
  public maxEndpointNameLength: number = 6;
  endpoints: Endpoint[] = [];
  displayedColumns: string[] = ['name', 'url', 'account', 'endpoint_tag', 'actions'];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    public toastr: ToastrService,
    public dialogRef: MatDialogRef<EndpointsComponent>,
    public dialog: MatDialog,
    private endpointService: EndpointService,
    private _fb: FormBuilder,
  ) { }

  ngOnInit() {
    this.initFormModel();
    this.getEndpointList();
  }

  public generateEndpointTag($event) {
    this.createEndpointForm.controls.endpoint_tag.setValue($event.target.value.toLowerCase());
  }

  public assignChanges(data) {
    this.endpointService.createEndpoint(data).subscribe(() => {
      this.toastr.success('Endpoint connected successfully!', 'Success!');
      this.dialogRef.close(true);
    }, error => this.toastr.error(error.message || 'Endpoint connection failed!', 'Oops!'));
  }

  public deleteEndpoint(data): void {
    this.endpointService.getEndpointsResource(data.name)
      .pipe(
        map(resource =>
          resource.projects
            .map(project =>
              EndpointsComponent.createResourceList(
                project.name,
                resource.exploratories.filter(notebook => notebook.project === project.name),
                project.endpoints.filter(endpoint => endpoint.name === data.name)[0].status)
            )
            .filter(project => project.nodeStatus !== 'TERMINATED'
              && project.nodeStatus !== 'TERMINATING'
              && project.nodeStatus !== 'FAILED'
            )
        )
      )
      .subscribe((resource: any) => {
        this.dialog.open(NotificationDialogComponent, {
          data: {
            type: 'confirmation', item: data, list: resource
          }, panelClass: 'modal-sm'
        })
          .afterClosed().subscribe(result => {
            result && this.deleteEndpointOption(data);
          });
      });
  }

  public getEndpoinConnectionStatus(url) {
    const getStatus = this.endpointService.getEndpoinConnectionStatus(encodeURIComponent(url));
    this.dialog.open(EndpointTestResultDialogComponent, { data: { url: url, getStatus }, panelClass: 'modal-sm' });
  }

  private static createResourceList(name: string, resource: Array<any>, nodeStatus: string): Object {
    return { name, resource, nodeStatus };
  }

  private initFormModel(): void {
    this.createEndpointForm = this._fb.group({
      name: ['', Validators.compose([
        Validators.required,
        Validators.pattern(PATTERNS.namePattern),
        this.validateName.bind(this),
        this.providerMaxLength.bind(this)
      ])],
      url: ['', Validators.compose([
        Validators.required,
        Validators.pattern(PATTERNS.fullUrl),
        this.validateUrl.bind(this)
      ])],
      account: ['', Validators.compose([
        Validators.required,
        Validators.pattern(PATTERNS.namePattern)
      ])],
      endpoint_tag: ['', Validators.compose([
        Validators.required,
        Validators.pattern(PATTERNS.namePattern)
      ])]
    });
  }

  private deleteEndpointOption(data): void {
    this.endpointService.deleteEndpoint(`${data.name}`)
      .subscribe(
        () => {
          this.toastr.success('Endpoint successfully disconnected. All related resources are terminating!', 'Success!');
          this.getEndpointList();
        },
        error => this.toastr.error(error.message || 'Endpoint creation failed!', 'Oops!')
      );
  }

  private getEndpointList(): void {
    this.endpointService.getEndpointsData().subscribe((endpoints: any) => this.endpoints = endpoints);
  }

  private validateUrl(control) {
    if (control && control.value) {
      const isDublicat = this.endpoints.some(endpoint => endpoint['url'].toLocaleLowerCase() === control.value.toLowerCase());
      return isDublicat ? { isDuplicate: true } : null;
    }
  }

  private validateName(control) {
    if (control && control.value) {
      const isDublicat = this.endpoints.some(endpoint => endpoint['name'].toLocaleLowerCase() === control.value.toLowerCase());
      return isDublicat ? { isDuplicate: true } : null;
    }
  }

  private providerMaxLength(control) {
    return control.value.length <= this.maxEndpointNameLength ? null : { limit: true };
  }
}

@Component({
  selector: 'endpoint-test-result-dialog',
  template: `
    <div id="dialog-box">
      <div class="dialog-header">
        <h4 class="modal-title">Endpoint test</h4>
        <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
      </div>
      <div class="progress-bar" >
        <mat-progress-bar *ngIf="!response" mode="indeterminate"></mat-progress-bar>
      </div>
      <div class="content-box">
      <div mat-dialog-content class="content message">
        <p
          class="dialog-message ellipsis"
          *ngIf="!response">
          Connecting to url
          <span class="strong"
                matTooltip="{{data.url}}"
                [matTooltipPosition]="'above'"
          >
            {{cutToLongUrl(data.url)}}
          </span>
        </p>
        <p
          class="dialog-message ellipsis"
          *ngIf="isConnected && response">
          <i class="material-icons icons-possition active">check_circle</i>
          Connected to url
          <span matTooltip="{{data.url}}"
                [matTooltipPosition]="'above'"
                class="strong"
          >
            {{cutToLongUrl(data.url)}}
          </span>
        </p>
        <p class="dialog-message ellipsis"
           *ngIf="!isConnected && response"
        >
          <i class="material-icons icons-possition failed">cancel</i>
          Failed to connect to url
          <span matTooltip="{{data.url}}"
                [matTooltipPosition]="'above'"
                class="strong"
          >
            {{cutToLongUrl(data.url)}}
          </span>
        </p>
      </div>
      <div class="text-center m-top-20 m-bott-10">
        <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">Close</button>
      </div>
      </div>
    </div>
  `,
  styles: [
    `#dialog-box {overflow: hidden}
    .icons-possition {line-height: 25px; vertical-align: middle; padding-right: 7px }
    .content { color: #718ba6; padding: 15px 50px; font-size: 14px; font-weight: 400; margin: 0; }
    .info .confirm-dialog { color: #607D8B; }
    header { display: flex; justify-content: space-between; color: #607D8B; }
    header h4 i { vertical-align: bottom; }
    header a i { font-size: 20px; }
    header a:hover i { color: #35afd5; cursor: pointer; }
    label { font-size: 15px; font-weight: 500; font-family: "Open Sans",sans-serif; cursor: pointer; display: flex; align-items: center;}
    .progress-bar{ height: 4px;}
    .dialog-message{min-height: 25px; overflow: hidden;}
    `
  ]
})
export class EndpointTestResultDialogComponent {
  public isConnected = false;
  public response = false;
  constructor(
    public dialogRef: MatDialogRef<EndpointTestResultDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.data.getStatus
      .subscribe(
        () => {
          this.isConnected = true;
          this.response = true;
          return;
        },
        () => {
          this.isConnected = false;
          this.response = true;
          return;
        });
  }
  public cutToLongUrl(url) {
    return url.length > 25 ? url.slice(0, 25) + '...' : url;
  }
}
