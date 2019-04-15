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

import { Component, EventEmitter, Input, Output, ViewChild, Inject, ViewContainerRef } from '@angular/core';
import { ToastsManager } from 'ng2-toastr';

import { UserResourceService } from '../../../core/services';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';

@Component({
  selector: 'computational-resources-list',
  templateUrl: 'computational-resources-list.component.html',
  styleUrls: ['./computational-resources-list.component.scss']
})

export class ComputationalResourcesListComponent {
  @ViewChild('confirmationDialog') confirmationDialog;
  @ViewChild('detailComputationalResource') detailComputationalResource;
  @ViewChild('clusterScheduler') clusterScheduler;

  @Input() resources: any[];
  @Input() environment: any[];
  @Input() healthStatus: string;

  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(
    private userResourceService: UserResourceService,
    public dialog: MatDialog,
    public toastr: ToastsManager,
    public vcr: ViewContainerRef
  ) {
    this.toastr.setRootViewContainerRef(vcr);
  }

  toggleResourceAction(resource, action: string) {
    if (action === 'stop' || action === 'terminate') {
      const dialogRef: MatDialogRef<ConfirmationDialogComponent> = this.dialog.open(ConfirmationDialogComponent,
        { data: {action, resource}, width: '550px' });
      dialogRef.afterClosed().subscribe(result => {
        if (result && action === 'stop') {
          this.userResourceService
            .toggleStopStartAction(this.environment['name'], resource.computational_name, action)
            .subscribe(() => {
              this.rebuildGrid();
            });
        } else if (result && action === 'terminate') {
          this.userResourceService
            .suspendComputationalResource(this.environment['name'], resource.computational_name)
            .subscribe(() => {
              this.rebuildGrid();
            });
        }
      });
    } else if (action === 'start') {
      this.userResourceService
        .toggleStopStartAction(this.environment['name'], resource.computational_name, 'start')
        .subscribe(
          () => this.rebuildGrid(),
          error => this.toastr.error(error.message || 'Computational resource starting failed!', 'Oops!', { toastLife: 5000 }));
    }
  }

  rebuildGrid(): void {
    this.buildGrid.emit();
  }

  detailComputationalResources(environment, resource): void {
    this.detailComputationalResource.open({ isFooter: false }, environment, resource);
  };

  openScheduleDialog(resource) {
    this.clusterScheduler.open({ isFooter: false }, this.environment, 'СOMPUTATIONAL', resource);
  }
}


@Component({
  selector: 'confirmation-dialog',
  template: `
  <div mat-dialog-content class="content">

    <p>Computational resource <strong> {{ data.resource.computational_name }}</strong> will be 
      <span *ngIf="data.action === 'terminate'"> decommissioned.</span>
      <span *ngIf="data.action === 'stop'">stopped.</span>
    </p>
    <p class="m-top-20"><strong>Do you want to proceed?</strong></p>
  </div>
  <div class="text-center">
    <button type="button" class="butt" mat-raised-button (click)="dialogRef.close()">No</button>
    <button type="button" class="butt butt-success" mat-raised-button (click)="dialogRef.close(true)">Yes</button>
  </div>
  `,
  styles: [`
    .content { color: #718ba6; padding: 20px 50px; font-size: 14px; font-weight: 400 }
  `]
})
export class ConfirmationDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}
