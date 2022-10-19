/*!
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
import { BehaviorSubject, EMPTY, Observable, pipe } from 'rxjs';
import { switchMap, take, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

import { ConnectedPlatformsStatus, GeneralEnvironmentStatus } from '../../administration/management/management.model';
import { HealthStatusService } from '../../core/services';
import { ConnectedPlatformsTableTitles, ConnectedPlatformDisplayedColumns } from './connected-platforms.config';
import { ConnectedPlatformDialogComponent } from './connected-platform-dialog/connected-platform-dialog.component';
import { ConnectedPlatformsInfo, Platform } from './connected-platforms.models';
import { ConnectedPlatformsService } from './connected-platforms.service';
import { WarningDialogComponent } from './warning-dialog/warning-dialog.component';

@Component({
  selector: 'datalab-connected-platforms',
  templateUrl: './connected-platforms.component.html',
  styleUrls: ['./connected-platforms.component.scss']
})
export class ConnectedPlatformsComponent implements OnInit {
  readonly tableHeaderCellTitles: typeof ConnectedPlatformsTableTitles = ConnectedPlatformsTableTitles;
  readonly maxUrlLength: number = 30;

  // tslint:disable-next-line:max-line-length
  private readonly connectedPlatformsStatus$$: BehaviorSubject<ConnectedPlatformsStatus> = new BehaviorSubject<ConnectedPlatformsStatus>({} as ConnectedPlatformsStatus);
  readonly connectedPlatformsStatus$: Observable<ConnectedPlatformsStatus> = this.connectedPlatformsStatus$$.asObservable();

  platformPageData$: Observable<ConnectedPlatformsInfo>;

  displayedColumns: typeof ConnectedPlatformDisplayedColumns = ConnectedPlatformDisplayedColumns;

  constructor(
    private healthStatusService: HealthStatusService,
    public toastr: ToastrService,
    private dialog: MatDialog,
    private connectedPlatformsService: ConnectedPlatformsService
  ) { }

  ngOnInit(): void {
    this.getEnvironmentHealthStatus();
    this.getConnectedPlatformPageInfo();
    this.initPageData();
  }

  onAddNew(): void {
    this.dialog.open(ConnectedPlatformDialogComponent, {
      data: this.connectedPlatformsService.addModalData,
      panelClass: 'modal-lg'
    })
      .afterClosed()
      .pipe(
        this.getModalAction(this.connectedPlatformsService.addPlatform),
      ).subscribe();
  }

  onPlatformDisconnect({name}: Platform): void {
    this.dialog.open(WarningDialogComponent,
      {
        data: name,
        panelClass: 'modal-sm'
      })
      .afterClosed()
      .pipe(
        this.getModalAction(this.connectedPlatformsService.disconnectPlatform)
      ).subscribe();
  }

  private getModalAction(callback: Function) {
    callback = callback.bind(this.connectedPlatformsService);
    return pipe(
      switchMap((arg) => {
        if (arg) {
          return callback(arg);
        }
        return EMPTY;
      }),
      switchMap(() => this.connectedPlatformsService.getConnectedPlatformPageInfo())
    );
  }

  private getEnvironmentHealthStatus(): void {
    this.healthStatusService.getEnvironmentHealthStatus().pipe(
      tap((response: GeneralEnvironmentStatus) => this.connectedPlatformsStatus$$.next(response.connectedPlatforms)),
      take(1)
    ).subscribe();
  }

  private getConnectedPlatformPageInfo(): void {
    this.connectedPlatformsService.getConnectedPlatformPageInfo().subscribe();
  }

  private initPageData(): void {
    this.platformPageData$ = this.connectedPlatformsService.platformPageData$;
  }
}
