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

import { Directive } from '@angular/core';
import { AbstractControl, NG_ASYNC_VALIDATORS, ValidationErrors, Validator } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap, take } from 'rxjs/operators';
import { RolesGroupsService } from '../services';
import { GroupModel } from '../models/role.model';
import 'rxjs-compat/add/observable/timer';


@Directive({
  selector: '[isGroupNameUnique]',
  providers: [{ provide: NG_ASYNC_VALIDATORS, useExisting: IsGroupNameUniqueDirective, multi: true }]
})
export class IsGroupNameUniqueDirective implements Validator {
  constructor( private rolesService: RolesGroupsService ) {
  }
  validate(control: AbstractControl): Observable<ValidationErrors | null> {
    return Observable.timer(300).pipe(
      switchMap( () => {
        return this.rolesService.getGroupsData().pipe(
          map(res => this.isGroupExist(res, control.value)),
          map(isGroupExist => (isGroupExist ? { duplicate: true } : null)),
          catchError(() => of(null)),
          take(1)
        );
      })
    );
  }

  private isGroupExist(groupList: GroupModel[], comparedValue: string): boolean {
    return groupList.some(({group}) => {
      return  group.toLowerCase() === comparedValue.toLowerCase();
    });
  }
}
