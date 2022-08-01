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
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { FilterFormPlaceholders } from './page-filter.config';
import { DropdownFieldNames, ImageFilterFormDropdownData, ImageFilterFormValue } from '../../images';

@Component({
  selector: 'datalab-page-filter',
  templateUrl: './page-filter.component.html',
  styleUrls: ['./page-filter.component.scss']
})
export class PageFilterComponent implements OnInit {
  @Input() $filterDropdownData: Observable<ImageFilterFormDropdownData>;
  @Input() $filterFormStartValue: Observable<ImageFilterFormValue>;

  @Output() filterFormValue: EventEmitter<ImageFilterFormValue> = new EventEmitter<ImageFilterFormValue>();
  @Output() closeFilter: EventEmitter<any> = new EventEmitter<any>();
  @Output() imageNameValue: EventEmitter<string> = new EventEmitter<string>();
  @Output() onValueChanges: EventEmitter<string> = new EventEmitter<string>();

  readonly placeholders: typeof FilterFormPlaceholders = FilterFormPlaceholders;
  readonly dropdownFieldNames: typeof DropdownFieldNames = DropdownFieldNames;

  filterForm: FormGroup;

  constructor(
    private fb: FormBuilder
  ) { }

  ngOnInit(): void {
    this.createFilterForm();
    this.onControlChange(DropdownFieldNames.imageName);
    this.setFilterValue();
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

  onControlChange(fieldName: keyof ImageFilterFormDropdownData): void {
   this.filterForm.get(fieldName)?.valueChanges.pipe(
      tap((inputValue: string) => this.onValueChanges.emit(inputValue))
    ).subscribe();
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
    this.$filterFormStartValue.subscribe(value => this.filterForm.patchValue(value));
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
