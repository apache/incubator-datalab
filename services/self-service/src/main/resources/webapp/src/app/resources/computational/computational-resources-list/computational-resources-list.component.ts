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

import { Component, EventEmitter, Input, Output, Inject } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { ToastrService } from 'ngx-toastr';

import { UserResourceService } from '../../../core/services';
import { DetailComputationalResourcesComponent } from '../cluster-details';
import { SchedulerComponent } from '../../scheduler';

@Component({
  selector: 'computational-resources-list',
  templateUrl: 'computational-resources-list.component.html',
  styleUrls: ['./computational-resources-list.component.scss']
})

export class ComputationalResourcesListComponent {
  @Input() resources: any[];
  @Input() environment: any;

  @Output() buildGrid: EventEmitter<{}> = new EventEmitter();

  constructor(
    public dialog: MatDialog,
    public toastr: ToastrService,
    private userResourceService: UserResourceService
  ) { }

  rebuildGrid(): void {
    this.buildGrid.emit();
  }

  public toggleResourceAction(resource, action: string): void {
    if (action === 'stop' || action === 'terminate') {
      const dialogRef: MatDialogRef<ConfirmationDialogComponent> = this.dialog.open(ConfirmationDialogComponent,
        { data: { action, resource }, width: '550px', panelClass: 'error-modalbox' });
      dialogRef.afterClosed().subscribe(result => {
        if (result && action === 'stop') {
          this.userResourceService
            .toggleStopStartAction(this.environment.project, this.environment.name, resource.computational_name, action)
            .subscribe(() => {
              this.rebuildGrid();
            });
        } else if (result && action === 'terminate') {
          this.userResourceService
            .suspendComputationalResource(this.environment.name, resource.computational_name)
            .subscribe(() => {
              this.rebuildGrid();
            });
        }
      });
    } else if (action === 'start') {
      this.userResourceService
        .toggleStopStartAction(this.environment.project, this.environment.name, resource.computational_name, 'start')
        .subscribe(
          () => this.rebuildGrid(),
          error => this.toastr.error(error.message || 'Computational resource starting failed!', 'Oops!'));
    }
  }

  public detailComputationalResources(environment, resource): void {
    this.dialog.open(DetailComputationalResourcesComponent, { data: { environment, resource }, panelClass: 'modal-sm' })
      .afterClosed().subscribe(() => this.rebuildGrid());
  };

  public openScheduleDialog(resource) {
    this.dialog.open(SchedulerComponent, { data: { notebook: this.environment, type: 'СOMPUTATIONAL', resource }, panelClass: 'modal-xl-s' })
      .afterClosed().subscribe(() => this.rebuildGrid());
  }
}


@Component({
  selector: 'confirmation-dialog',
  template: `
  <div class="dialog-header">
    <h4 class="modal-title"><span class="capitalize">{{ data.action }}</span> resource</h4>
    <button type="button" class="close" (click)="dialogRef.close()">&times;</button>
  </div>
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
  `
})
export class ConfirmationDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }
}
