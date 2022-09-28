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

import { Injectable } from '@angular/core';
import { ConnectedPlatformApiService } from '../../core/services/connected-platform-api.service';
import { tap } from 'rxjs/operators';
import { BehaviorSubject, Observable } from 'rxjs';
import { AddModalData, AddPlatformFromValue, ConnectedPlatformsInfo } from './connected-platforms.models';

@Injectable({
  providedIn: 'root'
})
export class ConnectedPlatformsService {
  // tslint:disable-next-line:max-line-length
  private platformPageData$$: BehaviorSubject<ConnectedPlatformsInfo> = new BehaviorSubject<ConnectedPlatformsInfo>({} as ConnectedPlatformsInfo);
  platformPageData$: Observable<ConnectedPlatformsInfo> = this.platformPageData$$.asObservable();
  addModalData: AddModalData;

  constructor(
    private connectedPlatformPageService: ConnectedPlatformApiService
  ) { }

  getConnectedPlatformPageInfo(): Observable<ConnectedPlatformsInfo> {
    return this.connectedPlatformPageService.getConnectedPlatformsPage()
      .pipe(
        tap( result => this.platformPageData$$.next(result)),
        tap( result => this.getAddModalData(result)),
      );
  }

  addPlatform(platformParams: AddPlatformFromValue): Observable<any> {
    return this.connectedPlatformPageService.addPlatform(platformParams);
  }

  disconnectPlatform(platformName: string): Observable<any> {
    return this.connectedPlatformPageService.disconnectPlatform(platformName);
  }

  private getAddModalData(info: ConnectedPlatformsInfo): void {
    const { platformNames, types } = info;
    this.addModalData = { platformNames, types };
  }
}
