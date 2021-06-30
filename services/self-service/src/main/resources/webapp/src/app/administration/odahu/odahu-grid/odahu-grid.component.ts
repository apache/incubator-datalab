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

import { Component, OnInit } from '@angular/core';
import {Subscription} from 'rxjs';
import {OdahuDataService} from '../odahu-data.service';
import { MatTableDataSource } from '@angular/material/table';

import {ToastrService} from 'ngx-toastr';
import {MatDialog} from '@angular/material/dialog';
import {OdahuDeploymentService} from '../../../core/services';
import {OdahuActionDialogComponent} from '../../../shared/modal-dialog/odahu-action-dialog';

@Component({
  selector: 'odahu-grid',
  templateUrl: './odahu-grid.component.html',
  styleUrls: ['./odahu-grid.component.scss']
})
export class OdahuGridComponent implements OnInit {
  private odahuList: any[];
  private subscriptions: Subscription = new Subscription();
  public dataSource: MatTableDataSource<any>;
  displayedColumns: string[] = [ 'odahu-name', 'project', 'endpoint-url', 'odahu-status', 'actions'];

  constructor(
    private odahuDataService: OdahuDataService,
    private odahuDeploymentService: OdahuDeploymentService,
    public toastr: ToastrService,
    public dialog: MatDialog
  ) { }

  ngOnInit() {
    this.subscriptions.add(this.odahuDataService._odahuClasters.subscribe(
      (value) => {
        if (value) {
          this.odahuList = value;
          this.dataSource = new MatTableDataSource(value);
        }
      }));
  }

  private odahuAction(element: any, action: string) {
    this.dialog.open(OdahuActionDialogComponent, {data: {type: action, item: element}, panelClass: 'modal-sm'})
      .afterClosed().subscribe(result => {
        result && this.odahuDeploymentService.odahuAction(element,  action).subscribe(v =>
          this.odahuDataService.updateClasters(),
          error => this.toastr.error(`Odahu cluster ${action} failed!`, 'Oops!')
        ) ;
      }, error => this.toastr.error(error.message || `Odahu cluster ${action} failed!`, 'Oops!')
    );
  }
}
