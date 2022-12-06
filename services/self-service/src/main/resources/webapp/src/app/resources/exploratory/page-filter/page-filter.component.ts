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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { MatOption } from '@angular/material/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { take, tap } from 'rxjs/operators';

import isEqual from 'lodash.isequal';

import { FilterFormPlaceholders } from './page-filter.config';
import {
  DropdownFieldNames,
  DropdownSelectAllValue,
  FilterFormControlNames,
  ImageFilterFormDropdownData,
  ImageFilterFormValue
} from '../../images';

@Component({
  selector: 'datalab-page-filter',
  templateUrl: './page-filter.component.html',
  styleUrls: ['./page-filter.component.scss']
})
export class PageFilterComponent implements OnInit {
  @Input() filterDropdownData$: Observable<ImageFilterFormDropdownData>;
  @Input() filterFormStartValue$: Observable<ImageFilterFormValue>;

  @Output() filterFormValue: EventEmitter<ImageFilterFormValue> = new EventEmitter<ImageFilterFormValue>();
  @Output() closeFilter: EventEmitter<any> = new EventEmitter<any>();
  @Output() imageNameValue: EventEmitter<string> = new EventEmitter<string>();
  @Output() onValueChanges: EventEmitter<string> = new EventEmitter<string>();

  readonly placeholders: typeof FilterFormPlaceholders = FilterFormPlaceholders;
  readonly dropdownFieldNames: typeof DropdownFieldNames = DropdownFieldNames;
  readonly controlNames: typeof FilterFormControlNames = FilterFormControlNames;
  readonly selectAllValue = DropdownSelectAllValue;

  private isApplyBtnDisabled$$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(true);
  private filterFormStartValue: ImageFilterFormValue;

  filterForm: FormGroup;
  setFilterValueObservable$: Observable<ImageFilterFormValue>;
  changeIsApplyBtnDisabledObservable$: Observable<ImageFilterFormValue>;
  isApplyBtnDisabled$: Observable<boolean> = this.isApplyBtnDisabled$$.asObservable();

  constructor(
    private fb: FormBuilder
  ) { }

  ngOnInit(): void {
    this.createFilterForm();
    this.onControlChange(this.controlNames.imageName);
    this.setFilterValue();
    this.isFilterFormChanged();
  }

  onSelectClick(): void {
    const dropdownList = document.querySelectorAll('.cdk-overlay-pane');
    dropdownList.forEach(el => el.classList.add('normalized-dropdown-position'));
  }

  confirmFilter(): void {
    this.filterFormValue.emit(this.filterForm.value);
  }

  cancelFilter(): void {
    this.closeFilter.emit();
  }

  onControlChange(fieldName: keyof ImageFilterFormValue): void {
    this.filterForm.get(fieldName)?.valueChanges.pipe(
      tap((inputValue: string) => this.onValueChanges.emit(inputValue))
    ).subscribe();
  }

  onClickAll(control: FormControl, allSelected: MatOption, key: DropdownFieldNames ): void {
    if (allSelected.selected) {
      this.filterDropdownData$.pipe(
        tap(value => control.patchValue([...value[key], this.selectAllValue])),
        take(1)
      ).subscribe();
    } else {
      control.patchValue([]);
    }
  }

  private createFilterForm(): void {
    this.filterForm = this.fb.group({
      imageName: '',
      endpoints: [[]],
      statuses: [[]],
      templateNames: [[]],
      sharingStatuses: [[]],
    });
  }

  private setFilterValue(): void {
    this.setFilterValueObservable$ = this.filterFormStartValue$.pipe(
      tap(value => this.updateFilterForm(value))
      );
  }

  private updateFilterForm(value: ImageFilterFormValue): void {
    this.filterFormStartValue = value;
    this.filterForm.patchValue(value);
  }

  private isFilterFormChanged(): void {
    this.changeIsApplyBtnDisabledObservable$ = this.filterForm.valueChanges.pipe(
      tap(formValue => this.isApplyBtnDisabled$$.next(isEqual(formValue, this.filterFormStartValue)))
    );
  }

  get statuses() {
    return this.filterForm.get(DropdownFieldNames.statuses) as FormControl;
  }

  get endpoints() {
    return this.filterForm.get(DropdownFieldNames.endpoints) as FormControl;
  }

  get templateNames() {
    return this.filterForm.get(DropdownFieldNames.templateNames) as FormControl;
  }

  get sharingStatuses() {
    return this.filterForm.get(DropdownFieldNames.sharingStatuses) as FormControl;
  }
}
